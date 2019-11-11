package com.dangdang.ddframe.job.lite.internal.manager;

import java.util.List;

import com.dangdang.ddframe.job.lite.api.listener.ElasticJobListener;
import com.dangdang.ddframe.job.lite.internal.schedule.SchedulerFacade;
import com.dangdang.ddframe.job.reg.base.CoordinatorRegistryCenter;

public class JobNodesManager {
//	private final String jobName;
	private final ConfigNodeManager configManager;
	private final InstanceNodeManager instanceManager;
	private final ServerNodeManager serverManager;
	private final ShardingNodeManager shardingManager;
	private final GuaranteeNodeManager guranteManager;
	private final ElectionLeaderManager electionManager;
	
	public JobNodesManager(final CoordinatorRegistryCenter regCenter, String jobName,
						   final SchedulerFacade schlFacade,
						   final List<ElasticJobListener> elasticJobListeners) {
		configManager = new ConfigNodeManager(jobName);
		instanceManager = new InstanceNodeManager(jobName, schlFacade);
		serverManager = new ServerNodeManager(jobName);
		shardingManager = new ShardingNodeManager(jobName);
		electionManager = new ElectionLeaderManager(jobName);
		guranteManager = new GuaranteeNodeManager(regCenter,jobName,elasticJobListeners);
	}
	
	public void startAllListeners() {
		configManager.start();
		instanceManager.start();
		serverManager.start();
		shardingManager.start();
		electionManager.start();
		guranteManager.start();
	}

}
