package com.openxsl.elasticjob.thread;

import java.util.concurrent.ExecutorService;

import com.dangdang.ddframe.job.lite.internal.schedule.JobRegistry;
import com.openxsl.config.thread.GrouppedThreadFactory;
import com.openxsl.config.util.StringUtils;

/**
 * 线程池执行服务对象.
 *
 * @author xiongsl
 */
public final class ExecutorServiceObject {
	private final ExecutorService threadPoolExecutor;
//    private final ThreadPoolExecutor threadPoolExecutor;
//    private final BlockingQueue<Runnable> workQueue;
    
    public ExecutorServiceObject(String prefix, String jobName) {
        if (!StringUtils.isEmpty(jobName)) {
        	prefix += '-'+jobName;
        }
        int size = this.size(jobName);
        threadPoolExecutor = new GrouppedThreadFactory(prefix)
        				.newThreadPool(size, size, 120, 2048);
        
//        workQueue = new LinkedBlockingQueue<>();
//        ThreadFactory factory = new BasicThreadFactory.Builder()
//        			.namingPattern(prefix+"-%s").build();
//        threadPoolExecutor = new ThreadPoolExecutor(size, size, 2L, TimeUnit.MINUTES,
//        				workQueue, factory);
//        threadPoolExecutor.allowCoreThreadTimeOut(true);
    }
    
    /**
     * 创建线程池服务对象.
     * 2019-05-30
     * 原来主要是为了返回ListeningExecutorService，以便可以做callback。
     * Futures.addCallback(future, new FutureCallback<V>() {
	 *
	 *		@Override
	 *		public void onSuccess(V result) {
	 *			// TODO Auto-generated method stub
	 *		}
	 *		@Override
	 *		public void onFailure(Throwable t) {
	 *			// TODO Auto-generated method stub
	 *		}
	 *	});
	 *
     * @return 线程池服务对象
     */
    public ExecutorService createExecutorService() {
    	return threadPoolExecutor;
//        return MoreExecutors.listeningDecorator(MoreExecutors.getExitingExecutorService(threadPoolExecutor));
    }
    
    public boolean isShutdown() {
        return threadPoolExecutor.isShutdown();
    }
    
//    /**
//     * 获取当前活跃的线程数.
//     *
//     * @return 当前活跃的线程数
//     */
//    public int getActiveThreadCount() {
//        return threadPoolExecutor.getActiveCount();
//    }
//    
//    /**
//     * 获取待执行任务数量.
//     *
//     * @return 待执行任务数量
//     */
//    public int getWorkQueueSize() {
//        return workQueue.size();
//    }
    
    /**
     * 
     * @param jobName
     * @return
     */
    private int size(String jobName) {
    	int shards = 1;
    	if (!StringUtils.isEmpty(jobName)) {
    		shards = Math.max(1, JobRegistry.getInstance().getCurrentShardingTotalCount(jobName));
    	}
    	int processors = Runtime.getRuntime().availableProcessors();
    	return Math.min(shards, processors);
    }
    
}
