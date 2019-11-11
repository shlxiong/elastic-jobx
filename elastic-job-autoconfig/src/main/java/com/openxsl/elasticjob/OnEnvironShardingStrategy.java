package com.openxsl.elasticjob;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.dangdang.ddframe.job.lite.api.strategy.JobInstance;

/**
 * 根据所处的运行环境来分片
 * @author xiongsl
 */
public class OnEnvironShardingStrategy extends CoverageAllHostJobShardingStrategy {
	private final Pattern pattern = Pattern.compile("\\S+@_@\\d+@_@(\\S+)");
	private String[] environs = {"dev", "test", "uat", "prod", "joint"};

	@Override
	public Map<JobInstance, List<Integer>> sharding(List<JobInstance> jobInstances,
						String jobName,	int shardingTotalCount) {
		Map<JobInstance, List<Integer>> shardingResults = new HashMap<JobInstance, List<Integer>>();
		Map<JobInstance, List<Integer>> environResult = new HashMap<JobInstance, List<Integer>>(2);
		int stage = 0;
		for (Map.Entry<String,List<JobInstance>> entry : 
						this.classifyJobInstances(jobInstances).entrySet()) {
			int size = entry.getValue().size();
			environResult = super.sharding(entry.getValue(), jobName, size); //fixed-[0,1,2]
			//调整基数
			if (environResult.size() > 0) {  
				for (Map.Entry<JobInstance, List<Integer>> children : environResult.entrySet()) {
					List<Integer> shardings = new ArrayList<Integer>(2);
					for (Integer shard : children.getValue()) {
						shardings.add(shard+stage);
					}
					environResult.put(children.getKey(), shardings);
				}
				stage += size;
			}
			shardingResults.putAll(environResult);
		}
		
		logger.info("'{}' [Environmental] sharding result: {}", jobName, shardingResults);
		return shardingResults;
	}
	
	/**
	 * 各环境分别有哪些实例（依次）
	 */
	private Map<String,List<JobInstance>> classifyJobInstances(List<JobInstance> jobInstances){
		Map<String,List<JobInstance>> instanceMap = new LinkedHashMap<String,List<JobInstance>>(4);
		for (String environ : environs) {
			instanceMap.put(environ, new ArrayList<JobInstance>(2));
			for (JobInstance jobInstance : jobInstances) {
				String env = this.getEnvironment(jobInstance.getJobInstanceId());
				if (environ.equals(env)) {
					instanceMap.get(environ).add(jobInstance);
				}
			}
			if (instanceMap.get(environ).size() < 1) {
				instanceMap.remove(environ);
			}
		}
		
		return instanceMap;
	}
	
	private String getEnvironment(String instanceId) {
		Matcher matcher = pattern.matcher(instanceId);
		if (matcher.matches()) {
			return matcher.group(1);
		} else {
			return "prod";
		}
	}
	
	public static void main(String[] args) {
		List<JobInstance> jobInstances = new ArrayList<JobInstance>(2);
		jobInstances.add(new JobInstance("192.168.16.12@_@9421@_@dev"));
		jobInstances.add(new JobInstance("192.168.13.10@_@8639@_@test"));
		jobInstances.add(new JobInstance("192.168.255.103@_@4349@_@uat"));
		jobInstances.add(new JobInstance("192.168.255.102@_@8364@_@uat"));
		jobInstances.add(new JobInstance("10.191.30.122@_@8385@_@prod"));
		jobInstances.add(new JobInstance("10.191.30.123@_@7643@_@prod"));
		OnEnvironShardingStrategy test = new OnEnvironShardingStrategy();
		test.sharding(jobInstances, "testJob", 4);
	}

}
