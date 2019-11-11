package com.openxsl.elasticjob.eventlog;

import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.PasswordManager;
import com.dangdang.ddframe.job.event.JobEventBus;
import com.dangdang.ddframe.job.event.JobEventConfiguration;
import com.openxsl.config.Environment;
import com.openxsl.config.util.StringUtils;
import com.openxsl.elasticjob.EventDataSourceConf;
import com.openxsl.elasticjob.anno.JobAnnoParser;

/**
 * JobEventConfiguration管理类
 * 
 * @author xiongsl
 */
public class JobEventRegistry {
	private static final Logger LOG = LoggerFactory.getLogger(JobEventRegistry.class);
	private static ConfigurableApplicationContext springContext;
	private static EventDataSourceConf dataSourceConf;
	private static DataSource dataSource;
	private static ConcurrentHashMap<String, Boolean> allowEventMap
				= new ConcurrentHashMap<String, Boolean>();
	
	public static void initate(EventDataSourceConf dataSourceConf,
					ConfigurableApplicationContext context) {
		JobEventRegistry.dataSourceConf = dataSourceConf;
		JobAnnoParser.allowEvent = dataSourceConf.isSendEvent();
		springContext = context;
		//elasticweb - 控制台需要查询日志
		if (Environment.exists("com.dangdang.ddframe.job.lite.console.ConsoleBootstrap")) {
			try {
				getDataSource();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static JobEventConfiguration getJobEventConfig(String jobName) {
		Boolean allow = allowEventMap.get(jobName);
		if (allow == null) {
			allow = dataSourceConf.isSendEvent();
		}
		DataSource dataSource = null;
		try {
			if (allow) {
				dataSource = getDataSource();
			}
		} catch (SQLException e) {
			LOG.warn("initiate job DataSource error: ", e);
		}
//		eventConfig = new JobEventRdbConfiguration(dataSource);
		return new MyJobEventConfig(dataSource);
	}
	/**
	 * 修改事件监听
	 * @param jobName
	 * @param flag
	 * @see #JobEventChangeListener
	 */
	public static JobEventBus modifyJobEventBus(String jobName, boolean flag) {
		if (allowEventMap.containsKey(jobName) && allowEventMap.get(jobName).equals(flag)) {
			return null;
		} else {
			allowEventMap.put(jobName, flag);
			return getJobEventConfig(jobName).createJobEventBus();
		}
	}
	
	/**
	 * 记录事件的数据源，只有一个
	 * @throws SQLException
	 */
	private static DataSource getDataSource() throws SQLException{
		if (dataSource != null) {
			return dataSource;
		}
		if (dataSourceConf.getBeanId().length() > 0 && springContext!=null) {
			return springContext.getBean(dataSourceConf.getBeanId(), DataSource.class);
		}
		if ("".equals(dataSourceConf.getConnectionProperties()) && "".equals(dataSourceConf.getJdbcUrl())){
			return null;
		}
		DruidDataSource druidSource = new DruidDataSource();
		if (StringUtils.isEmpty(dataSourceConf.getConnectionProperties())) {
			druidSource.setUrl(dataSourceConf.getJdbcUrl());
			druidSource.setUsername(dataSourceConf.getUsername());
			druidSource.setPassword(dataSourceConf.getPassword());
		} else {
			druidSource.setFilters("config,slf4j");
			druidSource.setConnectionProperties(dataSourceConf.getConnectionProperties());
		}
		
		if (springContext.containsBean("druidPasswordMgr")) {
			PasswordManager passMgr = springContext.getBean(PasswordManager.class);
			druidSource.setPasswordMgr(passMgr);
		}
		druidSource.init();
		
		dataSource = druidSource;
		//SpringBoot-autoconfig 对于多个DataSource会报错
		if (dataSourceConf.isAutowired() && springContext!=null) {
			springContext.getBeanFactory().registerSingleton("jobDataSource", dataSource);
		}
		return dataSource;
	}

}
