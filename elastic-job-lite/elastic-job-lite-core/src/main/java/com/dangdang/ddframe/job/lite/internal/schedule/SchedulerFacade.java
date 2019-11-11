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

package com.dangdang.ddframe.job.lite.internal.schedule;

import java.util.List;

import com.dangdang.ddframe.job.lite.api.listener.ElasticJobListener;
import com.dangdang.ddframe.job.lite.config.LiteJobConfiguration;
import com.dangdang.ddframe.job.lite.internal.config.ConfigurationService;
import com.dangdang.ddframe.job.lite.internal.election.LeaderService;
import com.dangdang.ddframe.job.lite.internal.failover.FailoverService;
import com.dangdang.ddframe.job.lite.internal.instance.InstanceService;
import com.dangdang.ddframe.job.lite.internal.listener.ListenerManager;
import com.dangdang.ddframe.job.lite.internal.manager.JobNodesManager;
import com.dangdang.ddframe.job.lite.internal.monitor.MonitorService;
import com.dangdang.ddframe.job.lite.internal.reconcile.ReconcileService;
import com.dangdang.ddframe.job.lite.internal.server.ServerService;
import com.dangdang.ddframe.job.lite.internal.sharding.ExecutionService;
import com.dangdang.ddframe.job.lite.internal.sharding.ShardingService;
import com.dangdang.ddframe.job.reg.base.CoordinatorRegistryCenter;

import lombok.Getter;

/**
 * 为调度器提供内部服务的门面类.
 * 
 * @author zhangliang
 * @author xiongsl XXXService get方法，JobNodesManager替代ListenerManager
 */
public final class SchedulerFacade {
	@Getter
    private final String jobName;
    
    @Getter
    private final ConfigurationService configService;
    @Getter
    private final LeaderService leaderService;
    @Getter
    private final ServerService serverService;
    @Getter
    private final InstanceService instanceService;
    @Getter
    private final ShardingService shardingService;
    @Getter
    private final ExecutionService executionService;
    @Getter
    private final MonitorService monitorService;
    @Getter
    private final ReconcileService reconcileService;
    @Getter
    private final FailoverService failoverService;
    
    @SuppressWarnings("unused")
	private ListenerManager listenerManager;
    private JobNodesManager jobNodeMgr;
    
    public SchedulerFacade(final CoordinatorRegistryCenter regCenter, final String jobName) {
        this.jobName = jobName;
        JobServiceRegistry registry = JobServiceRegistry.getInstance();
        configService = registry.getConfigService(jobName);// new ConfigurationService(regCenter, jobName);
        leaderService = registry.getLeaderService(jobName);// new LeaderService(regCenter, jobName);
        serverService = registry.getServerService(jobName);// new ServerService(regCenter, jobName);
        instanceService = registry.getInstanceService(jobName);// new InstanceService(regCenter, jobName);
        shardingService = registry.getShardService(jobName);// new ShardingService(regCenter, jobName);
        executionService = registry.getExecutionService(jobName);// new ExecutionService(regCenter, jobName);
        monitorService = registry.getMonitorService(jobName);// new MonitorService(regCenter, jobName);
        reconcileService = registry.getReconcileService(jobName);// new ReconcileService(regCenter, jobName);
        failoverService = registry.getFailoverService(jobName);// new FailoverService(regCenter, jobName);
    }
    
    public SchedulerFacade(final CoordinatorRegistryCenter regCenter, final String jobName, final List<ElasticJobListener> elasticJobListeners) {
        this(regCenter, jobName);
        listenerManager = new ListenerManager(regCenter, jobName, elasticJobListeners);
        jobNodeMgr = new JobNodesManager(regCenter, jobName, this, elasticJobListeners);
    }
    
    /**
     * 获取作业触发监听器.
     *
     * @return 作业触发监听器
     */
    public JobTriggerListener newJobTriggerListener() {
        return new JobTriggerListener(executionService, shardingService);
    }
    
    /**
     * 更新作业配置.
     *
     * @param liteJobConfig 作业配置
     * @return 更新后的作业配置
     */
    public LiteJobConfiguration updateJobConfiguration(final LiteJobConfiguration liteJobConfig) {
        configService.persist(liteJobConfig);
        return configService.load(false);
    }
    
    /**
     * 注册作业启动信息.
     * 
     * @param enabled 作业是否启用
     */
    public void registerStartUpInfo(final boolean enabled) {
    	jobNodeMgr.startAllListeners();
//        listenerManager.startAllListeners();
        
        leaderService.electLeader();
        serverService.persistOnline(enabled);
        instanceService.persistOnline();
        shardingService.setReshardingFlag();
        monitorService.listen();
        if (!reconcileService.isRunning()) {
            reconcileService.startAsync();
        }
    }
    
    /**
     * 终止一个实例，removeLeader， closeMonitor，stopAsync，JobRegistry.shutdown()
     */
    public void shutdownInstance() {
        if (leaderService.isLeader()) {
            leaderService.removeLeader();
        }
        monitorService.close();
        if (reconcileService.isRunning()) {
            reconcileService.stopAsync();
        }
        JobRegistry.getInstance().shutdown(jobName);
    }
}
