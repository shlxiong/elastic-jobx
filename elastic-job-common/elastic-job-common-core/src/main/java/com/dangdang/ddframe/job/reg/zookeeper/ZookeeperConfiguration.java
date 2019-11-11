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

package com.dangdang.ddframe.job.reg.zookeeper;

/**
 * 基于Zookeeper的注册中心配置.
 * 
 * @author zhangliang
 * @author caohao
 */
public final class ZookeeperConfiguration {
    /** 连接zk服务器的列表(127.0.0.1:2181,192.168.1.1:2181) */
    private final String serverLists;
    
    /**  命名空间 */
    private final String namespace;
    
    /** 等待重试的间隔时间的初始值(单位毫秒) */
    private int baseSleepTimeMilliseconds = 1000;
    
    /** 等待重试的间隔时间的最大值 (单位毫秒)  */
    private int maxSleepTimeMilliseconds = 3000;
    
    /**  最大重试次数 */
    private int maxRetries = 3;
    
    /** 会话超时时间(单位毫秒) */
    private int sessionTimeoutMilliseconds;
    
    /** 连接超时时间  (单位毫秒) */
    private int connectionTimeoutMilliseconds;
    
    /** 连接Zookeeper的权限令牌 (缺省为不需要权限验证) */
    private String digest;
    
    public ZookeeperConfiguration(String serverLists, String namespace){
    	this.serverLists = serverLists;
    	this.namespace = namespace;
    }

	public int getBaseSleepTimeMilliseconds() {
		return baseSleepTimeMilliseconds;
	}

	public void setBaseSleepTimeMilliseconds(int baseSleepTimeMilliseconds) {
		this.baseSleepTimeMilliseconds = baseSleepTimeMilliseconds;
	}

	public int getMaxSleepTimeMilliseconds() {
		return maxSleepTimeMilliseconds;
	}

	public void setMaxSleepTimeMilliseconds(int maxSleepTimeMilliseconds) {
		this.maxSleepTimeMilliseconds = maxSleepTimeMilliseconds;
	}

	public int getMaxRetries() {
		return maxRetries;
	}

	public void setMaxRetries(int maxRetries) {
		this.maxRetries = maxRetries;
	}

	public int getSessionTimeoutMilliseconds() {
		return sessionTimeoutMilliseconds;
	}

	public void setSessionTimeoutMilliseconds(int sessionTimeoutMilliseconds) {
		this.sessionTimeoutMilliseconds = sessionTimeoutMilliseconds;
	}

	public int getConnectionTimeoutMilliseconds() {
		return connectionTimeoutMilliseconds;
	}

	public void setConnectionTimeoutMilliseconds(int connectionTimeoutMilliseconds) {
		this.connectionTimeoutMilliseconds = connectionTimeoutMilliseconds;
	}

	public String getDigest() {
		return digest;
	}

	public void setDigest(String digest) {
		this.digest = digest;
	}

	public String getServerLists() {
		return serverLists;
	}

	public String getNamespace() {
		return namespace;
	}
    
}
