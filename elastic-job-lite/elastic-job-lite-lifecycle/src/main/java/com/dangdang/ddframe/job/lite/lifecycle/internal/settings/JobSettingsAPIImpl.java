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

package com.dangdang.ddframe.job.lite.lifecycle.internal.settings;

import com.dangdang.ddframe.job.api.JobType;
import com.dangdang.ddframe.job.config.JobCoreConfiguration;
import com.dangdang.ddframe.job.config.dataflow.DataflowJobConfiguration;
import com.dangdang.ddframe.job.config.script.ScriptJobConfiguration;
import com.dangdang.ddframe.job.lite.config.LiteJobConfiguration;
import com.dangdang.ddframe.job.lite.internal.config.LiteJobConfigurationGsonFactory;
import com.dangdang.ddframe.job.lite.internal.storage.JobNodePath;
import com.dangdang.ddframe.job.lite.lifecycle.api.JobSettingsAPI;
import com.dangdang.ddframe.job.lite.lifecycle.domain.JobSettings;
import com.dangdang.ddframe.job.reg.base.CoordinatorRegistryCenter;
import com.dangdang.ddframe.job.util.QuartzUtils;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import lombok.RequiredArgsConstructor;

/**
 * 作业配置的实现类.
 *
 * @author zhangliang
 * @author caohao
 */
@RequiredArgsConstructor
public final class JobSettingsAPIImpl implements JobSettingsAPI {
    
    private final CoordinatorRegistryCenter regCenter;
    
    @Override
    public JobSettings getJobSettings(final String jobName) {
        JobSettings result = new JobSettings();
        JobNodePath jobNodePath = new JobNodePath(jobName);
        LiteJobConfiguration liteJobConfig = LiteJobConfigurationGsonFactory.fromJson(regCenter.get(jobNodePath.getConfigNodePath()));
        String jobType = liteJobConfig.getTypeConfig().getJobType().name();
        buildSimpleJobSettings(jobName, result, liteJobConfig);
        if (JobType.DATAFLOW.name().equals(jobType)) {
            buildDataflowJobSettings(result, (DataflowJobConfiguration) liteJobConfig.getTypeConfig());
        }
        if (JobType.SCRIPT.name().equals(jobType)) {
            buildScriptJobSettings(result, (ScriptJobConfiguration) liteJobConfig.getTypeConfig());
        }
        return result;
    }
    
    private void buildSimpleJobSettings(final String jobName, final JobSettings result, final LiteJobConfiguration liteJobConfig) {
        JobCoreConfiguration coreConfig = liteJobConfig.getTypeConfig().getCoreConfig();
    	result.setJobName(jobName);
        result.setJobType(liteJobConfig.getTypeConfig().getJobType().name());
        result.setJobClass(liteJobConfig.getTypeConfig().getJobClass());
        result.setShardingTotalCount(coreConfig.getShardingTotalCount());
        result.setCron(coreConfig.getCron());
        result.setShardingItemParameters(coreConfig.getShardingItemParameters());
        result.setJobParameter(coreConfig.getJobParameter());
        result.setMonitorExecution(liteJobConfig.isMonitorExecution());
        result.setMaxTimeDiffSeconds(liteJobConfig.getMaxTimeDiffSeconds());
        result.setMonitorPort(liteJobConfig.getMonitorPort());
        result.setFailover(coreConfig.isFailover());
        result.setMisfire(coreConfig.isMisfire());
        result.setJobShardingStrategyClass(liteJobConfig.getJobShardingStrategyClass());
        result.setDescription(coreConfig.getDescription());
        result.setReconcileIntervalMinutes(liteJobConfig.getReconcileIntervalMinutes());
        //TODO("XIONGSL")
        result.getJobProperties().putAll(coreConfig.getJobProperties().getAll());
    }
    
    private void buildDataflowJobSettings(final JobSettings result, final DataflowJobConfiguration config) {
        result.setStreamingProcess(config.isStreamingProcess());
    }
    
    private void buildScriptJobSettings(final JobSettings result, final ScriptJobConfiguration config) {
        result.setScriptCommandLine(config.getScriptCommandLine());
    }
    
    @Override
    public void updateJobSettings(final JobSettings jobSettings) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(jobSettings.getJobName()), "jobName can not be empty.");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(jobSettings.getCron()), "cron can not be empty.");
        Preconditions.checkArgument(jobSettings.getShardingTotalCount() > 0, "shardingTotalCount should larger than zero.");
        JobNodePath jobNodePath = new JobNodePath(jobSettings.getJobName());
        //TODO("XIONGSL")
        String cron = QuartzUtils.checkCronExpress(jobSettings.getCron());
        jobSettings.setCron(cron);
        regCenter.update(jobNodePath.getConfigNodePath(), LiteJobConfigurationGsonFactory.toJsonForObject(jobSettings));
    }
    
    @Override
    public void removeJobSettings(final String jobName) {
        regCenter.remove("/" + jobName);
    }
    
}
