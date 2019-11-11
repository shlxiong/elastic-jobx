/*
 * Copyright 1999-2015 dangdang.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </p>
 */

package com.dangdang.ddframe.job.executor;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

import com.dangdang.ddframe.job.api.ShardingContext;
import com.dangdang.ddframe.job.config.JobRootConfiguration;
import com.dangdang.ddframe.job.context.TaskContext;
import com.dangdang.ddframe.job.event.type.JobExecutionEvent;
import com.dangdang.ddframe.job.event.type.JobExecutionEvent.ExecutionSource;
import com.dangdang.ddframe.job.event.type.JobStatusTraceEvent.State;
import com.dangdang.ddframe.job.exception.ExceptionUtil;
import com.dangdang.ddframe.job.exception.JobExecutionEnvironmentException;
import com.dangdang.ddframe.job.exception.JobSystemException;
import com.dangdang.ddframe.job.executor.handler.ExecutorServiceHandler;
import com.dangdang.ddframe.job.executor.handler.ExecutorServiceHandlerRegistry;
import com.dangdang.ddframe.job.executor.handler.JobExceptionHandler;
import com.dangdang.ddframe.job.executor.handler.JobProperties;
import com.dangdang.ddframe.job.executor.handler.JobProperties.JobPropertiesEnum;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * 弹性化分布式作业执行器.
 *
 * @author zhangliang
 */
@Slf4j
public abstract class AbstractElasticJobExecutor {
    
    @Getter(AccessLevel.PROTECTED)
    private final JobFacade jobFacade;
    
    @Getter(AccessLevel.PROTECTED)
    private final JobRootConfiguration jobRootConfig;
    
    private final String jobName;
    
    private final ExecutorService executorService;
    
    private final JobExceptionHandler jobExceptionHandler;
    
    private final Map<Integer, String> itemErrorMessages;
    
    protected AbstractElasticJobExecutor(final JobFacade jobFacade) {
        this.jobFacade = jobFacade;
        jobRootConfig = jobFacade.loadJobRootConfiguration(true);
        jobName = jobRootConfig.getTypeConfig().getCoreConfig().getJobName();
        executorService = ExecutorServiceHandlerRegistry.getExecutorServiceHandler(jobName, (ExecutorServiceHandler) getHandler(JobPropertiesEnum.EXECUTOR_SERVICE_HANDLER));
        jobExceptionHandler = (JobExceptionHandler) getHandler(JobPropertiesEnum.JOB_EXCEPTION_HANDLER);
        itemErrorMessages = new ConcurrentHashMap<>(jobRootConfig.getTypeConfig().getCoreConfig().getShardingTotalCount(), 1);
    }
    
    private Object getHandler(final JobProperties.JobPropertiesEnum jobPropertiesEnum) {
        String handlerClassName = jobRootConfig.getTypeConfig().getCoreConfig().getJobProperties().get(jobPropertiesEnum);
        try {
            Class<?> handlerClass = Class.forName(handlerClassName);
            if (jobPropertiesEnum.getClassType().isAssignableFrom(handlerClass)) {
                return handlerClass.newInstance();
            }
            return getDefaultHandler(jobPropertiesEnum, handlerClassName);
        } catch (final ReflectiveOperationException ex) {
            return getDefaultHandler(jobPropertiesEnum, handlerClassName);
        }
    }
    
    private Object getDefaultHandler(final JobProperties.JobPropertiesEnum jobPropertiesEnum, final String handlerClassName) {
        log.warn("Cannot instantiation class '{}', use default '{}' class.", handlerClassName, jobPropertiesEnum.getKey());
        try {
            return Class.forName(jobPropertiesEnum.getDefaultValue()).newInstance();
        } catch (final ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            throw new JobSystemException(e);
        }
    }
    
    /**
     * 执行作业。
     * 1、FAILOVER - 其他instance转移过来的
     * 2、NORMAL - 正常
     * 3、MISFIRE - 上次未执行的
     */
    public final void execute() {
        try {
            jobFacade.checkJobExecutionEnvironment();
        } catch (final JobExecutionEnvironmentException cause) {
            jobExceptionHandler.handleException(jobName, cause);
        }
        //failover first
        final ShardingContexts shardingContexts = jobFacade.getShardingContexts();
        //TODO("XIONGSL") 本机不存在分片，无需执行
        Set<Integer> shardIds = shardingContexts.getShardingItemParameters().keySet();
        if (shardIds.isEmpty()) {
            return;
        }
        
        this.postStatusTraceLog(shardingContexts, State.TASK_STAGING, String.format("Job '%s' execute begin.", jobName));
        if (jobFacade.misfireIfRunning(shardIds)) {
        	String message = String.format("Previous trigger '%s' - shardingItems '%s' is still running, misfired job will start after previous job completed.", 
        									jobName, shardIds);
        	this.postStatusTraceLog(shardingContexts, State.TASK_FINISHED, message);
            return;
        }
        try {
        	this.postStatusTraceLog(shardingContexts, State.TASK_PRPARE, "JobListener prepare");   //TODO("XIONGSL")
            jobFacade.beforeJobExecuted(shardingContexts);
        } catch (final Throwable cause) {
        	this.postStatusTraceLog(shardingContexts, State.TASK_ERROR, "JobListener prepare error");
            jobExceptionHandler.handleException(jobName, cause);
        }
        execute(shardingContexts, ExecutionSource.NORMAL_TRIGGER);
        
        while (jobFacade.isExecuteMisfired(shardIds)) {
            jobFacade.clearMisfire(shardIds);
            execute(shardingContexts, ExecutionSource.MISFIRE);
        }
        
        jobFacade.failoverIfNecessary();
        try {
            jobFacade.afterJobExecuted(shardingContexts);
            this.postStatusTraceLog(shardingContexts, State.TASK_CLEAN, "JobListener clean");   //TODO("XIONGSL")
        } catch (final Throwable cause) {
            jobExceptionHandler.handleException(jobName, cause);
            this.postStatusTraceLog(shardingContexts, State.TASK_ERROR, "JobListener clean error");
        }
    }
    
    private void execute(final ShardingContexts shardingContexts, final ExecutionSource executionSource) {
        if (shardingContexts.getShardingItemParameters().isEmpty()) {
            return;
        }
        
        jobFacade.registerJobBegin(shardingContexts);
        this.postStatusTraceLog(shardingContexts, State.TASK_RUNNING,
        			String.format("Job '%s' RUNNING", shardingContexts.getJobName()));
        try {
            process(shardingContexts, executionSource);
        } finally {
            // TODO 考虑增加作业失败的状态，并且考虑如何处理作业失败的整体回路
            jobFacade.registerJobCompleted(shardingContexts);
            if (itemErrorMessages.isEmpty()) {
            	this.postStatusTraceLog(shardingContexts, State.TASK_FINISHED, 
            				String.format("Job '%s' FINISHED", shardingContexts.getJobName()));
            } else {
            	this.postStatusTraceLog(shardingContexts, State.TASK_ERROR, itemErrorMessages.toString());
            }
        }
    }
    
    private void process(final ShardingContexts shardingContexts, final ExecutionSource executionSource) {
        Collection<Integer> items = shardingContexts.getShardingItemParameters().keySet();
        if (1 == items.size()) {
            int item = shardingContexts.getShardingItemParameters().keySet().iterator().next();
            JobExecutionEvent jobExecutionEvent =  new JobExecutionEvent(shardingContexts.getTaskId(), jobName, executionSource, item);
            jobExecutionEvent.setContextId(shardingContexts.getInvocationId());   //TODO("XIONGSL")
            process(shardingContexts, item, jobExecutionEvent);
            return;
        }
        final CountDownLatch latch = new CountDownLatch(items.size());
        for (final int each : items) {
            final JobExecutionEvent jobExecutionEvent = new JobExecutionEvent(shardingContexts.getTaskId(), jobName, executionSource, each);
            jobExecutionEvent.setContextId(shardingContexts.getInvocationId());   //TODO("XIONGSL")
            if (executorService.isShutdown()) {
                return;
            }
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        process(shardingContexts, each, jobExecutionEvent);
                    } finally {
                        latch.countDown();
                    }
                }
            });
        }
        try {
            latch.await();
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
    
    private void process(final ShardingContexts shardingContexts, final int item, final JobExecutionEvent startEvent) {
        this.postExecTraceLog(shardingContexts, startEvent);
        JobExecutionEvent completeEvent = null;
        try {
            process(new ShardingContext(shardingContexts, item));
            completeEvent = startEvent.executionSuccess();
            // CHECKSTYLE:OFF
        } catch (final Throwable cause) {
            // CHECKSTYLE:ON
            itemErrorMessages.put(item, ExceptionUtil.transform(cause));
            jobExceptionHandler.handleException(jobName, cause);
            completeEvent = startEvent.executionFailure(cause);
        } finally {
        	this.postExecTraceLog(shardingContexts, completeEvent);
        }
    }
    
    private final void postStatusTraceLog(ShardingContexts shardingContexts, State state, String message) {
    	if (shardingContexts.isAllowSendJobEvent()) {
    		message = shardingContexts.getInvocationId() + TaskContext.DELIMITER + message;
    		try {
    			jobFacade.postJobStatusTraceEvent(shardingContexts.getTaskId(), state, message);
    		} catch (Exception e) {
    			jobExceptionHandler.handleException(jobName, e);
    		}
    	}
    }
    private final void postExecTraceLog(ShardingContexts shardingContexts, JobExecutionEvent executionEvent) {
    	if (shardingContexts.isAllowSendJobEvent()) {
    		try {
    			jobFacade.postJobExecutionEvent(executionEvent);
    		} catch (Exception e) {
    			jobExceptionHandler.handleException(jobName, e);
    		}
        }
    }
    
    protected abstract void process(ShardingContext shardingContext);
}
