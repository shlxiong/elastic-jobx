package com.openxsl.elasticjob.eventlog;

import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import com.dangdang.ddframe.job.config.JobTypeConfiguration;
import com.dangdang.ddframe.job.lite.api.JobScheduler;
import com.dangdang.ddframe.job.lite.internal.instance.InstanceService;
import com.dangdang.ddframe.job.lite.internal.schedule.JobRegistry;
import com.openxsl.config.util.BeanUtils;
import com.openxsl.config.util.StringUtils;

/**
 * 通过JobFascade获取作业的相关信息
 *
 * @author xiongsl
 */
public class JobSchedulerRegistry {
	private static final ConcurrentHashMap<String, JobScheduler> SCHEDULER_MAP
				= new ConcurrentHashMap<String, JobScheduler>(4);
	private static final JobRegistry REGISTRY = JobRegistry.getInstance();
	
	public static void registerScheduler(String jobName, JobScheduler scheduler) {
		if (SCHEDULER_MAP.containsKey(jobName)) {
			throw new IllegalStateException("Such a jobName has been existed: "+jobName);
		}
		SCHEDULER_MAP.put(jobName, scheduler);
	}
	
	public static Properties getJobContext(String jobName){
		String instanceId = REGISTRY.getJobInstance(jobName).getJobInstanceId();
		JobScheduler jobScheduler = SCHEDULER_MAP.get(jobName);
//		Object controller = registry.getJobScheduleController(jobName);  "jobDetail"
//		ZookeeperConfiguration zkConf = (ZookeeperConfiguration)BeanUtils.getPrivateField(
//							registry.getRegCenter(jobName), "zkConfig");
		JobTypeConfiguration jobConf = (JobTypeConfiguration)BeanUtils.getPrivateFieldHierarchy(
							jobScheduler, "liteJobConfig.typeConfig");
		Properties jobProps = new Properties();
		jobProps.setProperty("jobType", jobConf.getJobType().name());
		jobProps.setProperty("jobClass", jobConf.getJobClass());
		jobProps.setProperty("instanceId", instanceId);
		jobProps.setProperty("jobParam", jobConf.getCoreConfig().getJobParameter());
		jobProps.setProperty("cron", jobConf.getCoreConfig().getCron());
		jobProps.setProperty("shardParams", jobConf.getCoreConfig().getShardingItemParameters());
		jobProps.setProperty("namespace", jobName.substring(0, jobName.indexOf("/")));
		InstanceService instanceService = (InstanceService)BeanUtils.getPrivateFieldHierarchy(
							jobScheduler, "jobFacade.shardingService.instanceService");
		jobProps.setProperty("instances", instanceService.getAvailableJobInstanceIds().toString());
		return jobProps;
	}
	
	public static Properties getShardContext(String jobName, String shardId){
		//shardId, shardParam, instanceId
		Properties shardProps = new Properties();
		String instanceId = REGISTRY.getJobInstance(jobName).getJobInstanceId();
		shardProps.setProperty("instanceId", instanceId);
		shardProps.setProperty("shardId", shardId);
		JobTypeConfiguration jobConf = (JobTypeConfiguration)BeanUtils.getPrivateFieldHierarchy(
							SCHEDULER_MAP.get(jobName), "liteJobConfig.typeConfig");
		String shardingParams = jobConf.getCoreConfig().getShardingItemParameters();
		int item = Integer.parseInt(shardId);
		String shardParam = StringUtils.split(shardingParams, ",")[item].split("=")[1];
		shardProps.setProperty("shardParam", shardParam);
		return shardProps;
	}
	
	public static JobScheduler getJobScheduler(String jobName) {
		return SCHEDULER_MAP.get(jobName);
	}
	

}
