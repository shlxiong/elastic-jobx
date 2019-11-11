package com.openxsl.elasticjob.health;

import org.apache.curator.framework.recipes.cache.TreeCacheEvent.Type;

import com.dangdang.ddframe.job.lite.api.JobScheduler;
import com.dangdang.ddframe.job.lite.internal.manager.JobEventNode;
import com.dangdang.ddframe.job.lite.internal.manager.JobNodeListener;
import com.dangdang.ddframe.job.lite.internal.storage.JobNodePath;
import com.openxsl.config.filter.domain.Invoker;
import com.openxsl.config.filter.tracing.TraceContext;

/**
 * Shutdown之后重新上线
 * 
 * @author xiongsl
 */
public class JobRestartListener extends JobNodeListener {
	private final String jobName;
	private final JobScheduler scheduler;
	private final JobEventNode serverNode;
	
	public JobRestartListener(String jobName, JobScheduler scheduler) {
		this.jobName = jobName;
		this.scheduler = scheduler;
		serverNode = new JobEventNode(jobName, JobNodePath.SERVERS_NODE);
	}
	
	public void start() {
		serverNode.addDataListener(this);
	}

	@Override
	protected void dataChanged(String path, Type eventType, String data) {
		if (eventType == Type.NODE_UPDATED && "RESTART".equals(data)) {
			//初始化Quartz的线程上下文
			Invoker invoker = new Invoker("sched", jobName, "restart");
			String rpcId = TraceContext.getRpcId();  //null? curator's executor
			TraceContext.initiate(rpcId, invoker);

			//TODO 会产生多个NodeListener
			scheduler.restart();   //new StdSchedulerFactory.getScheduler -> InheritThreadPool.newInstance()
		}
	}
	
}
