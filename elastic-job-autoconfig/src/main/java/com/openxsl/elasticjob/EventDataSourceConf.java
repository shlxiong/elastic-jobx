package com.openxsl.elasticjob;

import org.springframework.beans.factory.annotation.Value;

import com.openxsl.config.autodetect.ScanConfig;

/**
 * 日志数据源
 * @author xiongsl
 */
@ScanConfig
public class EventDataSourceConf {
	@Value("${job.event.sendEvent:true}")
	private boolean sendEvent;
	@Value("${job.datasource.autowired:true}")
	private boolean autowired;     //是否注入到spring上下文
	@Value("${job.event.dataSource:}")
	private String beanId;
	
	@Value("${job.event.connectionProperties:}")
	private String connectionProperties;
	
	@Value("${job.event.driver:com.mysql.jdbc.Driver}")
	private String jdbcDriver;
	@Value("${job.event.url:}")
	private String jdbcUrl;
	@Value("${job.event.username:}")
	private String username;
	@Value("${job.event.password:}")
	private String password;
	
	public String getJdbcDriver() {
		return jdbcDriver;
	}
	public void setJdbcDriver(String jdbcDriver) {
		this.jdbcDriver = jdbcDriver;
	}
	public String getJdbcUrl() {
		return jdbcUrl;
	}
	public void setJdbcUrl(String jdbcUrl) {
		this.jdbcUrl = jdbcUrl;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getConnectionProperties() {
		return connectionProperties;
	}
	public void setConnectionProperties(String connectionProperties) {
		this.connectionProperties = connectionProperties;
	}
	public String getBeanId() {
		return beanId;
	}
	public void setBeanId(String beanId) {
		this.beanId = beanId;
	}
	public boolean isSendEvent() {
		return sendEvent;
	}
	public void setSendEvent(boolean sendEvent) {
		this.sendEvent = sendEvent;
	}
	public boolean isAutowired() {
		return autowired;
	}
	public void setAutowired(boolean autowired) {
		this.autowired = autowired;
	}

}
