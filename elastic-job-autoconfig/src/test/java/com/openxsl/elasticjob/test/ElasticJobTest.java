package com.openxsl.elasticjob.test;

import org.junit.Test;
import org.junit.runner.notification.RunNotifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.dangdang.ddframe.job.lite.internal.schedule.JobRegistry;
import com.dangdang.ddframe.job.lite.internal.schedule.JobScheduleController;
import com.openxsl.config.testuse.AutoConfig;
import com.openxsl.config.testuse.BasicTest;

@ContextConfiguration(locations="classpath*:spring/dal/http-client.xml")
@AutoConfig(application="elasticJob")
public class ElasticJobTest extends BasicTest {
	
	@Test
	public void testClassGenerator() throws Exception{
		while (true) {
			Thread.sleep(1000);
//			runImmediately();
		}
	}
	
	public static void main(String[] args) throws Exception {
//		Class<?> jobClass = SimpleJobProxy.makeSimpleJobClass(SimpleJobAction.class, "jobExec");
//		Assert.notNull(jobClass, "generate jobClass error");
		
		new SpringJUnit4ClassRunner(ElasticJobTest.class).run(new RunNotifier());
		
//		new Thread() {
//            public void run() {
//                JUnitCore.runClasses(ElasticJobTest.class);
//           }
//        }.start();
	}
	
	void runImmediately() {
		String jobName = "elasticJob/SimpleJobActionSimpleJob";
		JobScheduleController controller = JobRegistry.getInstance().getJobScheduleController(jobName);
		controller.triggerJob();
	}
	
}
