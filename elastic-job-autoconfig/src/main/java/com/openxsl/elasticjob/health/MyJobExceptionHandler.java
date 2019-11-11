package com.openxsl.elasticjob.health;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dangdang.ddframe.job.exception.ExceptionUtil;
import com.dangdang.ddframe.job.executor.handler.JobExceptionHandler;

/**
 * 作业调用失败的处理类，发送邮件通知
 * 
 * @author xiongsl
 */
public class MyJobExceptionHandler implements JobExceptionHandler {
	private final Logger logger = LoggerFactory.getLogger(getClass());

	@Override
	public void handleException(String jobName, Throwable cause) {
		logger.error("Job '{}' exception occur in job processing", jobName, cause);
		String errorTrace = ExceptionUtil.transform(cause);
		JobMailNotify.notice(jobName, "elasjob_execfail_mail", errorTrace);
	}

}
