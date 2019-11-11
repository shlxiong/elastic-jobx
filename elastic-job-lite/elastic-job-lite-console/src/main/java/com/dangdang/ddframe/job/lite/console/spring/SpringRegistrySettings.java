package com.dangdang.ddframe.job.lite.console.spring;

import javax.sql.DataSource;

import com.dangdang.ddframe.job.reg.zookeeper.ZookeeperRegistryCenter;

import cn.sumpay.config.Environment;

/**
 * 替代 utils.Session*Configration.java
 * @author xiongsl
 */
public class SpringRegistrySettings {
	private static ZookeeperRegistryCenter registry;
	private static DataSource dataSource;
	
	public static ZookeeperRegistryCenter getRegistryCenter() {
		if (registry == null) {
			registry = Environment.getSpringContext().getBean("jobRegistry",
								ZookeeperRegistryCenter.class);
			registry.getZkConfig().getNamespace();
		}
		return registry;
	}
	
	public static DataSource getEventDataSource() {
		if (dataSource == null) {
			dataSource = Environment.getSpringContext().getBean("jobDataSource",
								DataSource.class);
		}
		return dataSource;
	}
	
}
