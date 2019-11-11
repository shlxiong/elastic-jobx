package com.openxsl.elasticjob.eventlog;

import java.sql.SQLException;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.druid.filter.ConnectionFilter;
import com.dangdang.ddframe.job.event.JobEventListener;
import com.dangdang.ddframe.job.event.type.JobExecutionEvent;
import com.dangdang.ddframe.job.event.type.JobStatusTraceEvent;

public class MyJobEventListener implements JobEventListener{
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final EventLogRepository repository;
	
	public MyJobEventListener(DataSource dataSource) throws Exception{
		disableTracing(true);
		this.repository = new EventLogRepository(dataSource);  //createTableIfNecessary
		disableTracing(false);
	}

	@Override
	public String getIdentity() {
		return "openxsl";
	}

	@Override
	public void listen(JobExecutionEvent jobExecutionEvent) {
		disableTracing(true);
		try {
			repository.saveJobExecution(jobExecutionEvent);
		} catch (SQLException e) {
			logger.error("", e);
		}
		disableTracing(false);
	}

	@Override
	public void listen(JobStatusTraceEvent jobStatusTraceEvent) {
		disableTracing(true);
		try {
			repository.addJobStatusLog(jobStatusTraceEvent);
		} catch (SQLException e) {
			logger.error("", e);
		}
		disableTracing(false);
	}
	
	private void disableTracing(boolean flag) {
		try {
			ConnectionFilter.disableTracing(flag);
		} catch (Throwable e) {
			//maybe ClassNotFound
		}
	}

}
