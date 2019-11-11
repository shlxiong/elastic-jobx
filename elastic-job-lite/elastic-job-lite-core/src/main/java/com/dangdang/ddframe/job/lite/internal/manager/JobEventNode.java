package com.dangdang.ddframe.job.lite.internal.manager;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.TreeCache;

import com.dangdang.ddframe.job.lite.api.strategy.JobInstance;
import com.dangdang.ddframe.job.lite.internal.schedule.JobRegistry;
import com.dangdang.ddframe.job.reg.base.CoordinatorRegistryCenter;

/**
 * 事件节点
 * @author xiongsl
 */
public class JobEventNode {
	private final String jobName;
	private final JobInstance jobInstance;
	private final String fullPath;
	private final CoordinatorRegistryCenter regCenter;
	
	public JobEventNode(String jobName, String path) {
		this.jobName = jobName;
		this.fullPath = "/"+jobName + "/"+path;
		this.regCenter = JobRegistry.getInstance().getRegCenter(jobName);
		jobInstance = JobRegistry.getInstance().getJobInstance(jobName);
	}
	
	/**
	 * 注册数据监听器
	 * @param listener 数据监听器
	 */
	public void addDataListener(final JobNodeListener listener) {
        TreeCache cache = (TreeCache)regCenter.getRawCache(fullPath);
        cache.getListenable().addListener(listener);
    }
	
	/**
	 * 监听Zookeeper连接
	 */
	public void addConnectionListener() {
		CuratorFramework client = (CuratorFramework)regCenter.getRawClient();
		client.getConnectionStateListenable().addListener(
				new RegCenterConnectionListener(jobName));
	}
	
	public boolean isLocalInstance(String instancePath) {//{jobName}/instances/{instanceId}
		String instanceId = instancePath.substring(fullPath.length()+1);
		if (instanceId == null) {
			return false;
		}
		return jobInstance.getJobInstanceId().equals(instanceId);
	}
	public boolean isLocalServer(String serverPath) {//{jobName}/servers/{host}
		String serverIp = serverPath.substring(fullPath.length()+1);
		if (serverIp == null) {
			return false;
		}
		return jobInstance.getIp().equals(serverIp);
	}
	public boolean isLocalShard(String shardingPath) {//{jobName}/shardings/{item}
		String instanceId = regCenter.getDirectly(shardingPath+"/instance");
		return jobInstance.getJobInstanceId().equals(instanceId);
	}
	public String getFullPath() {
		return fullPath;
	}
	public String getJobName() {
		return jobName;
	}

}
