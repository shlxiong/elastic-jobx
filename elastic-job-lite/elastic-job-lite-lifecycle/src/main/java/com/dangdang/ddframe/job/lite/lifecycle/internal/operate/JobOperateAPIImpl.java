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

package com.dangdang.ddframe.job.lite.lifecycle.internal.operate;

import java.util.Collections;
import java.util.List;

import com.dangdang.ddframe.job.lite.internal.storage.JobNodePath;
import com.dangdang.ddframe.job.lite.lifecycle.api.JobOperateAPI;
import com.dangdang.ddframe.job.lite.lifecycle.api.JobStatisticsAPI;
import com.dangdang.ddframe.job.lite.lifecycle.domain.JobBriefInfo.JobStatus;
import com.dangdang.ddframe.job.reg.base.CoordinatorRegistryCenter;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

/**
 * 操作作业的实现类.
 *
 * @author caohao
 */
public final class JobOperateAPIImpl implements JobOperateAPI {
    
    private final CoordinatorRegistryCenter regCenter;
    //TODO("XIONGSL")
    private final JobStatisticsAPI jobStatis;
    
    public JobOperateAPIImpl(final CoordinatorRegistryCenter regCenter) {
        this.regCenter = regCenter;
        jobStatis = new JobStatisticsAPI(regCenter);
    }
    
    @Override
    public void trigger(final Optional<String> jobName, final Optional<String> serverIp) {
        if (jobName.isPresent()) {
            JobNodePath jobNodePath = new JobNodePath(jobName.get());
            for (String each : regCenter.getChildrenKeys(jobNodePath.getInstancesNodePath())) {
                regCenter.persist(jobNodePath.getInstanceNodePath(each), "TRIGGER");
            }
        }
    }
    
    @Override
    public void disable(final Optional<String> jobName, final Optional<String> serverIp) {
        disableOrEnableJobs(jobName, serverIp, true);
    }
    
    @Override
    public void enable(final Optional<String> jobName, final Optional<String> serverIp) {
        disableOrEnableJobs(jobName, serverIp, false);
    }
    
    private void disableOrEnableJobs(final Optional<String> jobName, final Optional<String> serverIp, final boolean disabled) {
        Preconditions.checkArgument(jobName.isPresent() || serverIp.isPresent(), "At least indicate jobName or serverIp.");
        if (serverIp.isPresent()) {  //服务器维度
        	List<String> jobs = jobName.isPresent() ? Collections.singletonList(jobName.get())
        					: jobStatis.getJobNames();
        	for (String job : jobs) {
        		persistDisabledOrEnabledJob(job, serverIp.get(), disabled);
        	}
        } else {  //作业维度
        	JobNodePath jobNodePath = new JobNodePath(jobName.get());
            for (String each : regCenter.getChildrenKeys(jobNodePath.getServerNodePath())) {
                if (disabled) {
                    regCenter.persist(jobNodePath.getServerNodePath(each), JobStatus.DISABLED.name());
                } else {
                    regCenter.persist(jobNodePath.getServerNodePath(each), "");
                }
            }
        }
    }
    
    private void persistDisabledOrEnabledJob(final String jobName, final String serverIp, final boolean disabled) {
        JobNodePath jobNodePath = new JobNodePath(jobName);
        String serverNodePath = jobNodePath.getServerNodePath(serverIp);
        if (disabled) {
            regCenter.persist(serverNodePath, "DISABLED");
        } else {
            regCenter.persist(serverNodePath, "");
        }
    }
    
    @Override
    public void shutdown(final Optional<String> jobName, final Optional<String> serverIp) {
        Preconditions.checkArgument(jobName.isPresent() || serverIp.isPresent(), "At least indicate jobName or serverIp.");
        if (serverIp.isPresent()) {  //服务器维度
        	List<String> jobs = jobName.isPresent() ? Collections.singletonList(jobName.get())
							: jobStatis.getJobNames();
        	for (String job : jobs) {
        		 JobNodePath jobNodePath = new JobNodePath(job);
        		 for (String each : regCenter.getChildrenKeys(jobNodePath.getInstancesNodePath())) {
                     if (serverIp.get().equals(each.split("@-@")[0])) {
                         regCenter.remove(jobNodePath.getInstanceNodePath(each));
                     }
                 }
        		 this.removeShardings(jobNodePath, serverIp);
        	}
        } else {  //作业维度
        	JobNodePath jobNodePath = new JobNodePath(jobName.get());
            for (String each : regCenter.getChildrenKeys(jobNodePath.getInstancesNodePath())) {
                regCenter.remove(jobNodePath.getInstanceNodePath(each));
            }
            this.removeShardings(jobNodePath, serverIp);
        }
    }
    //TODO("XIONGSL")
    private void removeShardings(JobNodePath jobNodePath, final Optional<String> serverIp) {
    	String instanceIp;
    	if (serverIp.isPresent()) {
    		for (String each : regCenter.getChildrenKeys(jobNodePath.getShardingNodePath())) {
    			String path = jobNodePath.getShardingNodePath(each, "instance");
    			instanceIp = regCenter.getDirectly(path).split("@-@")[0];
    			if (serverIp.get().equals(instanceIp)) {
    				regCenter.remove(path);
    			}
    		}
    	} else {
    		for (String each : regCenter.getChildrenKeys(jobNodePath.getShardingNodePath())) {
    			String path = jobNodePath.getShardingNodePath(each, "instance");
            	regCenter.remove(path);
            }
    	}
    }
    
    @Override
    public void removeServer(final Optional<String> jobName, final Optional<String> serverIp) {
        shutdown(jobName, serverIp);
        if (jobName.isPresent() && serverIp.isPresent()) {
            regCenter.remove(new JobNodePath(jobName.get()).getServerNodePath(serverIp.get()));
//        } else if (jobName.isPresent()) {  //没有这个用例
//            JobNodePath jobNodePath = new JobNodePath(jobName.get());
//            List<String> servers = regCenter.getChildrenKeys(jobNodePath.getServerNodePath());
//            for (String each : servers) {
//                regCenter.remove(jobNodePath.getServerNodePath(each));
//            }
        } else if (serverIp.isPresent()) {
            for (String each : jobStatis.getJobNames()) {
                regCenter.remove(new JobNodePath(each).getServerNodePath(serverIp.get()));
            }
        }
    }
}
