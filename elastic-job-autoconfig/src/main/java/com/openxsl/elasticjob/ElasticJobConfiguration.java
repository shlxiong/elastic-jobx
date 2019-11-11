package com.openxsl.elasticjob;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import org.quartz.JobDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

import com.dangdang.ddframe.job.api.ElasticJob;
import com.dangdang.ddframe.job.event.JobEventConfiguration;
import com.dangdang.ddframe.job.executor.ShardingContexts;
import com.dangdang.ddframe.job.lite.api.JobScheduler;
import com.dangdang.ddframe.job.lite.api.listener.ElasticJobListener;
import com.dangdang.ddframe.job.lite.config.LiteJobConfiguration;
import com.dangdang.ddframe.job.lite.internal.schedule.JobRegistry;
import com.dangdang.ddframe.job.lite.internal.schedule.JobScheduleController;
import com.dangdang.ddframe.job.reg.zookeeper.ZookeeperConfiguration;
import com.dangdang.ddframe.job.reg.zookeeper.ZookeeperRegistryCenter;
import com.dangdang.ddframe.job.util.env.IpUtils;
import com.openxsl.config.autodetect.ScanConfig;
import com.openxsl.config.proxy.ClassGenerator;
import com.openxsl.config.util.BeanUtils;
import com.openxsl.elasticjob.anno.DataflowJob;
import com.openxsl.elasticjob.anno.ElasticJobConf;
import com.openxsl.elasticjob.anno.JobAnnoParser;
import com.openxsl.elasticjob.anno.LiteJobConfigWrapper;
import com.openxsl.elasticjob.anno.ScriptJob;
import com.openxsl.elasticjob.eventlog.JobEventChangeListener;
import com.openxsl.elasticjob.eventlog.JobEventRegistry;
import com.openxsl.elasticjob.eventlog.JobSchedulerRegistry;
import com.openxsl.elasticjob.health.JobRestartListener;

/**
 * 初始化调度作业，用于处理带@SimpleJob的服务
 * 
 * @author xiongsl
 */
@ScanConfig
@Configuration    //spring-boot EnableAutoConfiguration
public class ElasticJobConfiguration implements ApplicationContextAware, InitializingBean{
	private final Logger logger = LoggerFactory.getLogger(getClass());
	@Autowired
	private EventDataSourceConf dataSourceConf;
	@Autowired
	private RegistryConfiguration registryConfig;
	
	private ConfigurableApplicationContext springContext;
	private ZookeeperRegistryCenter registry;
	@Value("${job.registry.namespace:}")
	private String namespace;
	/**
	 * 指定作业实例的本机IP（由于IpUtils不准确）
	 */
	@Value("${job.instance.ip:}")
	private String localIp;
	/**
	 * 启动开关
	 */
	@Value("${job.reboot.switch:true}")
	private boolean bootSwitch;
	/**
	 * 注入Quartz属性
	 */
	private Properties quartzProperites;
	public void setQuartzProperties(Properties qtzProps) {
    	quartzProperites = qtzProps;
    }
	
	@Override
	public void setApplicationContext(ApplicationContext arg0) throws BeansException {
		this.springContext = (ConfigurableApplicationContext)arg0;
	}
	
//	@Bean(name="jobRegistry", initMethod="init")
	public ZookeeperRegistryCenter registry() {
		if (!"".equals(localIp)) {
			IpUtils.setHostIp(localIp);
		}
		ZookeeperConfiguration configuration = new ZookeeperConfiguration(
					registryConfig.getServerAddrs(), "elastic-job");
		configuration.setConnectionTimeoutMilliseconds(registryConfig.getConnectTimeout());
		configuration.setSessionTimeoutMilliseconds(registryConfig.getSessionTimeout());
		configuration.setMaxRetries(registryConfig.getRetries());
		configuration.setBaseSleepTimeMilliseconds(registryConfig.getBaseSleepTime());
		configuration.setMaxSleepTimeMilliseconds(registryConfig.getMaxSleepTime());
		configuration.setDigest(registryConfig.getDigest());
		registry = new ZookeeperRegistryCenter(configuration);
		registry.init();
		springContext.getBeanFactory().registerSingleton("jobRegistry", registry);
		return registry;
	}
	
	@Override
	public void afterPropertiesSet() throws Exception {
		this.registry();
		JobEventRegistry.initate(dataSourceConf, springContext);
//		this.eventConfig();
		
		Map<SimpleJob, Object> jobActions = new LinkedHashMap<SimpleJob, Object>();
		Map<String, Object> beans = this.springContext.getBeansWithAnnotation(SimpleJobs.class);
		if (beans != null) {
			SimpleJob[] jobs;
        	for (Map.Entry<String,Object> entry : beans.entrySet()) {
        		Class<?> serviceClass = entry.getValue().getClass();
        		jobs = serviceClass.getAnnotation(SimpleJobs.class).value();
        		for (SimpleJob job : jobs) {
        			if ("".equals(job.name())) {
        				throw new IllegalStateException("作业名(@SimpleJobs)不能为空！JobAction="
        						+ serviceClass.getName());
        			} else {
        				jobActions.put(job, entry.getValue());
        			}
        		}
        	}
		}
		beans = this.springContext.getBeansWithAnnotation(SimpleJob.class);
		if (beans != null) {
        	for (Map.Entry<String,Object> entry : beans.entrySet()) {
        		SimpleJob jobAnno = entry.getValue().getClass().getAnnotation(SimpleJob.class);
        		jobActions.put(jobAnno, entry.getValue());
        	}
        }
		
		for (Map.Entry<SimpleJob,Object> entry : jobActions.entrySet()) {
			this.registerSimpleJob(entry.getKey(), entry.getValue());
		}
		
		this.registerDataflowJobs();
		
		this.registerScriptJobs();
	}
	/**
	 * 注册一个SimpleJob
	 * @param beanId ID
	 * @param bean   JobAction
	 * @throws Exception
	 */
	private void registerSimpleJob(SimpleJob jobAnno, Object jobAction) throws Exception{
		LiteJobConfigWrapper liteConfig = 
				JobAnnoParser.build(jobAnno, jobAction, namespace, bootSwitch);
		ElasticJob elasticJob = liteConfig.getJobClass().newInstance();
		this.startJobScheduler(liteConfig, elasticJob);
//		Class<?> serviceClass = jobAction.getClass();
//		String method = jobAnno.method();
//		Class<?> jobClass = SimpleJobProxy.makeSimpleJobClass(serviceClass, method);
//		LiteJobConfiguration liteConfig = 
//					JobAnnoParser.build(jobAnno, jobClass.getCanonicalName(), namespace, bootSwitch);
//		int len = jobAnno.listeners().length;
//		ElasticJobListener[] listeners = new ElasticJobListener[len+1];
//		String jobName = liteConfig.getJobName();
//		listeners[0] = new JobActionInjectListener(jobName, jobAction);
//		for (int i=0; i<len; i++) {
//			listeners[i+1] = jobAnno.listeners()[i].newInstance();
//		}
//		
//		logger.info("register job(name={})....", jobName);
//		JobEventConfiguration eventConfig = JobEventRegistry.getJobEventConfig(jobName);
//		JobScheduler scheduler = new JobScheduler(registry, liteConfig, eventConfig, listeners);
//		scheduler.setQuartzProperties(quartzProperites);
//		JobSchedulerRegistry.registerScheduler(jobName, scheduler);   //ensure unique
//		try {
//			scheduler.setJobClass(jobClass);
//			scheduler.init();
//			springContext.getBeanFactory().registerSingleton(jobName+"Scheduler", scheduler);
//		} catch (Exception e) {
//			logger.error("start job(name={}) error: ", jobName,e);
//		}
//		//listener for allowSendEvent
//		new JobEventChangeListener(jobName).start();
	}
	
	private void registerDataflowJobs() throws Exception {
		Map<String, Object> beans = this.springContext.getBeansWithAnnotation(DataflowJob.class);
		if (beans != null) {
        	for (Map.Entry<String,Object> entry : beans.entrySet()) {
        		Class<?> jobClass = entry.getValue().getClass();
        		DataflowJob jobAnno = jobClass.getAnnotation(DataflowJob.class);
//        		if (!com.dangdang.ddframe.job.api.dataflow.DataflowJob.class.isAssignableFrom(jobClass)) {
//TODO        		jobClass = makeDataflowJobClass();
//        		}
        		LiteJobConfigWrapper liteConfig =
        					JobAnnoParser.build(jobAnno, jobClass.getName(), namespace, bootSwitch);
        		this.startJobScheduler(liteConfig, (ElasticJob)entry.getValue());
        	}
		}
		
		beans = this.springContext.getBeansWithAnnotation(ElasticJobConf.class);
		if (beans != null) {
        	for (Map.Entry<String,Object> entry : beans.entrySet()) {
        		Class<?> jobClass = entry.getValue().getClass();
        		ElasticJobConf jobAnno = jobClass.getAnnotation(ElasticJobConf.class);
        		LiteJobConfigWrapper liteConfig =
        					JobAnnoParser.build(jobAnno, jobClass.getName(), namespace, bootSwitch);
        		this.startJobScheduler(liteConfig, (ElasticJob)entry.getValue());
        	}
		}
	}
	
	private void registerScriptJobs() throws Exception {
		Map<String, Object> beans = this.springContext.getBeansWithAnnotation(ScriptJob.class);
		if (beans != null) {
        	for (Map.Entry<String,Object> entry : beans.entrySet()) {
        		Class<?> jobClass = entry.getValue().getClass();
        		DataflowJob jobAnno = jobClass.getAnnotation(DataflowJob.class);
//        		if (!com.dangdang.ddframe.job.api.dataflow.ScriptJob.class.isAssignableFrom(jobClass)) {
//TODO        		jobClass = makeDataflowJobClass();
//        		}
        		LiteJobConfigWrapper liteConfig =
        					JobAnnoParser.build(jobAnno, jobClass.getName(), namespace, bootSwitch);
        		this.startJobScheduler(liteConfig, (ElasticJob)entry.getValue());
        	}
		}
	}
	
	private void startJobScheduler(LiteJobConfigWrapper liteConfig, ElasticJob job) {
		String jobName = liteConfig.getLitejobConfiguration().getJobName();
		logger.info("register job(name={})....", jobName);
		JobEventConfiguration eventConfig = JobEventRegistry.getJobEventConfig(jobName);
		LiteJobConfiguration liteJobConfig = liteConfig.getLitejobConfiguration();
		JobScheduler scheduler = new SpringJobScheduler(job, registry, liteJobConfig, 
						eventConfig, liteConfig.getJobListeners());
		scheduler.setQuartzProperties(quartzProperites);
		JobSchedulerRegistry.registerScheduler(jobName, scheduler);   //ensure unique
		try {
			scheduler.init();
			springContext.getBeanFactory().registerSingleton(jobName+"Scheduler", scheduler);
			//listener for allowSendEvent
			new JobEventChangeListener(jobName).start();
			new JobRestartListener(jobName, scheduler).start();
		} catch (Exception e) {
			logger.error("start job(name={}) error: ", jobName,e);
		}
	}
	
	/**
	 * 将JobAction绑到SimpleJob中
	 * @author xiongsl
	 */
	class JobActionInjectListener implements ElasticJobListener {
		private String jobName;
		private Object jobAction;
		private boolean setted = false;
		
		public JobActionInjectListener(String jobName, Object jobAction) {
			this.jobName = jobName;
			this.jobAction = jobAction;
		}

		@Override
		public void beforeJobExecuted(ShardingContexts shardingContexts) {
			if (!setted) {
				this.registerJobAction(jobName, jobAction);
				setted = true;
			}
		}

		@Override
		public void afterJobExecuted(ShardingContexts shardingContexts) {
		}
		
		private void registerJobAction(String jobName, Object jobAction) {
			JobScheduleController controller = JobRegistry.getInstance().getJobScheduleController(jobName);
			JobDetail jobDetail = (JobDetail)BeanUtils.getPrivateField(controller, "jobDetail");
			ElasticJob job = (ElasticJob)jobDetail.getJobDataMap().get(JobScheduler.ELASTIC_JOB_DATA_MAP_KEY);
			try {
				if (job!=null && job instanceof ClassGenerator.DC) {
					BeanUtils.setPrivateField(job, "jobAction", jobAction);
				}
			}finally {
				jobDetail = null;
				controller = null;
			}
		}
	}

}
