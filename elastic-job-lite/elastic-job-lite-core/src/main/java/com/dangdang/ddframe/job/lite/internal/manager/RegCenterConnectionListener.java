package com.dangdang.ddframe.job.lite.internal.manager;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;

import com.dangdang.ddframe.job.lite.internal.instance.InstanceService;
import com.dangdang.ddframe.job.lite.internal.schedule.JobRegistry;
import com.dangdang.ddframe.job.lite.internal.schedule.JobScheduleController;
import com.dangdang.ddframe.job.lite.internal.schedule.JobServiceRegistry;
import com.dangdang.ddframe.job.lite.internal.server.ServerService;
import com.dangdang.ddframe.job.lite.internal.sharding.ExecutionService;
import com.dangdang.ddframe.job.lite.internal.sharding.ShardingService;

/**
 * 注册中心连接状态监听器.
 *
 * @author zhangliang
 */
public final class RegCenterConnectionListener implements ConnectionStateListener {
    
    private final String jobName;
    
    private final ServerService serverService;
    
    private final InstanceService instanceService;
    
    private final ShardingService shardingService;
    
    private final ExecutionService executionService;
    
    public RegCenterConnectionListener(final String jobName) {
        this.jobName = jobName;
        JobServiceRegistry registry = JobServiceRegistry.getInstance();
        serverService = //new ServerService(regCenter, jobName);
        		registry.getServerService(jobName);
        instanceService = //new InstanceService(regCenter, jobName);
        		registry.getInstanceService(jobName);
        shardingService = //new ShardingService(regCenter, jobName);
        		registry.getShardService(jobName);
        executionService = //new ExecutionService(regCenter, jobName);
        		registry.getExecutionService(jobName);
    }
    
    @Override
    public void stateChanged(final CuratorFramework client, final ConnectionState newState) {
        if (JobRegistry.getInstance().isShutdown(jobName)) {
            return;
        }
        System.out.println("RegCenterConnectionListener state: "+newState);
        //local?
        JobScheduleController scheduleController = JobRegistry.getInstance().getJobScheduleController(jobName);
        if (ConnectionState.SUSPENDED == newState || ConnectionState.LOST == newState) {
        	scheduleController.pauseJob();
        } else if (ConnectionState.RECONNECTED == newState) {
            serverService.persistOnline(serverService.isEnableServer(JobRegistry.getInstance().getJobInstance(jobName).getIp()));
            instanceService.persistOnline();
            executionService.clearRunningInfo(shardingService.getLocalShardingItems());
            scheduleController.resumeJob();
        }
        System.out.println("RegCenterConnectionListener end: "+newState);
    }
}
