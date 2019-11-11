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

package com.dangdang.ddframe.job.lite.internal.manager;

import org.apache.curator.framework.recipes.cache.TreeCacheEvent.Type;

import com.dangdang.ddframe.job.config.JobCoreConfiguration;
import com.dangdang.ddframe.job.lite.config.LiteJobConfiguration;
import com.dangdang.ddframe.job.lite.internal.config.LiteJobConfigurationGsonFactory;
import com.dangdang.ddframe.job.lite.internal.failover.FailoverService;
import com.dangdang.ddframe.job.lite.internal.schedule.JobRegistry;
import com.dangdang.ddframe.job.lite.internal.schedule.JobServiceRegistry;
import com.dangdang.ddframe.job.lite.internal.sharding.ExecutionService;
import com.dangdang.ddframe.job.lite.internal.sharding.ShardingService;
import com.dangdang.ddframe.job.lite.internal.storage.JobNodePath;
import com.dangdang.ddframe.job.util.QuartzUtils;

/**
 * 重调度监听管理器.
 * 
 * @author caohao
 * @author zhangliang
 */
public final class ConfigNodeManager extends AbstractNodeManager {
    private final JobEventNode configNode;
    
    public ConfigNodeManager(final String jobName) {
        super(jobName);
        configNode = new JobEventNode(jobName, JobNodePath.CONFIG_NODE);
    }
    
    @Override
    public void start() {
    	configNode.addDataListener(new CronSettingChangedJobListener());
    	configNode.addDataListener(new ShardingTotalCountChangedJobListener());
    	configNode.addDataListener(new FailoverSettingsChangedListener());
    	configNode.addDataListener(new MonitorExecutionSettingsChangedListener());
    	//TODO MISFIRE
    }
    
    private final LiteJobConfiguration getLiteConfiguration(String data) {
    	return LiteJobConfigurationGsonFactory.fromJson(data);
    }
    private final JobCoreConfiguration getCoreConfiguration(String data) {
    	return LiteJobConfigurationGsonFactory.fromJson(data)
    				.getTypeConfig().getCoreConfig();
    }
    
    /**
     * 修改Cron表达式，则重新scheduleJob
     * @author xiongsl
     */
    class CronSettingChangedJobListener extends JobNodeListener {
        
    	//SOURCE: JobSettingsAPIImpl.updateJobSettings()->perist("/config", json)
        @Override
        protected void dataChanged(final String path, final Type eventType, final String data) {
        	String old = JobRegistry.getInstance().getJobCron(jobName);
        	String cron = getCoreConfiguration(data).getCron();
        	QuartzUtils.checkCronExpress(cron);
            if (Type.NODE_UPDATED == eventType && !isShutdown() && (old==null || !cron.equals(old))) {
                JobRegistry.getInstance().getJobScheduleController(jobName).rescheduleJob(cron);
                JobRegistry.getInstance().setJobCron(jobName, cron);
            }
        }
    }
    
    class ShardingTotalCountChangedJobListener extends JobNodeListener {
    	private final ShardingService shardingService =
    					JobServiceRegistry.getInstance().getShardService(jobName);
        @Override
        protected void dataChanged(final String path, final Type eventType, final String data) {
        	int shards = JobRegistry.getInstance().getCurrentShardingTotalCount(jobName);
            if (Type.NODE_UPDATED == eventType && 0 != shards) {
                int newShardingTotalCount = getCoreConfiguration(data).getShardingTotalCount();
                if (newShardingTotalCount != shards) {
                    shardingService.setReshardingFlag();
                    JobRegistry.getInstance().setCurrentShardingTotalCount(jobName, newShardingTotalCount);
                }
            }
        }
    }
    
    class FailoverSettingsChangedListener extends JobNodeListener {
    	private final FailoverService failoverService = 
    					JobServiceRegistry.getInstance().getFailoverService(jobName);
        @Override
        protected void dataChanged(final String path, final Type eventType, final String data) {
            if (Type.NODE_UPDATED == eventType && !getLiteConfiguration(data).isFailover()) {
                failoverService.removeFailoverInfo();
            }
        }
    }
    
    class MonitorExecutionSettingsChangedListener extends JobNodeListener {
    	private final ExecutionService executionService =
    					JobServiceRegistry.getInstance().getExecutionService(jobName);
        
        @Override
        protected void dataChanged(final String path, final Type eventType, final String data) {
            if (Type.NODE_UPDATED == eventType && !getLiteConfiguration(data).isMonitorExecution()) {
                executionService.clearAllRunningInfo();
            }
        }
    }
    
}
