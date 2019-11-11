package com.openxsl.elasticjob;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dangdang.ddframe.job.lite.api.strategy.JobInstance;
import com.dangdang.ddframe.job.lite.api.strategy.impl.AverageAllocationJobShardingStrategy;

/**
 * 当只有一个shard时，有可能会将所有的作业都分布在第一个instance上
 * 本策略尽可能的将作业分散在每一台机器上
 * 
 * @author xiongsl
 */
public class CoverageAllHostJobShardingStrategy extends AverageAllocationJobShardingStrategy {
	protected Logger logger = LoggerFactory.getLogger(getClass());

	@Override
	public Map<JobInstance, List<Integer>> sharding(List<JobInstance> jobInstances, 
						String jobName,	int shardingTotalCount) {
		Map<JobInstance, List<Integer>> result;
		if (shardingTotalCount > 1) {
			result = super.sharding(jobInstances, jobName, shardingTotalCount);
		} else {
			result = new HashMap<JobInstance, List<Integer>>(1);
			int index = jobName.length() % jobInstances.size();
			Integer[] shardItems = { 0 };
			result.put(jobInstances.get(index), Arrays.asList(shardItems));
		}
		logger.info("'{}' sharding result: {}", jobName, result);
		
		return result;
	}

}
