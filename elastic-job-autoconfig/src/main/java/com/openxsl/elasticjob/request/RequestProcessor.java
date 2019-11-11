package com.openxsl.elasticjob.request;

import org.quartz.Scheduler;

import com.dangdang.ddframe.job.lite.internal.schedule.JobRegistry;
import com.dangdang.ddframe.job.lite.internal.schedule.JobScheduleController;

public class RequestProcessor {
	//put params + triggerTime into queue
	//上一条 数据是否执行完成: JobRegistry.getInstance().isJobRunning(jobName)
	//queue.take()
	//比较当前时间，如果早于，则立即执行（多线程）
	//否则：jobCtrl.schedulJob(cron);
	
	public void process(String jobName, String jobParam) {
//		JobSchedulerRegistry.get(jobName)：  JobScheduler
		JobScheduleController jobCtrl = JobRegistry.getInstance()
				.getJobScheduleController(jobName);
		String cron = "";
		if (jobCtrl.isPaused()) {
			jobCtrl.rescheduleJob(cron);
		} else {
			jobCtrl.scheduleJob(cron);
		}
		Scheduler scheduler;
	}

}
