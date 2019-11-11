package com.openxsl.elasticjob.eventlog;

import java.io.Serializable;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dangdang.ddframe.job.event.JobEventBus;
import com.dangdang.ddframe.job.event.JobEventConfiguration;
import com.dangdang.ddframe.job.event.JobEventListener;
import com.dangdang.ddframe.job.event.JobEventListenerConfigurationException;

@SuppressWarnings("serial")
public class MyJobEventConfig implements JobEventConfiguration, Serializable {
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final DataSource dataSource;
	
	public MyJobEventConfig(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	@Override
	public String getIdentity() {
		return "openxsl";
	}

	@Override
	public JobEventListener createJobEventListener() throws JobEventListenerConfigurationException {
		try{
			if (dataSource == null) {
				return null;
			}
			return new MyJobEventListener(dataSource);
		}catch(Exception e) {
			throw new JobEventListenerConfigurationException(e);
		}
	}
	
	@Override
	public JobEventBus createJobEventBus() {
		try {
			return new MyJobEventBus(this.createJobEventListener());
		} catch (JobEventListenerConfigurationException ex) {
			logger.error("Elastic job: JobEventBus has no subscribers, for createJobEventListener() failure: ", ex);
			return new MyJobEventBus();
		}
	}

}
