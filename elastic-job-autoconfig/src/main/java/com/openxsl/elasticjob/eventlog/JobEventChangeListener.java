package com.openxsl.elasticjob.eventlog;

import org.apache.curator.framework.recipes.cache.TreeCacheEvent.Type;

import com.dangdang.ddframe.job.event.JobEventBus;
import com.dangdang.ddframe.job.executor.JobFacade;
import com.dangdang.ddframe.job.executor.handler.JobProperties;
import com.dangdang.ddframe.job.executor.handler.JobProperties.JobPropertiesEnum;
import com.dangdang.ddframe.job.lite.internal.config.LiteJobConfigurationGsonFactory;
import com.dangdang.ddframe.job.lite.internal.manager.JobEventNode;
import com.dangdang.ddframe.job.lite.internal.manager.JobNodeListener;
import com.dangdang.ddframe.job.lite.internal.storage.JobNodePath;
import com.openxsl.config.util.BeanUtils;

/**
 * 修改 allowSendEvent
 * 
 * @author xiongsl
 */
public class JobEventChangeListener extends JobNodeListener {
	private final String jobName;
	private final JobEventNode configNode;
	
	public JobEventChangeListener(String jobName) {
		this.jobName = jobName;
		configNode = new JobEventNode(jobName, JobNodePath.CONFIG_NODE);
	}
	
	public void start() {
		configNode.addDataListener(this);
	}

	@Override
	protected void dataChanged(String path, Type eventType, String data) {
		Boolean allowEvent = this.getAllowSendEvent(data);
		if (Type.NODE_UPDATED == eventType && allowEvent != null) {
			JobEventBus eventBus = JobEventRegistry.modifyJobEventBus(jobName, allowEvent);
			if (eventBus != null) {
				JobFacade jobFacade = JobSchedulerRegistry.getJobScheduler(jobName)
								.getJobFacade();
				//先回收老对象，再赋新对象
				BeanUtils.setPrivateField(jobFacade, "jobEventBus", null);
				BeanUtils.setPrivateField(jobFacade, "jobEventBus", eventBus);
			}
		}
	}
	
	private final Boolean getAllowSendEvent(String data) {
		JobProperties jobProps = LiteJobConfigurationGsonFactory.fromJson(data)
    					.getTypeConfig().getCoreConfig().getJobProperties();
		String mapStr = jobProps.get(JobPropertiesEnum.JOB_RUNTIME_PROPERTIES);
		if (mapStr != null && !"".equals(mapStr)) {
			mapStr = mapStr.substring(1, mapStr.length()-1);
			for (String pair : mapStr.split(",")) {
				String[] kvs = pair.split("=");
				if ("allowSendEvent".equals(kvs[0].trim())) {
					return Boolean.valueOf(kvs[1].trim());
				}
			}
		}
		return null;
    }
	
}
