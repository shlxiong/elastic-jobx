package com.dangdang.ddframe.job.lite.internal.schedule;

import org.quartz.InterruptableJob;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.UnableToInterruptJobException;

import com.dangdang.ddframe.job.api.ElasticJob;
import com.dangdang.ddframe.job.executor.JobExecutorFactory;
import com.dangdang.ddframe.job.executor.JobFacade;

import lombok.Setter;

/**
 * Lite调度作业.
 *
 * @author zhangliang
 * @author xiongsl 修改实现接口InterruptableJob
 */
public final class LiteJob implements InterruptableJob {
    
    @Setter
    private ElasticJob elasticJob;
    
    @Setter
    private JobFacade jobFacade;
    
    @Override
    public void execute(final JobExecutionContext context) throws JobExecutionException {
        JobExecutorFactory.getJobExecutor(elasticJob, jobFacade).execute();
    }

	@Override
	public void interrupt() throws UnableToInterruptJobException {
		// TODO Auto-generated method stub
	}
}
