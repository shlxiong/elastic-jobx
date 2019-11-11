package com.openxsl.elasticjob.anno;

import java.util.List;

import com.dangdang.ddframe.job.api.ElasticJob;
import com.dangdang.ddframe.job.lite.api.listener.ElasticJobListener;
import com.dangdang.ddframe.job.lite.config.LiteJobConfiguration;

/**
 * 封装LiteJobConfiguration + ElasticJobListener
 * @author xiongsl
 * @create 2019-07-04
 */
public class LiteJobConfigWrapper {
	private final LiteJobConfiguration litejobConfiguration;
	private ElasticJobListener[] jobListeners;
	private Class<? extends ElasticJob> jobClass;
	
	public LiteJobConfigWrapper(LiteJobConfiguration litejobConfiguration) {
		this.litejobConfiguration = litejobConfiguration;
	}

	public ElasticJobListener[] getJobListeners() {
		return jobListeners;
	}

	public void setJobListeners(List<ElasticJobListener> jobListeners) {
		int len = jobListeners==null ? 0 : jobListeners.size();
		this.jobListeners = new ElasticJobListener[len];
		if (len > 0) {
			for (int i=0; i<len; i++) {
				this.jobListeners[i] = jobListeners.get(0);
			}
		}
	}
	public LiteJobConfigWrapper setJobListeners(ElasticJobListener[] jobListeners) {
		this.jobListeners = jobListeners;
		return this;
	}

	public LiteJobConfiguration getLitejobConfiguration() {
		return litejobConfiguration;
	}

	public LiteJobConfigWrapper setJobClass(Class<? extends ElasticJob> jobClass) {
		this.jobClass = jobClass;
		return this;
	}
	
	public Class<? extends ElasticJob> getJobClass() {
		return jobClass;
	}

}
