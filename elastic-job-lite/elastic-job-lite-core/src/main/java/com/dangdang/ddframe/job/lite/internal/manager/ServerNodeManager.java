package com.dangdang.ddframe.job.lite.internal.manager;

import org.apache.curator.framework.recipes.cache.TreeCacheEvent.Type;

import com.dangdang.ddframe.job.lite.internal.election.LeaderService;
import com.dangdang.ddframe.job.lite.internal.schedule.JobServiceRegistry;
import com.dangdang.ddframe.job.lite.internal.server.ServerStatus;
import com.dangdang.ddframe.job.lite.internal.sharding.ShardingService;
import com.dangdang.ddframe.job.lite.internal.storage.JobNodePath;

/**
 * 服务器节点("/{jobName}/servers")监听器
 * @author xiongsl
 */
public class ServerNodeManager extends AbstractNodeManager {
	private final JobEventNode serverNode;

	protected ServerNodeManager(String jobName) {
		super(jobName);
		serverNode = new JobEventNode(jobName, JobNodePath.SERVERS_NODE);
	}

	@Override
	public void start() {
		serverNode.addDataListener(new ServersChangedJobListener());
	}
	
	/**
	 * 服务器变更，包括：增加、删除、DISABLED
	 * @author xiongsl
	 */
	class ServersChangedJobListener extends JobNodeListener {
		private final ShardingService shardingService =
						JobServiceRegistry.getInstance().getShardService(jobName);
		private final LeaderService leaderService =
						JobServiceRegistry.getInstance().getLeaderService(jobName);
        
		//JobOperateAPIImpl.enable/disable(), removeServer()
        @Override
        protected void dataChanged(final String path, final Type eventType, final String data) {
        	if (isShutdown() || Type.NODE_ADDED == eventType) {
        		return;
        	}
        	
        	String serverIp = path.substring(path.lastIndexOf("/"));
        	boolean isLocal = serverNode.isLocalServer(path);
        	if (Type.NODE_UPDATED == eventType && isLocal) {
        		if (logger.isInfoEnabled()) {
        			logger.info("Job(name={})'s server:{} has been {}.", 
        						jobName, serverIp, data.equals("")?"ENABLED":data);
        		}
        		if (ServerStatus.DISABLED.name().equals(data)) {
        			if (leaderService.isLeader()) {
            			leaderService.removeLeader();   //then LeaderMissingListener
            		}
        		} else if ("".equals(data)){
        			if (!leaderService.hasLeader()) {
            			leaderService.electLeader();
            		}
        		}
        	}
//        	if (Type.NODE_ADDED == eventType && !ServerStatus.DISABLED.name().equals(data)) { //boot   
//        		shardingService.setReshardingFlag();
//        	}
        	if (Type.NODE_REMOVED == eventType && !isLocal) {  //after shutdown(failover) then delete
        		if (logger.isInfoEnabled()) {
        			logger.info("Job(name={})'s server:{} has been removed!! and will be resharding.", jobName,serverIp);
        		}
        		shardingService.setReshardingFlag();
        	}
        	
        }
    }

}
