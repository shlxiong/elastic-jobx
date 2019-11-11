package com.openxsl.elasticjob.test;

import java.io.IOException;
import java.util.HashMap;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;

import com.dangdang.ddframe.job.api.ShardingContext;
import com.dangdang.ddframe.job.exception.JobConfigurationException;
import com.dangdang.ddframe.job.executor.ShardingContexts;
import com.dangdang.ddframe.job.util.json.GsonFactory;

public class ScriptJobTest {
	
	public static void main(String[] args) {
		ShardingContexts contexts = new ShardingContexts("task1", "testJob",
				1, null, new HashMap<Integer, String>());
		ShardingContext shardingContext = new ShardingContext(contexts, 0);
		String scriptCommandLine = "E:\\servers\\zookeeper-3.4.10\\bin\\zkCli.cmd";
		CommandLine commandLine = CommandLine.parse(scriptCommandLine);
        commandLine.addArgument(GsonFactory.getGson().toJson(shardingContext), false);
        try {
            new DefaultExecutor()
            		.execute(commandLine);
        } catch (final IOException ex) {
            throw new JobConfigurationException("Execute script failure.", ex);
        }
	}

}
