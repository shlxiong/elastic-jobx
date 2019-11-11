package com.dangdang.ddframe.job.lite.internal.manager;

import org.apache.curator.framework.recipes.cache.TreeCacheEvent.Type;

import com.dangdang.ddframe.job.lite.api.strategy.JobInstance;
import com.dangdang.ddframe.job.lite.internal.election.LeaderService;
import com.dangdang.ddframe.job.lite.internal.schedule.JobRegistry;
import com.dangdang.ddframe.job.lite.internal.schedule.JobServiceRegistry;
import com.dangdang.ddframe.job.lite.internal.server.ServerService;
import com.dangdang.ddframe.job.lite.internal.storage.JobNodePath;

/**
 * Manager for "leader/election/instance"
 * @author xiongsl
 */
public class ElectionLeaderManager extends AbstractNodeManager {
	private final JobEventNode electionNode;
	private final LeaderService leaderService;
	private final ServerService serverService;

	protected ElectionLeaderManager(String jobName) {
		super(jobName);
		electionNode = new JobEventNode(jobName, JobNodePath.LEADER_HOST_NODE);
		leaderService = JobServiceRegistry.getInstance().getLeaderService(jobName);
		serverService = JobServiceRegistry.getInstance().getServerService(jobName);
	}

	@Override
	public void start() {
		electionNode.addDataListener(new LeaderMissingListener());
	}
	
	class LeaderMissingListener extends JobNodeListener{

		@Override
		protected void dataChanged(String path, Type eventType, String data) {
			if (Type.NODE_REMOVED != eventType || isShutdown()) {
				return;
			}
			logger.info("Job(name={})'s Leader {} has been removed!!, and will try election..", jobName, data);
			JobInstance jobInstance = JobRegistry.getInstance().getJobInstance(jobName);
			if (!data.equals(jobInstance.getJobInstanceId())
					&& serverService.isAvailableServer(jobInstance.getIp())) {
				leaderService.electLeader();
			}
		}
		
	}

}
