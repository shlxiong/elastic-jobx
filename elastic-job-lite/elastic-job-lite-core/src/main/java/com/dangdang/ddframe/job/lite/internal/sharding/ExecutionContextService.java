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

package com.dangdang.ddframe.job.lite.internal.sharding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dangdang.ddframe.job.config.JobCoreConfiguration;
import com.dangdang.ddframe.job.context.TaskContext;
import com.dangdang.ddframe.job.executor.ShardingContexts;
import com.dangdang.ddframe.job.executor.handler.JobProperties.JobPropertiesEnum;
import com.dangdang.ddframe.job.lite.api.strategy.JobInstance;
import com.dangdang.ddframe.job.lite.config.LiteJobConfiguration;
import com.dangdang.ddframe.job.lite.internal.config.ConfigurationService;
import com.dangdang.ddframe.job.lite.internal.schedule.JobRegistry;
import com.dangdang.ddframe.job.lite.internal.schedule.JobServiceRegistry;
import com.dangdang.ddframe.job.lite.internal.storage.JobNodeStorage;
import com.dangdang.ddframe.job.reg.base.CoordinatorRegistryCenter;
import com.dangdang.ddframe.job.util.config.ShardingItemParameters;
import com.dangdang.ddframe.job.util.json.GsonFactory;
import com.google.common.base.Joiner;

/**
 * 作业运行时上下文服务.
 * 
 * @author zhangliang
 */
public final class ExecutionContextService {
    
    private final String jobName;
    
    private final JobNodeStorage jobNodeStorage;
    
    private final ConfigurationService configService;
    
    public ExecutionContextService(final CoordinatorRegistryCenter regCenter, final String jobName) {
        this.jobName = jobName;
        jobNodeStorage = new JobNodeStorage(regCenter, jobName);
        configService = //new ConfigurationService(regCenter, jobName);
        		JobServiceRegistry.getInstance().getConfigService(jobName);
    }
    
    /**
     * 获取当前作业服务器分片上下文.
     * 
     * @param shardingItems 分片项
     * @return 分片上下文
     */
    public ShardingContexts getJobShardingContext(final List<Integer> shardingItems) {
        LiteJobConfiguration liteJobConfig = configService.load(false);
        removeRunningIfMonitorExecution(liteJobConfig.isMonitorExecution(), shardingItems);
        
        //TODO("XIONGSL")
        JobCoreConfiguration coreConfig = liteJobConfig.getTypeConfig().getCoreConfig();
        String jobName = liteJobConfig.getJobName();
        String jobParam = coreConfig.getJobParameter();
        int shardCount = coreConfig.getShardingTotalCount();
        Map<Integer, String> shardingParams = Collections.emptyMap();
        if (!shardingItems.isEmpty()) {
        	shardingParams = new ShardingItemParameters(coreConfig.getShardingItemParameters()).getMap();
        	shardingParams = this.getAssignedShardingItemParameterMap(shardingItems, shardingParams);
        }
        String taskId = this.buildTaskId(liteJobConfig, shardingItems);
        ShardingContexts contexts = new ShardingContexts(taskId, jobName, shardCount, jobParam, shardingParams);
        String runtimes = coreConfig.getJobProperties().get(JobPropertiesEnum.JOB_RUNTIME_PROPERTIES);
        Boolean allows = (Boolean)GsonFactory.getGson().fromJson(runtimes, Map.class)
        					.get("allowSendEvent");
        contexts.setAllowSendJobEvent(allows==null ? true : allows.booleanValue());
        return contexts;
    }
    
    private String buildTaskId(final LiteJobConfiguration liteJobConfig, final List<Integer> shardingItems) {
        JobInstance jobInstance = JobRegistry.getInstance().getJobInstance(jobName);
        return liteJobConfig.getJobName() + TaskContext.DELIMITER + Joiner.on(",").join(shardingItems)
        		+ TaskContext.DELIMITER + "READY"
        		+ TaskContext.DELIMITER + jobInstance.getJobInstanceId();
    }
    
    private void removeRunningIfMonitorExecution(final boolean monitorExecution, final List<Integer> shardingItems) {
        if (!monitorExecution) {
            return;
        }
        List<Integer> runningShardingItems = new ArrayList<>(shardingItems.size());
        for (int each : shardingItems) {
            if (isRunning(each)) {
                runningShardingItems.add(each);
            }
        }
        shardingItems.removeAll(runningShardingItems);
    }
    
    private boolean isRunning(final int shardingItem) {
        return jobNodeStorage.isJobNodeExisted(ShardingNode.getRunningNode(shardingItem));
    }
    
    private Map<Integer, String> getAssignedShardingItemParameterMap(final List<Integer> shardingItems, final Map<Integer, String> shardingItemParameterMap) {
        Map<Integer, String> result = new HashMap<>(shardingItemParameterMap.size(), 1);
        for (int each : shardingItems) {
            result.put(each, shardingItemParameterMap.get(each));
        }
        return result;
    }
}
