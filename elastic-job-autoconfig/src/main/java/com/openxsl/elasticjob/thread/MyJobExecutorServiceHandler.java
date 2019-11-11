package com.openxsl.elasticjob.thread;

import java.util.concurrent.ExecutorService;

import com.dangdang.ddframe.job.executor.handler.ExecutorServiceHandler;

/**
 * AbstractElasticJobExecutor中创建线程池
 * @author xiongsl
 * @created 2019-05-20
 */
public class MyJobExecutorServiceHandler implements ExecutorServiceHandler {
    
    @Override
    public ExecutorService createExecutorService(final String jobName) {
        return new ExecutorServiceObject("inner-job-", jobName).createExecutorService();
    }

}
