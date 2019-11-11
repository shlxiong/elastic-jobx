package com.dangdang.ddframe.job.lite.console.spring;

import com.dangdang.ddframe.job.lite.console.service.JobAPIService;
import com.dangdang.ddframe.job.lite.lifecycle.api.JobOperateAPI;
import com.dangdang.ddframe.job.lite.lifecycle.api.JobSettingsAPI;
import com.dangdang.ddframe.job.lite.lifecycle.api.JobStatisticsAPI;
import com.dangdang.ddframe.job.lite.lifecycle.api.ServerStatisticsAPI;
import com.dangdang.ddframe.job.lite.lifecycle.api.ShardingOperateAPI;
import com.dangdang.ddframe.job.lite.lifecycle.api.ShardingStatisticsAPI;
import com.dangdang.ddframe.job.lite.lifecycle.internal.operate.JobOperateAPIImpl;
import com.dangdang.ddframe.job.lite.lifecycle.internal.operate.ShardingOperateAPIImpl;
import com.dangdang.ddframe.job.lite.lifecycle.internal.settings.JobSettingsAPIImpl;
import com.dangdang.ddframe.job.lite.lifecycle.internal.statistics.ServerStatisticsAPIImpl;
import com.dangdang.ddframe.job.lite.lifecycle.internal.statistics.ShardingStatisticsAPIImpl;
import com.dangdang.ddframe.job.reg.zookeeper.ZookeeperRegistryCenter;

/**
 * 替换JobAPIServiceImpl + JobAPIFactory
 * @author xiongsl
 */
public class JobAPIServiceSpring implements JobAPIService{
	private final ZookeeperRegistryCenter registry = 
						SpringRegistrySettings.getRegistryCenter();
	
	private static JobAPIServiceSpring instance;
	public static synchronized JobAPIServiceSpring getInstance() {
		if (instance == null) {
			instance = new JobAPIServiceSpring();
		}
		return instance;
	}
	private JobAPIServiceSpring() {
	}
	
	@Override
    public JobSettingsAPI getJobSettingsAPI() {
        return new JobSettingsAPIImpl(registry);
    }
    
    @Override
    public JobOperateAPI getJobOperatorAPI() {
    	return new JobOperateAPIImpl(registry);
    }
    
    @Override
    public ShardingOperateAPI getShardingOperateAPI() {
    	return new ShardingOperateAPIImpl(registry);
    }
    
    @Override
    public JobStatisticsAPI getJobStatisticsAPI() {
    	return new JobStatisticsAPI(registry);
    }
    
    @Override
    public ServerStatisticsAPI getServerStatisticsAPI() {
    	return new ServerStatisticsAPIImpl(registry);
    }
    
    @Override
    public ShardingStatisticsAPI getShardingStatisticsAPI() {
    	return new ShardingStatisticsAPIImpl(registry);
    }
    
}
