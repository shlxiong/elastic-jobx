package com.openxsl.elasticjob.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.stereotype.Component;

import com.dangdang.ddframe.job.lite.api.listener.ElasticJobListener;

/**
 * 定义DataflowJob，与@ElasticJobConf相同
 * 
 * @author xiongsl
 * @create 2019-07-04
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface DataflowJob {
	
	/**
	 * 是否流式处理数据
	 * <p>如果流式处理数据, 则fetchData不返回空结果将持续执行作业<p>
	 * <p>如果非流式处理数据, 则处理数据完成后作业结束<p>
	 * @return
	 */
	boolean streamingProcess() default false;
	
	/*================== 以下与@SimpleJob属性相同 ===================*/
	
	/**
	 * Job唯一名，如果不设置将默认为类名+SimpleJob
	 */
	String name() default "";
	
	/**
	 * 任务执行时间的cron表达式
	 */
	String cron();
	
	/**
	 * 作业描述（中文）
	 */
	String description() default "";
	
//	/**
//	 * 业务方法，包括1~3个参数，依次：(jobParam,shardParam,shardId)
//	 */
//	String method() default "jobExec";
	
	/**
	 * 作业自定义的全局参数，该属性为method()第一个参数
	 */
	String jobParameter() default "";
	
	/**
	 * 分片依据的属性值， 是指作业数据对象的某个属性的枚举值
	 * 检索数据时需带上这个属性，method()的第二个传参为该数组的第n个值
	 */
	String[] sharddingParams() default {};
	
	/**
	 * 监听器，可以在作业执行前和完成后做一些事
	 */
	Class<ElasticJobListener>[] listeners() default {};
	
	/**
	 * 是否每次都覆盖 LiteJobConfiguration
	 */
	boolean overwrite() default false;
	
	/**
	 * 失效转移
	 */
	boolean failover() default true;
	
	/**
	 * 错过重新执行
	 */
	boolean misfire() default true;
	
	/**
	 * 是否做幂等性检查
	 */
	boolean monitor() default false;
	/**
	 * 监听接口
	 */
	int monitorPort() default 8899;
	
	/**
	 * 作业是否禁止启动,可用于部署作业时，先禁止启动，部署结束后统一启动。
	 * 对应相反的配置属性：job.reboot.switch
	 * @return
	 */
	boolean disabled() default false;
	
	/**
	 * 是否启用调度轨迹，若true，则将每一次的调用时间都记录到数据库。
	 * 对应的配置属性：job.event.sendEvent
	 * @return
	 */
	boolean eventTrace() default true;

}
