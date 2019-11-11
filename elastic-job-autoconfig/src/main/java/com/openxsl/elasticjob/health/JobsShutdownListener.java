package com.openxsl.elasticjob.health;

import org.apache.curator.framework.recipes.cache.TreeCacheEvent.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dangdang.ddframe.job.lite.internal.manager.JobNodeListener;
import com.dangdang.ddframe.job.reg.zookeeper.ZookeeperRegistryCenter;

/**
 * 任务被关闭时，发邮件通知
 * @author xiongsl
 */
public class JobsShutdownListener extends JobNodeListener {
	private static final Logger LOG = LoggerFactory.getLogger(JobsShutdownListener.class);
	
	private ZookeeperRegistryCenter registry;
	
	public JobsShutdownListener(ZookeeperRegistryCenter regCenter) {
		registry = regCenter;
	}

	@Override
	protected void dataChanged(String path, Type eventType, String data) {
		if (Type.NODE_REMOVED == eventType) {
			String instancePath = path.substring(0, path.lastIndexOf("/"));  //"/jobName/instances"
			if (registry.getNumChildren(instancePath) < 1) {
				String jobName = instancePath.substring(0, instancePath.lastIndexOf("/"));
				notice(jobName);
			}
		}
	}
	
	static void notice(String jobName) {
		LOG.info("****Notice: {} has been shutdown!!!", jobName);
		JobMailNotify.notice(jobName, "elasjob_shutdown_mail", null);
		//notify-sms mail service
//		if (NOTICE_URL != null && RECEIVER != null) {
//			Map<String, Object> argsMap = new HashMap<String, Object>();
//			argsMap.put("serviceId", "elasjob_shutdown_mail");
//			argsMap.put("dataId", System.currentTimeMillis());
//			argsMap.put("bizSys", "elasticweb");
//			argsMap.put("finger", PASSWORD);
//			//2019-06-16：添加注册中心地址。
//			String paramStr = String.format("{\"RECEIVER\":\"%s\",\"jobName\":\"%s: %s\"}",
//									RECEIVER,REGISTRY_ADDRESS,jobName);
//			argsMap.put("paramStr", paramStr);
//			try {
//				HttpClientInvoker.getInstance().request(NOTICE_URL, argsMap, 1);
//			} catch(java.net.SocketTimeoutException toe) {
//				LOG.warn("request timeout: {}", NOTICE_URL);
//			}catch (IOException e) {
//				LOG.warn("send notice-mail error: ", e);
//			}
//		}
	}

}
