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

package com.openxsl.elasticjob.eventlog;

import com.dangdang.ddframe.job.event.JobEvent;
import com.dangdang.ddframe.job.event.JobEventBus;
import com.dangdang.ddframe.job.event.JobEventListener;
import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;

import com.openxsl.elasticjob.thread.ExecutorServiceObject;

/**
 * 运行痕迹事件总线.
 * 
 * @author xiongsl
 */
public class MyJobEventBus implements JobEventBus {
    
    private final ExecutorServiceObject executorServiceObject;
    private final EventBus eventBus;
    private boolean isRegistered;
    
    public MyJobEventBus(final JobEventListener... listeners) {
        executorServiceObject = new ExecutorServiceObject("job-event", null);
        eventBus = new AsyncEventBus(executorServiceObject.createExecutorService());
        try {
        	for (JobEventListener listener : listeners) {
        		eventBus.register(listener);
        	}
            isRegistered = true;
        } catch (NullPointerException npe) {
        	//listener = null;
        } 
    }
    
    /**
     * 发布事件.
     *
     * @param event 作业事件
     */
    @Override
    public void post(final JobEvent event) {
        if (isRegistered && !executorServiceObject.isShutdown()) {
            eventBus.post(event);
        }
    }
    
    @Override
    protected void finalize() {
    	if (!executorServiceObject.isShutdown()) {
    		executorServiceObject.createExecutorService().shutdown();
    	}
//    	executorServiceObject = null;
//    	eventBus = null;
    }
    
}

