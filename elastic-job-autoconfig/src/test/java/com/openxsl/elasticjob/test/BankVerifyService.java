package com.openxsl.elasticjob.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//@SimpleJobs({
//	@SimpleJob(name="PingAnVerifyJob", cron="0 5/20 * * * ?", method="jobExec", jobParameter="PAB"),
//	@SimpleJob(name="OneOrderVerifyJob", cron="0 5/20 * * * ?", method="jobExec", jobParameter="SINGLE"),
//	@SimpleJob(name="RefundVerifyJob", cron="0 5/20 * * * ?", method="jobExec", jobParameter="REFUND"),
//	@SimpleJob(name="BalanceVerifyJob", cron="0 5/20 * * * ?", method="jobExec", jobParameter="CHECK")
//})
//@Component("bankVerifyService")
public class BankVerifyService {
	private Logger logger = LoggerFactory.getLogger(getClass());
	
	public String jobExec(final String arg0) {
		if (arg0.equals("PAB")) {
			logger.info("平安银行查证");
		} else if (arg0.equals("SINGLE")) {
			logger.info("单笔交易查证");
		} else if (arg0.equals("REFUND")) {
			logger.info("退款交易查证");
		} else if (arg0.equals("CHECK")) {
			logger.info("对账文件查证");
		} else{
			logger.info("任务参数类型有误: {}", arg0);
		}
		return arg0;
	}

}
