package com.dangdang.ddframe.job.lite.internal.schedule;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.dangdang.ddframe.job.lite.api.JobScheduler;
import com.dangdang.ddframe.job.lite.internal.config.ConfigurationService;
import com.dangdang.ddframe.job.lite.internal.election.LeaderService;
import com.dangdang.ddframe.job.lite.internal.failover.FailoverService;
import com.dangdang.ddframe.job.lite.internal.guarantee.GuaranteeService;
import com.dangdang.ddframe.job.lite.internal.instance.InstanceService;
import com.dangdang.ddframe.job.lite.internal.monitor.MonitorService;
import com.dangdang.ddframe.job.lite.internal.reconcile.ReconcileService;
import com.dangdang.ddframe.job.lite.internal.server.ServerService;
import com.dangdang.ddframe.job.lite.internal.sharding.ExecutionContextService;
import com.dangdang.ddframe.job.lite.internal.sharding.ExecutionService;
import com.dangdang.ddframe.job.lite.internal.sharding.ShardingService;
import com.dangdang.ddframe.job.lite.internal.storage.JobNodePath;
import com.dangdang.ddframe.job.reg.base.CoordinatorRegistryCenter;

/**
 * 缓存Job的Service对象
 * @author xiongsl
 */
public class JobServiceRegistry {
	private static volatile JobServiceRegistry instance;
	private final Map<String, ConfigurationService> configServices = new ConcurrentHashMap<>();
	private final Map<String, LeaderService> leaderServices = new ConcurrentHashMap<>();
	private final Map<String, ShardingService> shardServices = new ConcurrentHashMap<>();
	private final Map<String, InstanceService> instanceServices = new ConcurrentHashMap<>();
	private final Map<String, ServerService> serverServices = new ConcurrentHashMap<>();
	private final Map<String, ExecutionService> executeServices = new ConcurrentHashMap<>();
	private final Map<String, ExecutionContextService> contextServices = new ConcurrentHashMap<>();
	private final Map<String, FailoverService> failoverServices = new ConcurrentHashMap<>();
	private final Map<String, GuaranteeService> guaranteServices = new ConcurrentHashMap<>();
	private final Map<String, MonitorService> monitorServices = new ConcurrentHashMap<>();
	private final Map<String, ReconcileService> reconcileServices = new ConcurrentHashMap<>();
	
	public static synchronized JobServiceRegistry getInstance() {
		if (instance == null) {
			instance = new JobServiceRegistry();
		}
		return instance;
	}
	
	/**
	 * 一次性注册所有的服务
	 * @see JobScheduler
	 * @param jobName
	 */
	public void register(String jobName, CoordinatorRegistryCenter regCenter) {
		//注意初始化顺序
		configServices.putIfAbsent(jobName, new ConfigurationService(regCenter, jobName));
		serverServices.putIfAbsent(jobName, new ServerService(regCenter, jobName));
		instanceServices.putIfAbsent(jobName, new InstanceService(regCenter, jobName));  //server
		leaderServices.putIfAbsent(jobName, new LeaderService(regCenter, jobName));  //server
		executeServices.putIfAbsent(jobName, new ExecutionService(regCenter, jobName));  //config
		contextServices.putIfAbsent(jobName, new ExecutionContextService(regCenter, jobName));  //config
		shardServices.putIfAbsent(jobName, new ShardingService(regCenter, jobName));  //above all
		guaranteServices.putIfAbsent(jobName, new GuaranteeService(regCenter, jobName)); //config
		monitorServices.putIfAbsent(jobName, new MonitorService(regCenter, jobName)); //config
		failoverServices.putIfAbsent(jobName, new FailoverService(regCenter, jobName));  //shard
		reconcileServices.putIfAbsent(jobName, new ReconcileService(regCenter, jobName)); //config,leader,shard
		
		JobRegistry.getInstance().addRegCenter(jobName, regCenter);
		String jobKey = "/" + jobName +"/";
		regCenter.addCacheData(jobKey+JobNodePath.CONFIG_NODE);
        regCenter.addCacheData(jobKey+JobNodePath.INSTANCES_NODE);
        regCenter.addCacheData(jobKey+JobNodePath.SERVERS_NODE);
        regCenter.addCacheData(jobKey+JobNodePath.SHARDING_NODE);
        regCenter.addCacheData(jobKey+JobNodePath.LEADER_HOST_NODE);
        regCenter.addCacheData(jobKey+JobNodePath.GUARANTEE_NODE);
	}
	
	public ConfigurationService getConfigService(String jobName) {
		return configServices.get(jobName);
	}
	
	public LeaderService getLeaderService(String jobName) {
		return leaderServices.get(jobName);
	}
	
	public ShardingService getShardService(String jobName) {
		return shardServices.get(jobName);
	}
	
	public ServerService getServerService(String jobName) {
		return serverServices.get(jobName);
	}
	
	public InstanceService getInstanceService(String jobName) {
		return instanceServices.get(jobName);
	}
	
	public ExecutionService getExecutionService(String jobName) {
		return executeServices.get(jobName);
	}
	
	public ExecutionContextService getExecutionContextService(String jobName) {
		return contextServices.get(jobName);
	}
	
	public FailoverService getFailoverService(String jobName) {
		return failoverServices.get(jobName);
	}
	
	public GuaranteeService getGuaranteeService(String jobName) {
		return guaranteServices.get(jobName);
	}
	
	public MonitorService getMonitorService(String jobName) {
		return monitorServices.get(jobName);
	}
	
	public ReconcileService getReconcileService(String jobName) {
		return reconcileServices.get(jobName);
	}

}
