package com.openxsl.elasticjob.anno;

import java.util.HashMap;

import com.dangdang.ddframe.job.api.ElasticJob;
import com.dangdang.ddframe.job.config.JobCoreConfiguration;
import com.dangdang.ddframe.job.config.dataflow.DataflowJobConfiguration;
import com.dangdang.ddframe.job.config.script.ScriptJobConfiguration;
import com.dangdang.ddframe.job.config.simple.SimpleJobConfiguration;
import com.dangdang.ddframe.job.executor.handler.JobProperties.JobPropertiesEnum;
import com.dangdang.ddframe.job.lite.api.listener.ElasticJobListener;
import com.dangdang.ddframe.job.lite.config.LiteJobConfiguration;
import com.dangdang.ddframe.job.util.QuartzUtils;
import com.openxsl.config.Environment;
import com.openxsl.elasticjob.CoverageAllHostJobShardingStrategy;
import com.openxsl.elasticjob.SimpleJob;
import com.openxsl.elasticjob.SimpleJobProxy;
import com.openxsl.elasticjob.health.MyJobExceptionHandler;
import com.openxsl.elasticjob.thread.MyJobExecutorServiceHandler;

/**
 * 统一解析Job注解的地方
 * 
 * @author xiongsl
 * @create 2019-07-04
 */
public class JobAnnoParser {
	public static boolean allowEvent = true;    //全局开关
	private static final String SHARDING_STRATEGY = CoverageAllHostJobShardingStrategy.class.getName();
	private static final String EXECUTOR_SERVICE_HANDLER = MyJobExecutorServiceHandler.class.getName();
	private static final String JOB_EXCEPTION_HANDLER = MyJobExceptionHandler.class.getName();
	
	public static LiteJobConfigWrapper build(SimpleJob jobAnno, Object jobAction,
						String namespace, boolean bootSwitch) throws Exception {
		Class<?> serviceClass = jobAction.getClass();
		String method = jobAnno.method();
		@SuppressWarnings("unchecked")
		Class<ElasticJob> proxyClass = (Class<ElasticJob>)SimpleJobProxy.makeSimpleJobClass(serviceClass, method);
		String jobClass = proxyClass.getCanonicalName();
		
		String jobName = jobAnno.name();
		if ("".equals(jobName)) {
			jobName = jobClass.substring(jobClass.lastIndexOf(".")+1,
										jobClass.lastIndexOf("_G"));
		}
		if (jobName.indexOf("_") != -1) {
			throw new IllegalArgumentException("jobName can NOT contain char '_'");
		}
		if ("".equals(namespace)) {
			namespace = Environment.getApplication();
		}
		
		jobName = namespace + "/" + jobName;
		String cron = jobAnno.cron();
		String jobParam = jobAnno.jobParameter();
		JobCoreConfiguration coreConfig = newJobCoreConfiguration(jobName, cron,
					jobAnno.description(), jobParam, jobAnno.sharddingParams(),
					jobAnno.failover(), jobAnno.misfire(), jobAnno.eventTrace()
				);
		boolean disabled = bootSwitch && jobAnno.disabled();    //bootSwitch=false: 统一关闭
		ElasticJobListener[] listeners = instantiateListeners(jobAnno.listeners(), true);
		listeners[0] = new SimpleJobInjectionListener(jobName, jobAction);
		LiteJobConfiguration liteJobConfig = LiteJobConfiguration.newBuilder(
					new SimpleJobConfiguration(coreConfig, jobClass))
				.overwrite(jobAnno.overwrite())  //本地配置是否覆盖注册中心的[false]
				.reconcileIntervalMinutes(10)    //自诊断
				.jobShardingStrategyClass(SHARDING_STRATEGY)  //分片策略 
				.disabled(disabled)              //启动或失效
				.build();
		return new LiteJobConfigWrapper(liteJobConfig).setJobClass(proxyClass)
					.setJobListeners(listeners);
	}
	
	public static LiteJobConfigWrapper build(DataflowJob jobAnno, String jobClass,
						String namespace, boolean bootSwitch) {
		String jobName = jobAnno.name();
		if (jobName.endsWith("Job") || jobName.endsWith("job")) {
			jobName = jobName.substring(0, jobName.length()-3);
		}
		jobName = namespace + "/" + jobName+"FlowJob";
		String cron = jobAnno.cron();
		String jobParam = jobAnno.jobParameter();
		JobCoreConfiguration coreConfig = newJobCoreConfiguration(jobName, cron,
					jobAnno.description(), jobParam, jobAnno.sharddingParams(),
					jobAnno.failover(), jobAnno.misfire(), jobAnno.eventTrace()
				);
		boolean streaming = jobAnno.streamingProcess();
		boolean disabled = bootSwitch && jobAnno.disabled();    //bootSwitch=false: 统一关闭
		LiteJobConfiguration liteJobConfig = LiteJobConfiguration.newBuilder(
						new DataflowJobConfiguration(coreConfig, jobClass, streaming))
					.overwrite(jobAnno.overwrite())  //本地配置是否覆盖注册中心的[false]
					.reconcileIntervalMinutes(10)    //自诊断
					.jobShardingStrategyClass(SHARDING_STRATEGY)  //分片策略 
					.disabled(disabled)    	//启动或失效
					.build();
		ElasticJobListener[] listeners = instantiateListeners(jobAnno.listeners(), false);
		return new LiteJobConfigWrapper(liteJobConfig).setJobListeners(listeners);
	}
	
	/**
	 * 也是DataFlowJob
	 */
	public static LiteJobConfigWrapper build(ElasticJobConf jobAnno, String jobClass,
						String namespace, boolean bootSwitch) {
		String jobName = jobAnno.name();
		if (jobName.endsWith("Job") || jobName.endsWith("job")) {
			jobName = jobName.substring(0, jobName.length()-3);
		}
		jobName = namespace + "/" + jobName+"FlowJob";
		String cron = jobAnno.cron();
		String jobParam = jobAnno.jobParameter();
		JobCoreConfiguration coreConfig = newJobCoreConfiguration(jobName, cron,
					jobAnno.description(), jobParam, jobAnno.sharddingParams(),
					jobAnno.failover(), jobAnno.misfire(), jobAnno.eventTrace()
				);
		boolean streaming = jobAnno.streamingProcess();
		boolean disabled = bootSwitch && jobAnno.disabled();    //bootSwitch=false: 统一关闭
		LiteJobConfiguration liteJobConfig = LiteJobConfiguration.newBuilder(
						new DataflowJobConfiguration(coreConfig, jobClass, streaming))
					.overwrite(jobAnno.overwrite())  //本地配置是否覆盖注册中心的[false]
					.reconcileIntervalMinutes(10)    //自诊断
					.jobShardingStrategyClass(SHARDING_STRATEGY)  //分片策略 
					.disabled(disabled)    			//启动或失效
					.build();
		ElasticJobListener[] listeners = instantiateListeners(jobAnno.listeners(), false);
		return new LiteJobConfigWrapper(liteJobConfig).setJobListeners(listeners);
	}
	
	public static LiteJobConfigWrapper build(ScriptJob jobAnno, String namespace, boolean bootSwitch) {
		String jobName = jobAnno.name();
		if (jobName.endsWith("Job") || jobName.endsWith("job")) {
			jobName = jobName.substring(0, jobName.length()-3);
		}
		jobName = namespace + "/" + jobName+"ScriptJob";
		String cron = jobAnno.cron();
		String jobParam = jobAnno.jobParameter();
		JobCoreConfiguration coreConfig = newJobCoreConfiguration(jobName, cron,
					jobAnno.description(), jobParam, jobAnno.sharddingParams(),
					jobAnno.failover(), jobAnno.misfire(), jobAnno.eventTrace()
				);
		String scriptCommand = jobAnno.scriptCommand();
		boolean disabled = bootSwitch && jobAnno.disabled();    //bootSwitch=false: 统一关闭
		LiteJobConfiguration liteJobConfig = LiteJobConfiguration.newBuilder(
						new ScriptJobConfiguration(coreConfig, scriptCommand))
					.overwrite(jobAnno.overwrite())  //本地配置是否覆盖注册中心的[false]
					.reconcileIntervalMinutes(10)    //自诊断
					.jobShardingStrategyClass(SHARDING_STRATEGY)  //分片策略 
					.disabled(disabled)    			//启动或失效
					.build();
		ElasticJobListener[] listeners = instantiateListeners(jobAnno.listeners(), false);
		return new LiteJobConfigWrapper(liteJobConfig).setJobListeners(listeners);
	}
	
	private static String getShardingParamters(String[] shardingParams) {
		StringBuilder shardParams = new StringBuilder();
		if (shardingParams.length > 0) {
			int shards = shardingParams.length;
			for (int i=0; i<shards; i++) {
				shardParams.append(i).append("=").append(shardingParams[i]).append(",");
			}
			shardParams.deleteCharAt(shardParams.length()-1);
		}
		return shardParams.toString();
	}
	private static String getJobRuntimeProps(boolean allowSend) {
		HashMap<String,Object> runtimes = new HashMap<String,Object>(1);
		runtimes.put("allowSendEvent", allowSend);
		return runtimes.toString();
	}
	private static JobCoreConfiguration newJobCoreConfiguration(
						String jobName, String cron, String description,
						String jobParam, String[] shardingParams, 
						boolean failover, boolean misfire, boolean allowSend) {
		int shards = shardingParams==null ? 0 : shardingParams.length;
		String key1 = JobPropertiesEnum.EXECUTOR_SERVICE_HANDLER.getKey();
		String key2 = JobPropertiesEnum.JOB_EXCEPTION_HANDLER.getKey();
		String key3 = JobPropertiesEnum.JOB_RUNTIME_PROPERTIES.getKey();
		boolean flag = QuartzUtils.getIntervalMils(cron) >= 1800 * 1000;
		allowSend = allowEvent && allowSend && flag;
		return JobCoreConfiguration.newBuilder(jobName, cron, shards)
					.jobParameter(jobParam)
					.shardingItemParameters(getShardingParamters(shardingParams))
					.failover(failover)    //失效转移[true]
					.misfire(misfire)      //错过重跑[true]
					.jobProperties(key1, EXECUTOR_SERVICE_HANDLER)  //线程池创建者
					.jobProperties(key2, JOB_EXCEPTION_HANDLER)  	//异常处理
					.jobProperties(key3, getJobRuntimeProps(allowSend))   //自定义属性
					.description(description)
					.build();
	}
	private static ElasticJobListener[] instantiateListeners(Class<ElasticJobListener>[] classes,
					boolean simpleJobFlag) {
		int len = classes.length;
		ElasticJobListener[] listeners = new ElasticJobListener[len];
		if (simpleJobFlag) {
			listeners = new ElasticJobListener[len+1];
		}
		int offs = listeners.length - len;
		for (int i=0; i<len; i++) {
			try {
				listeners[i+offs] = classes[i].newInstance();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return listeners;
	}

}
