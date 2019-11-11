package com.dangdang.ddframe.job.lite.internal.manager;

import org.apache.curator.framework.recipes.cache.TreeCacheEvent.Type;

import com.dangdang.ddframe.job.lite.internal.storage.JobNodePath;

/**
 * 分片(/{jobName}/sharding)监听管理器.
 * @author xiongsl
 */
public final class ShardingNodeManager extends AbstractNodeManager {
	private final JobEventNode shardsNode;
    
    public ShardingNodeManager(final String jobName) {
        super(jobName);
        shardsNode = new JobEventNode(jobName, JobNodePath.SHARDING_NODE);
    }
    
    @Override
    public void start() {
    	shardsNode.addDataListener(new ShardingDisabledListener());
    }
    
    class ShardingDisabledListener extends JobNodeListener{

        //SOURCE: ShardingOperateAPIImpl.disable()
		@Override
		protected void dataChanged(String path, Type eventType, String data) {
			if (isShutdown()) {
				return;
			}
			if (path.endsWith("/disabled")) {  //不用做任何事，下一次执行的时候自动绕过
				String shardPath = path.substring(0, path.indexOf("/disabled"));
				int shardId = Integer.parseInt(shardPath.substring(shardPath.lastIndexOf("/")+1));
				boolean isLocal = shardsNode.isLocalShard(shardPath);
				if (Type.NODE_ADDED == eventType) { //disable
					if (logger.isInfoEnabled() && isLocal) {
						logger.info("Job(name={})'s item:{} has been disabled.", jobName,shardId);
					}
				} else if (Type.NODE_REMOVED == eventType) {  //enable
					if (logger.isInfoEnabled() && isLocal) {
						logger.info("Job(name={})'s item:{} is activated again.", jobName,shardId);
					}
				}
			} else if (path.endsWith("misfire")) {
				if (logger.isInfoEnabled()) {
					logger.info("Job(name={}) is misfired!!", jobName);
				}
			}
		}
    	
    }
    
}
