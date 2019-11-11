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

package com.dangdang.ddframe.job.lite.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.util.ClassUtils;

import com.dangdang.ddframe.job.api.ElasticJob;
import com.dangdang.ddframe.job.api.script.ScriptJob;
import com.dangdang.ddframe.job.config.JobCoreConfiguration;
import com.dangdang.ddframe.job.event.DefaultJobEventBus;
import com.dangdang.ddframe.job.event.JobEventBus;
import com.dangdang.ddframe.job.event.JobEventConfiguration;
import com.dangdang.ddframe.job.exception.JobConfigurationException;
import com.dangdang.ddframe.job.exception.JobSystemException;
import com.dangdang.ddframe.job.executor.JobFacade;
import com.dangdang.ddframe.job.lite.api.listener.AbstractDistributeOnceElasticJobListener;
import com.dangdang.ddframe.job.lite.api.listener.ElasticJobListener;
import com.dangdang.ddframe.job.lite.api.strategy.JobInstance;
import com.dangdang.ddframe.job.lite.config.LiteJobConfiguration;
import com.dangdang.ddframe.job.lite.internal.guarantee.GuaranteeService;
import com.dangdang.ddframe.job.lite.internal.schedule.JobRegistry;
import com.dangdang.ddframe.job.lite.internal.schedule.JobScheduleController;
import com.dangdang.ddframe.job.lite.internal.schedule.JobServiceRegistry;
import com.dangdang.ddframe.job.lite.internal.schedule.JobShutdownHookPlugin;
import com.dangdang.ddframe.job.lite.internal.schedule.LiteJob;
import com.dangdang.ddframe.job.lite.internal.schedule.LiteJobFacade;
import com.dangdang.ddframe.job.lite.internal.schedule.SchedulerFacade;
import com.dangdang.ddframe.job.reg.base.CoordinatorRegistryCenter;
import com.google.common.base.Optional;

import lombok.Getter;

/**
 * 作业调度器.
 * 
 * @author zhangliang
 * @author caohao
 */
public class JobScheduler {
    
    public static final String ELASTIC_JOB_DATA_MAP_KEY = "elasticJob";
    
    private static final String JOB_FACADE_DATA_MAP_KEY = "jobFacade";
    
    private final LiteJobConfiguration liteJobConfig;
    
    private final CoordinatorRegistryCenter regCenter;
    
    // TODO 为测试使用,测试用例不能反复new monitor service,以后需要把MonitorService重构为单例
    @Getter
    private final SchedulerFacade schedulerFacade;
    @Getter
    private final JobFacade jobFacade;
    
    public JobScheduler(final CoordinatorRegistryCenter regCenter, final LiteJobConfiguration liteJobConfig, final ElasticJobListener... elasticJobListeners) {
        this(regCenter, liteJobConfig, new DefaultJobEventBus(), elasticJobListeners);
    }
    
    public JobScheduler(final CoordinatorRegistryCenter regCenter, final LiteJobConfiguration liteJobConfig, final JobEventConfiguration jobEventConfig, 
                        final ElasticJobListener... elasticJobListeners) {
        this(regCenter, liteJobConfig, jobEventConfig.createJobEventBus(), elasticJobListeners);
    }
    
    private JobScheduler(final CoordinatorRegistryCenter regCenter, final LiteJobConfiguration liteJobConfig, final JobEventBus jobEventBus, final ElasticJobListener... elasticJobListeners) {
        this.liteJobConfig = liteJobConfig;
        this.regCenter = regCenter;
        String jobName = liteJobConfig.getJobName();
        JobRegistry.getInstance().addJobInstance(jobName, new JobInstance());
        //TODO("XIONGSL")
        JobServiceRegistry.getInstance().register(jobName, regCenter); 
        List<ElasticJobListener> elasticJobListenerList = new ArrayList<ElasticJobListener>(
        								Arrays.asList(elasticJobListeners));
        setGuaranteeServiceForElasticJobListeners(regCenter, elasticJobListenerList);
        schedulerFacade = new SchedulerFacade(regCenter, jobName, elasticJobListenerList);
        jobFacade = new LiteJobFacade(regCenter, jobName, elasticJobListenerList, jobEventBus);
    }
    
    private void setGuaranteeServiceForElasticJobListeners(final CoordinatorRegistryCenter regCenter, final List<ElasticJobListener> elasticJobListeners) {
        GuaranteeService guaranteeService = //new GuaranteeService(regCenter, liteJobConfig.getJobName());
        				JobServiceRegistry.getInstance().getGuaranteeService(liteJobConfig.getJobName());
        for (ElasticJobListener each : elasticJobListeners) {
            if (each instanceof AbstractDistributeOnceElasticJobListener) {
                ((AbstractDistributeOnceElasticJobListener) each).setGuaranteeService(guaranteeService);
            }
        }
    }
    
    /**
     * xiongsl:2019-06-05 the ClassLoader maybe alternate, so directly set
     */
    private Class<?> jobClass = null;
    public void setJobClass(Class<?> jobClass) {
    	this.jobClass = jobClass;
    }
    private JobScheduleController jobScheduleController;
    public void restart() {
    	String jobName = liteJobConfig.getJobName();
    	String jobClass = liteJobConfig.getTypeConfig().getJobClass();
    	boolean enabled = !liteJobConfig.isDisabled(); //初始状态
    	String cron = JobRegistry.getInstance().getJobCron(jobName);
    	JobRegistry.getInstance().addJobInstance(jobName, new JobInstance());
        JobRegistry.getInstance().registerJob(jobName, jobScheduleController, regCenter);
        try {
        	schedulerFacade.registerStartUpInfo(enabled);
        } catch (IllegalStateException ise) {
			//reconsileService.startAsyc() error
		}
        jobScheduleController = new JobScheduleController(
                this.createScheduler(), this.createJobDetail(jobClass), jobName);
        JobRegistry.getInstance().registerJob(jobName, jobScheduleController, regCenter);
        jobScheduleController.scheduleJob(cron);
    }
    
    /**
     * 初始化作业.
     */
    public void init() {
        LiteJobConfiguration liteJobConfigFromRegCenter = schedulerFacade.updateJobConfiguration(liteJobConfig);
        String jobName = liteJobConfig.getJobName();   
        boolean enabled = !liteJobConfig.isDisabled(); //初始状态
        String jobClass = liteJobConfigFromRegCenter.getTypeConfig().getJobClass();
        JobCoreConfiguration coreConfig = liteJobConfigFromRegCenter.getTypeConfig().getCoreConfig();
        JobRegistry.getInstance().setCurrentShardingTotalCount(jobName, coreConfig.getShardingTotalCount());
        
        /*JobScheduleController*/ jobScheduleController = new JobScheduleController(
                this.createScheduler(), this.createJobDetail(jobClass), jobName);
        JobRegistry.getInstance().registerJob(jobName, jobScheduleController, regCenter);
        JobRegistry.getInstance().setJobCron(jobName, coreConfig.getCron());
        schedulerFacade.registerStartUpInfo(enabled);
        jobScheduleController.scheduleJob(coreConfig.getCron());
//        if (!boot) {
//        	this.disableJob(jobName);  //console-显示"已失效"状态, 好于jobScheduleController.pauseJob();
//        }
    }
    
    private JobDetail createJobDetail(final String jobClassName) {
        JobDetail result = JobBuilder.newJob(LiteJob.class).withIdentity(liteJobConfig.getJobName()).build();
        result.getJobDataMap().put(JOB_FACADE_DATA_MAP_KEY, jobFacade);
        Optional<ElasticJob> elasticJobInstance = createElasticJobInstance();
        if (elasticJobInstance.isPresent()) {
            result.getJobDataMap().put(ELASTIC_JOB_DATA_MAP_KEY, elasticJobInstance.get());
        } else if (!jobClassName.equals(ScriptJob.class.getCanonicalName())) {
            try {
            	Object elastic = null;   //Class.forName(jobClassName).newInstance();
            	//TODO("XIONGSL") spring-core/ClassUtils
            	if (this.jobClass != null) {
            		elastic = this.jobClass.newInstance();
            	} else {
            		elastic = ClassUtils.getDefaultClassLoader().loadClass(jobClassName).newInstance();
            	}
                result.getJobDataMap().put(ELASTIC_JOB_DATA_MAP_KEY, elastic);
            } catch (final ReflectiveOperationException ex) {
//                throw new JobConfigurationException("Elastic-Job: Job class '%s' can not initialize.", jobClass);
                throw new JobConfigurationException(ex);
            }
        }
        return result;
    }
    
    protected Optional<ElasticJob> createElasticJobInstance() {
        return Optional.absent();
    }
    
    private Scheduler createScheduler() {
        Scheduler result;
        try {
            StdSchedulerFactory factory = new StdSchedulerFactory();
            factory.initialize(getBaseQuartzProperties());
            result = factory.getScheduler();
            result.getListenerManager().addTriggerListener(schedulerFacade.newJobTriggerListener());
        } catch (final SchedulerException ex) {
            throw new JobSystemException(ex);
        }
        return result;
    }
    
    private Properties getBaseQuartzProperties() {
        Properties result = new Properties();
        result.put("org.quartz.threadPool.class", org.quartz.simpl.SimpleThreadPool.class.getName());
        result.put("org.quartz.threadPool.threadCount", "1");
        result.put("org.quartz.scheduler.instanceName", liteJobConfig.getJobName());
        result.put("org.quartz.jobStore.misfireThreshold", "1");
        result.put("org.quartz.plugin.shutdownhook.class", JobShutdownHookPlugin.class.getName());
        result.put("org.quartz.plugin.shutdownhook.cleanShutdown", Boolean.TRUE.toString());
        //不会启动Timer 2019-06-18
        result.put("org.terracotta.quartz.skipUpdateCheck", Boolean.TRUE.toString());
        result.put("org.quartz.scheduler.skipUpdateCheck", Boolean.TRUE.toString());
        //TODO("XIONGSL")
        if (quartzProperites != null) {
        	result.putAll(quartzProperites);
        }
        return result;
    }
    
    private Properties quartzProperites;
    public void setQuartzProperties(Properties qtzProps) {
    	quartzProperites = qtzProps;
    }
    
//    private void disableJob(String jobName) {
//    	 JobNodePath jobNodePath = new JobNodePath(jobName);
//         for (String each : regCenter.getChildrenKeys(jobNodePath.getServerNodePath())) {
//              regCenter.persist(jobNodePath.getServerNodePath(each), "DISABLED");
//         }
//    }
}
