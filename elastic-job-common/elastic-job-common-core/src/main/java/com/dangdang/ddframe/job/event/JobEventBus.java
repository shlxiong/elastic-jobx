package com.dangdang.ddframe.job.event;

/**
 * 为了增加扩展性，将类改为接口. 原类改为DefaultJobEventBus
 * @author xiongsl
 * @see DefaultJobEventBus
 */
public interface JobEventBus {
	
	/**
     * 发布事件.
     *
     * @param event 作业事件
     */
	public void post(final JobEvent event);

}
