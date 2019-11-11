package com.openxsl.elasticjob.test;

import java.sql.SQLException;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import com.alibaba.druid.pool.DruidDataSource;
import com.dangdang.ddframe.job.util.IDGenerator;
import com.openxsl.config.testuse.AutoConfig;
import com.openxsl.config.testuse.BasicTest;
import com.openxsl.config.util.StringUtils;
import com.openxsl.elasticjob.EventDataSourceConf;
import com.openxsl.elasticjob.eventlog.JobExecutionBean;
import com.openxsl.elasticjob.jpa.JdbcUtils;

@ContextConfiguration
@AutoConfig(application="elasticJob")
public class EventLogRepoTest extends BasicTest{
	@Autowired
	private EventDataSourceConf dataSourceConf;
	
	@Test
	public void testExecution() throws SQLException {
		JobExecutionBean execution = new JobExecutionBean();
		execution.setId(IDGenerator.getTraceID());
		execution.setTaskId("abcd");
		execution.setShardId(0);
		execution.setStartTime(new java.util.Date());
		execution.setExecSource("NORMAL_TRIGGER");
		JdbcUtils.insert(execution, this.initDataSource());
	}
	
	private DruidDataSource initDataSource() throws SQLException {
		DruidDataSource dataSource = new DruidDataSource();
		if (StringUtils.isEmpty(dataSourceConf.getConnectionProperties())) {
			dataSource.setUrl(dataSourceConf.getJdbcUrl());
			dataSource.setUsername(dataSourceConf.getUsername());
			dataSource.setPassword(dataSourceConf.getPassword());
		} else {
			dataSource.setFilters("config,slf4j");
			dataSource.setConnectionProperties(dataSourceConf.getConnectionProperties());
		}
		
		dataSource.init();
		return dataSource;
	}
}
