package com.openxsl.elasticjob.health;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.openxsl.config.Environment;
import com.openxsl.config.dal.HttpClientInvoker;

public class JobMailNotify {
	private static final Logger LOG = LoggerFactory.getLogger(JobMailNotify.class);
	private static final String NOTICE_URL = Environment.getProperty("job.healthCheck.notice.url");
	private static final String RECEIVER = Environment.getProperty("job.healthCheck.notice.receiver");
	private static final String PASSWORD = Environment.getProperty("job.healthCheck.notice.finger");
	private static final String REGISTRY = Environment.getProperty("elastic.job.registry.servers");
	
	static void notice(String jobName, String serviceId, String message) {
		LOG.info("****Notice: Job({}) occurs a mail.....", jobName);
		//notify-sms mail service
		if (NOTICE_URL != null && RECEIVER != null) {
			Map<String, Object> argsMap = new HashMap<String, Object>();
			argsMap.put("serviceId", serviceId);
			argsMap.put("dataId", System.currentTimeMillis());
			argsMap.put("bizSys", "elasticweb");
			argsMap.put("finger", PASSWORD);
			//2019-06-16：添加注册中心地址。
			String paramStr = String.format("{\"RECEIVER\":\"%s\",\"jobName\":\"%s: %s\"",
									RECEIVER,REGISTRY,jobName);
			if (message != null) {
				paramStr += String.format(",\"message\":\"%s\"", message);
			}
			paramStr += '}';
			argsMap.put("paramStr", paramStr);
			try {
				HttpClientInvoker.getInstance().request(NOTICE_URL, argsMap, 1);
			} catch(java.net.SocketTimeoutException toe) {
				LOG.warn("request timeout: {}", NOTICE_URL);
			}catch (IOException e) {
				LOG.warn("send notice-mail error: ", e);
			}
		}
	}
	
}
