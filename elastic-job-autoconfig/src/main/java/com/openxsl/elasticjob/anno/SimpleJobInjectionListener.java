package com.openxsl.elasticjob.anno;

import org.quartz.JobDetail;

import com.dangdang.ddframe.job.api.ElasticJob;
import com.dangdang.ddframe.job.executor.ShardingContexts;
import com.dangdang.ddframe.job.lite.api.JobScheduler;
import com.dangdang.ddframe.job.lite.api.listener.ElasticJobListener;
import com.dangdang.ddframe.job.lite.internal.schedule.JobRegistry;
import com.dangdang.ddframe.job.lite.internal.schedule.JobScheduleController;
import com.openxsl.config.proxy.ClassGenerator;
import com.openxsl.config.util.BeanUtils;

/**
 * 将JobAction注入ElasticJob对象中
 * @see ElasticJobConfiguration$JobActionInjectListener
 * 
 * @author xiongsl
 */
public class SimpleJobInjectionListener implements ElasticJobListener {
	private String jobName;
	private Object jobAction;
	private boolean setted = false;
	
	public SimpleJobInjectionListener(String jobName, Object jobAction) {
		this.jobName = jobName;
		this.jobAction = jobAction;
	}

	@Override
	public void beforeJobExecuted(ShardingContexts shardingContexts) {
		if (!setted) {
			this.registerJobAction(jobName, jobAction);
			setted = true;
		}
	}

	@Override
	public void afterJobExecuted(ShardingContexts shardingContexts) {
	}
	
	private void registerJobAction(String jobName, Object jobAction) {
		JobScheduleController controller = JobRegistry.getInstance().getJobScheduleController(jobName);
		JobDetail jobDetail = (JobDetail)BeanUtils.getPrivateField(controller, "jobDetail");
		ElasticJob job = (ElasticJob)jobDetail.getJobDataMap().get(JobScheduler.ELASTIC_JOB_DATA_MAP_KEY);
		try {
			if (job!=null && job instanceof ClassGenerator.DC) {
				BeanUtils.setPrivateField(job, "jobAction", jobAction);
			}
		}finally {
			jobDetail = null;
			controller = null;
		}
	}
	
}
