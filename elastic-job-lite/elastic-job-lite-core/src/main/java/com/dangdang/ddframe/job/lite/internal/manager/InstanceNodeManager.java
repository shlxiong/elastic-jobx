package com.dangdang.ddframe.job.lite.internal.manager;

import java.util.List;

import org.apache.curator.framework.recipes.cache.TreeCacheEvent.Type;

import com.dangdang.ddframe.job.lite.config.LiteJobConfiguration;
import com.dangdang.ddframe.job.lite.internal.failover.FailoverService;
import com.dangdang.ddframe.job.lite.internal.instance.InstanceOperation;
import com.dangdang.ddframe.job.lite.internal.instance.InstanceService;
import com.dangdang.ddframe.job.lite.internal.schedule.JobRegistry;
import com.dangdang.ddframe.job.lite.internal.schedule.JobServiceRegistry;
import com.dangdang.ddframe.job.lite.internal.schedule.SchedulerFacade;
import com.dangdang.ddframe.job.lite.internal.sharding.ShardingService;
import com.dangdang.ddframe.job.lite.internal.storage.JobNodePath;

/**
 * "/instances"节点
 * @author xiongsl
 *
 */
public class InstanceNodeManager extends AbstractNodeManager{
	private final JobEventNode instanceNode;
	private final InstanceService instanceService;
	private final SchedulerFacade schedulerFacade;
	private final FailoverService failoverService;
	private final ShardingService shardingService;

	protected InstanceNodeManager(final String jobName, SchedulerFacade scheduler) {
		super(jobName);
		instanceNode = new JobEventNode(jobName, JobNodePath.INSTANCES_NODE);
		instanceService = JobServiceRegistry.getInstance().getInstanceService(jobName);
		failoverService = JobServiceRegistry.getInstance().getFailoverService(jobName);
		shardingService = JobServiceRegistry.getInstance().getShardService(jobName);
		schedulerFacade = scheduler;
	}

	@Override
	public void start() {
		instanceNode.addDataListener(new JobTriggerStatusListener());
		instanceNode.addDataListener(new InstanceShutdownStatusListener());
		instanceNode.addDataListener(new ShardingListener());
		instanceNode.addConnectionListener();
	}
	
	/**
	 * 添加了"TRIGGER"值，表示要立即执行任务
	 * @author xiongsl
	 */
	public class JobTriggerStatusListener extends JobNodeListener{

		//SOURCE: JobOperateAPIImpl.trigger()->perist("", "TRIGGER")
		@Override
		protected void dataChanged(String path, Type eventType, String data) {
			if (Type.NODE_UPDATED != eventType || !InstanceOperation.TRIGGER.name().equals(data)
					|| isShutdown()) {
                return;
            }
			if (instanceNode.isLocalInstance(path)) {
	            instanceService.clearTriggerFlag();  //immediate reset ''
	            if (!JobRegistry.getInstance().isJobRunning(jobName)) {
	            	if (logger.isInfoEnabled()) {
	            		logger.info("Job(name={}) is triggered.", jobName);
	            	}
	            	// TODO 目前是作业运行时不能触发, 未来改为堆积式触发
	                JobRegistry.getInstance().getJobScheduleController(jobName).triggerJob();
	            }
			}
		}
		
	}
	
	/**
	 * 本实例的instance节点被移除了或与注册中心失去联系
	 * @author xiongsl
	 */
	class InstanceShutdownStatusListener extends JobNodeListener {
        
		//SOURCE: JobOperateAPIImpl.shutdown()
        @Override
        protected void dataChanged(final String path, final Type eventType, final String data) {
        	if (Type.NODE_REMOVED != eventType) {
        		return;
        	}
        	if (instanceNode.isLocalInstance(path)) {
        		if (!isShutdown() && !isPaused()) {  //?why !paused
        			if (logger.isInfoEnabled()) {
        				logger.info("Job(name={}) has been shutdown.", jobName);
        			}
            		schedulerFacade.shutdownInstance();
            	}
        	} else {  //其他节点挂了
        		if (isFailoverEnabled()) {
        			String jobInstanceId = path.substring(instanceNode.getFullPath().length() + 1);
        			//"/sharding/{1}/failover"
                    List<Integer> failoverItems = failoverService.getFailoverItems(jobInstanceId);
                    failoverItems.addAll(instanceService.getShardingItems(jobInstanceId,true));
                    if (logger.isInfoEnabled()) {
                    	logger.info("Job(name={})'s instance {} has been shutdown, and its items:{} MAY be failovered.",
                    				jobName,jobInstanceId,failoverItems);
                    }
                    for (int each : failoverItems) {
                        failoverService.setCrashedFailoverFlag(each);  //leader/failover/items/{1}
                        failoverService.failoverIfNecessary();         //executeInLeader
                    }
        		}
        	}
//            if (Type.NODE_REMOVED == eventType && instanceNode.isLocalInstance(path)) {
//            	if (!isShutdown() && !isPaused()) {  //?why !paused
//            		schedulerFacade.shutdownInstance();
//            	}
//            }
        }
        
        private boolean isFailoverEnabled() {
            LiteJobConfiguration jobConfig = JobServiceRegistry.getInstance().getConfigService(jobName)
            			.load(true);
            return null != jobConfig && jobConfig.isFailover();
        }
    }
	
	/**
	 * 重新分片
	 * @author xiongsl
	 */
	class ShardingListener extends JobNodeListener {

		@Override
		protected void dataChanged(String path, Type eventType, String data) {
			if (Type.NODE_REMOVED == eventType && !instanceNode.isLocalInstance(path)
					|| Type.NODE_ADDED == eventType) {
				if (!isShutdown()) {
					shardingService.setReshardingFlag();
					if (logger.isInfoEnabled()) {
	                	logger.info("Job(name={}) will be resharding...", jobName);
	                }
				}
			}
		}
		
	}
	
	/**
	 * 节点挂了，接管其他的 shard
	 * @author xiongsl
	 *
	class JobCrashedListener extends JobNodeListener {
		private final FailoverService failoverService = 
						JobServiceRegistry.getInstance().getFailoverService(jobName);
		//SOURCE: JobOperateAPIImpl.shutdown()
		@Override
        protected void dataChanged(final String path, final Type eventType, final String data) {
            if (Type.NODE_REMOVED == eventType && isFailoverEnabled()) {
                if (instanceNode.isLocalInstance(path)) {
                    return;
                }
                String jobInstanceId = path.substring(instanceNode.getFullPath().length() + 1);
                //"/leader/failover/items"
                List<Integer> failoverItems = failoverService.getFailoverItems(jobInstanceId);
                if (!failoverItems.isEmpty()) {   //sharding/1/failover
                    for (int each : failoverItems) {
                        failoverService.setCrashedFailoverFlag(each);  //leader/failover/items
                        failoverService.failoverIfNecessary();  //executeInLeader
                    }
                } else {
                    for (int each : instanceService.getShardingItems(jobInstanceId,null)) {
                        failoverService.setCrashedFailoverFlag(each);
                        failoverService.failoverIfNecessary();
                    }
                }
            }
        }
        
        private boolean isFailoverEnabled() {
            LiteJobConfiguration jobConfig = JobServiceRegistry.getInstance().getConfigService(jobName)
            			.load(true);
            return null != jobConfig && jobConfig.isFailover();
        }
    } */

}
