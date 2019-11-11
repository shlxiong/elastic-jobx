package com.openxsl.elasticjob;

import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.springframework.util.Assert;

import com.dangdang.ddframe.job.api.ShardingContext;
import com.openxsl.config.proxy.ClassGenerator;

import javassist.Modifier;

/**
 * 生成SimpleJob动态代理类，调用JobAction的方法完成操作，方法要求：<br>
 *    一至三个参数，依次是：(String jobParam, String shardParam, int shardId)
 * <pre>
 * public class DemoSimpleJob implements com.dangdang.ddframe.job.api.simple.SimpleJob{
 *     private final Logger logger = LoggerFactory.getLogger(getClass());
 *     private Object jobAction;
 *     
 *     public void execute(ShardingContext shardingContext) {
 *     	   String jobParam = shardingContext.getJobParameter();
 *         String shardParam = shardingContext.getShardingParameter();
 *         String shardId = shardingContext.getShardingItem();
 *         Object result = null;
 *         try{
 *         	   result = jobAction.method(jobParam, shardParam, shardId);
 *             logger.info("Job execute result: {}", result);
 *         }catch(Throwable t){
 *             logger.error("Job execute error: ", t);
 *         }
 *     }
 *     
 *     public void setJobAction(Object jobAction){
 *     	   this.jobAction = jobAction;
 *     }
 * }
 * </pre>
 * @author xiongsl
 */
public final class SimpleJobProxy {
	
	public static Class<?> makeSimpleJobClass(Class<?> serviceClass,
					String methodName) throws Exception{
		Assert.notNull(serviceClass, "serviceClass can not be null");
		Assert.notNull(methodName, "methodName can not be null");
		Method method = null;
		for (Method m : serviceClass.getDeclaredMethods()) {
			if (methodName.equals(m.getName())) {  //不能重载
				method = m;
				break;
			}
		}
		if (method == null) {
			throw new IllegalArgumentException(String.format("method '%s' is not found in class: %s",
					methodName, serviceClass.getCanonicalName()));
		}
		
		final String beanId = "jobAction";
		String className = serviceClass.getPackage().getName().replaceAll(".service$", "") + ".job."
				+ serviceClass.getSimpleName().replaceAll("Service$", "") + "SimpleJob";
		ClassGenerator generator = ClassGenerator.newInstance(className);
		generator.addInterface(com.dangdang.ddframe.job.api.simple.SimpleJob.class.getName());
		generator.addField("logger", Modifier.PRIVATE+Modifier.FINAL, 
						Logger.class, "org.slf4j.LoggerFactory.getLogger(getClass())");
		generator.addField(beanId, Modifier.PRIVATE, serviceClass);
		
		StringBuilder body = new StringBuilder();
		body.append("String jobParam = arg0.getJobParameter();\n");
		body.append("int shardId = arg0.getShardingItem();\n");
		body.append("String shardParam = arg0.getShardingParameter();\n");
		body.append("logger.info(\"job parameter: {}\", jobParam);\n")
			.append("logger.info(\"shardId:{}, shardParam:{}\", String.valueOf(shardId),shardParam);\n");
		body.append("try { Object result = this.").append(beanId).append('.')
			.append(methodName).append("(jobParam");
		int argc = method.getParameterTypes().length;
		if (argc >= 2) {
			body.append(", shardParam");
			if (argc == 3) {
				body.append(", shardId");
			}
		} 
		body.append(");\n");
		body.append("logger.info(\"Job execute result: {}\", result);\n");
		body.append("}catch(Throwable t){logger.error(\"Job execute error: \", t);}");
		Class<?>[] paramTypes = {ShardingContext.class};
		generator.addMethod("execute", Modifier.PUBLIC, void.class, paramTypes, body.toString());
		
		body.delete(0, body.length());
		body.append("this.").append(beanId).append(" = arg0");
		paramTypes = new Class<?>[]{serviceClass};
		generator.addMethod("setJobAction", Modifier.PUBLIC, void.class, paramTypes, body.toString());
		return generator.toClass();
	}

}
