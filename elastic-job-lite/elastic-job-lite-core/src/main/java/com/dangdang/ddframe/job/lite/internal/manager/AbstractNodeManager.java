/*
 * Copyright 1999-2015 dangdang.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </p>
 */

package com.dangdang.ddframe.job.lite.internal.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dangdang.ddframe.job.lite.internal.schedule.JobRegistry;

/**
 * 注册中心的监听器管理者的抽象类。（替代AbstractListenerManager）
 * 
 * @author xiongsl
 */
public abstract class AbstractNodeManager {
	protected final Logger logger = LoggerFactory.getLogger(getClass());
	protected final String jobName;
    
    protected AbstractNodeManager(String jobName) {
    	this.jobName = jobName;
    }
    
    protected boolean isShutdown() {
    	return JobRegistry.getInstance().isShutdown(jobName);
    }
    protected boolean isPaused() {
    	return JobRegistry.getInstance().getJobScheduleController(jobName).isPaused();
    }

    /**
     * 开启监听器，调用addDataListener()
     */
    public abstract void start();
    
}
