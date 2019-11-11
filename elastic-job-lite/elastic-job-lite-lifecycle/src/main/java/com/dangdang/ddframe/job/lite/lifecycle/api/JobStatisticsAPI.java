/*
 * Copyright 1999-2015 dangdang.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </p>
 */

package com.dangdang.ddframe.job.lite.lifecycle.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.dangdang.ddframe.job.config.JobCoreConfiguration;
import com.dangdang.ddframe.job.lite.api.strategy.JobInstance;
import com.dangdang.ddframe.job.lite.internal.config.LiteJobConfigurationGsonFactory;
import com.dangdang.ddframe.job.lite.internal.storage.JobNodePath;
import com.dangdang.ddframe.job.lite.lifecycle.domain.JobBriefInfo;
import com.dangdang.ddframe.job.lite.lifecycle.domain.JobBriefInfo.JobStatus;
import com.dangdang.ddframe.job.reg.base.CoordinatorRegistryCenter;

/**
 * 作业状态展示的API.
 * @author caohao
 */
public class JobStatisticsAPI {
	private final String NSP_PATH = "/%s";
	private final String JOB_PATH = "%s/%s";
	private final CoordinatorRegistryCenter regCenter;
	
	public JobStatisticsAPI(CoordinatorRegistryCenter regCenter){
		this.regCenter = regCenter;
	}
	
	/**
     * TODO("XIONGSL") 获得所有作业名（包括路径）
     */
	public List<String> getJobNames(){
		List<String> jobNames = new ArrayList<String>();
		for (String namespace : this.getNamespaces()) {
			for (String jobName : regCenter.getChildrenKeys(namespace)) {
				jobNames.add(String.format(JOB_PATH, namespace.substring(1),jobName));
			}
		}
		return jobNames;
	}
	private List<String> getNamespaces(){
		List<String> namespaces = new ArrayList<String>();
		for (String each : regCenter.getChildrenKeys("/")) {
			namespaces.add(String.format(NSP_PATH, each));
		}
		return namespaces;
	}

	/**
	 * 获取作业总数.
	 */
	public int getJobsTotalCount() {
		int count = 0;
		for (String namespace : this.getNamespaces()) {
			count += regCenter.getNumChildren(namespace);
		}
		return count;
	}

	/**
	 * 获取所有作业简明信息.
	 */
	public Collection<JobBriefInfo> getAllJobsBriefInfo() {
		List<JobBriefInfo> result = new ArrayList<>();
		for (String jobName : this.getJobNames()) {
			JobBriefInfo jobBriefInfo = getJobBriefInfo(jobName);
			if (null != jobBriefInfo) {
				result.add(jobBriefInfo);
			}
		}
		Collections.sort(result);
		return result;
	}

	/**
	 * 获取作业简明信息.
	 * 
	 * @param jobName
	 *            作业名称
	 * @return 作业简明信息.
	 */
	private JobBriefInfo getJobBriefInfo(final String jobName) {
		JobNodePath jobNodePath = new JobNodePath(jobName);
		JobBriefInfo result = new JobBriefInfo();
		result.setJobName(jobName);
		String liteJobConfigJson = regCenter.get(jobNodePath.getConfigNodePath());
		if (null == liteJobConfigJson) {
			return null;
		}
		JobCoreConfiguration coreConfig = LiteJobConfigurationGsonFactory.fromJson(liteJobConfigJson)
					.getTypeConfig().getCoreConfig();
		result.setDescription(coreConfig.getDescription());
		result.setCron(coreConfig.getCron());
		result.setInstanceCount(this.getJobInstanceCount(jobName));
		result.setShardingTotalCount(coreConfig.getShardingTotalCount());
		result.setStatus(getJobStatus(jobName));
		return result;
	}

	private JobStatus getJobStatus(final String jobName) {
		JobNodePath jobNodePath = new JobNodePath(jobName);
		List<String> instances = regCenter.getChildrenKeys(jobNodePath.getInstancesNodePath());
		if (instances.isEmpty()) {  //下线
			return JobStatus.CRASHED;
		}
		if (isAllDisabled(jobNodePath, instances)) {  //失效
			return JobStatus.DISABLED;
		}
		if (isHasShardingFlag(jobNodePath, instances)) {  //分片中
			return JobStatus.SHARDING_FLAG;
		}
		return JobStatus.OK;
	}

	/**
	 * 必须从存在的节点中去查找
	 * @param jobNodePath
	 * @param instances
	 */
	private boolean isAllDisabled(final JobNodePath jobNodePath, List<String> instances) {
		String serverNode = jobNodePath.getServerNodePath() + "/";
		for (String instance : instances) {
			String serverPath = serverNode + new JobInstance(instance).getIp();
			if (!JobStatus.DISABLED.name().equals(regCenter.get(serverPath))){
				return false;
			}
		}
		return true;
//		List<String> serversPath = regCenter.getChildrenKeys(jobNodePath.getServerNodePath());
//		int disabledServerCount = 0;
//		for (String each : serversPath) {
//			if (JobStatus.DISABLED.name().equals(regCenter.get(jobNodePath.getServerNodePath(each)))) {
//				disabledServerCount++;
//			}
//		}
//		return disabledServerCount == serversPath.size();
	}

	private boolean isHasShardingFlag(final JobNodePath jobNodePath, final List<String> instances) {
		Set<String> shardingInstances = new HashSet<>();
		for (String each : regCenter.getChildrenKeys(jobNodePath.getShardingNodePath())) {
			String instanceId = regCenter.get(jobNodePath.getShardingNodePath(each, "instance"));
			if (null != instanceId && !instanceId.isEmpty()) {
				shardingInstances.add(instanceId);
			}
		}
		return !instances.containsAll(shardingInstances) || shardingInstances.isEmpty();
	}

	private int getJobInstanceCount(final String jobName) {
		return regCenter.getChildrenKeys(new JobNodePath(jobName).getInstancesNodePath()).size();
	}

	/**
	 * 获取该IP下所有作业简明信息.
	 * @param ip 服务器IP
	 * @return 作业简明信息集合.
	 */
	public Collection<JobBriefInfo> getJobsBriefInfo(final String ip) {
		List<JobBriefInfo> result = new ArrayList<>();
		for (String jobName : this.getJobNames()) {
			JobBriefInfo jobBriefInfo = getJobBriefInfoByJobNameAndIp(jobName, ip);
			if (null != jobBriefInfo) {
				result.add(jobBriefInfo);
			}
		}
		Collections.sort(result);
		return result;
	}

	private JobBriefInfo getJobBriefInfoByJobNameAndIp(final String jobName, final String ip) {
		if (!regCenter.isExisted(new JobNodePath(jobName).getServerNodePath(ip))) {
			return null;
		}
		JobBriefInfo result = new JobBriefInfo();
		result.setJobName(jobName);
		result.setStatus(getJobStatusByJobNameAndIp(jobName, ip));
		result.setInstanceCount(getJobInstanceCountByJobNameAndIp(jobName, ip));
		return result;
	}

	private JobStatus getJobStatusByJobNameAndIp(final String jobName, final String ip) {
		JobNodePath jobNodePath = new JobNodePath(jobName);
		String status = regCenter.get(jobNodePath.getServerNodePath(ip));
		if ("DISABLED".equalsIgnoreCase(status)) {
			return JobStatus.DISABLED;
		} else {
			return JobStatus.OK;
		}
	}

	private int getJobInstanceCountByJobNameAndIp(final String jobName, final String ip) {
		int instanceCount = 0;
		JobNodePath jobNodePath = new JobNodePath(jobName);
		List<String> instances = regCenter.getChildrenKeys(jobNodePath.getInstancesNodePath());
		for (String each : instances) {
			if (ip.equals(each.split("@-@")[0])) {
				instanceCount++;
			}
		}
		return instanceCount;
	}
}
