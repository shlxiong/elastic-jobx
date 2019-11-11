package com.openxsl.elasticjob.health;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;

import com.dangdang.ddframe.job.lite.internal.instance.InstanceNode;
import com.dangdang.ddframe.job.lite.internal.manager.JobEventNode;
import com.dangdang.ddframe.job.lite.internal.schedule.JobRegistry;
import com.dangdang.ddframe.job.lite.internal.storage.JobNodePath;
import com.dangdang.ddframe.job.reg.zookeeper.ZookeeperRegistryCenter;
import com.openxsl.config.Environment;
import com.openxsl.config.autodetect.ScanConfig;
import com.openxsl.config.condition.ConditionalOnPresent;
import com.openxsl.elasticjob.SimpleJob;

/**
 * 健康检查，检查作业Shutdown、Disable状态改变（仅限控制台启用）
 * 
 * @author xiongsl
 * @created 2019-05-30
 */
@ScanConfig
@ConditionalOnPresent(classes="com.dangdang.ddframe.job.lite.console.ConsoleBootstrap")
@SimpleJob(name="JobHealthCheckTask", cron="30 0/10 * * * ?", description="Job健康检查")
public class JobHealthCheck {
	@Autowired
	private ZookeeperRegistryCenter registry;
	private Set<String> jobNames = new HashSet<String>();
	private boolean skipDev = false;
	private JobsShutdownListener listener;
	
	/**
	 * 刷新监听器（JobsShutdownListener）
	 */
	@PostConstruct
	public void init() {
		skipDev = this.skipDev();
		if (skipDev) {
			return;
		}
		
		listener = new JobsShutdownListener(registry);
		this.synchronizeJobNames();
		
		List<String> deadJobs = new ArrayList<String>();
		for (String jobName : jobNames) {
			String instancePath = new InstanceNode(jobName).getInstanceFullPath();
			if (registry.getChildrenKeys(instancePath).size() < 1) {
				deadJobs.add(jobName);
			}
		}
		final int len = deadJobs.size();
		for (int i=0; i<len; i+=10) {
			int size = Math.min(i+10, len);
			JobsShutdownListener.notice(deadJobs.subList(i, size).toString());
		}
	}
	
	public String jobExec(String jobParam, String shardParam, int shardId) {
		this.synchronizeJobNames();
		return null;
	}
	
	private synchronized void synchronizeJobNames() {
		if (skipDev) {
			return;
		}
		List<String> children;
		for (String namespace : registry.getChildrenKeys("/")) {
			children = registry.getChildrenKeys("/"+namespace);
			if (children.contains(JobNodePath.INSTANCES_NODE) && children.contains(JobNodePath.CONFIG_NODE)) {
				//没有namespace，本身就是jobName
				this.registerStatusListener(namespace);
			} else {
				for (String jobName : children) {
					String fullName = namespace+"/"+jobName;
					this.registerStatusListener(fullName);
				}
			}
		}
	}
	
	/**
	 * 判断是否为开发环境（"test"：由于"dev"已被写死）
	 */
	private boolean skipDev() {
		String[] environs = Environment.getSpringEnvironment().getActiveProfiles();
		for (String envir : environs) {
			if ("test".equalsIgnoreCase(envir)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * 注册监听器
	 * @param fullName
	 */
	private void registerStatusListener(String fullName) {
		if (!jobNames.contains(fullName)) {   //!equals(JobHealthCheckTask)
			jobNames.add(fullName);
			JobRegistry.getInstance().addRegCenter(fullName, registry);
			String instancePath = new JobNodePath(fullName).getInstancesNodePath();  //"/jobName/instances"
			if (registry.getRawCache(instancePath) == null) {
				registry.addCacheData(instancePath);
			}
			new JobEventNode(fullName, JobNodePath.INSTANCES_NODE)
						.addDataListener(listener);
		}
	}
	
}
