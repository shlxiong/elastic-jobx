package com.openxsl.elasticjob.test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.openxsl.config.dal.RestInvoker;
import com.openxsl.elasticjob.SimpleJob;

@SimpleJob(cron="0 */1 * * * ?", method="jobExec", jobParameter="3",
		description="SimpleJob测试样例"
		,sharddingParams= {"shanghai", "beijing", "newyork", "washington"}
	)
@Component("cardTransTask")
public class SimpleJobAction {
	@Autowired
	private RestInvoker restInvoker;
	
	public String jobExec(String jobParam) {//throws TaskException {
		try {
			restInvoker.postJson("https://www.baidu.com", "");
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return "result-"+jobParam;
	}

}
