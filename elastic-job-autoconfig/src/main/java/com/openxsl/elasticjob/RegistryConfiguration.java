package com.openxsl.elasticjob;

import org.springframework.beans.factory.annotation.Value;

import com.openxsl.config.autodetect.ScanConfig;

/**
 * 注册中心
 * @author xiongsl
 */
@ScanConfig
public class RegistryConfiguration {
	/**
     * 连接Zookeeper服务器的列表.
     * 如: host1:2181,host2:2181
     */
	@Value("${job.registry.servers}")
	private String serverAddrs;
	/**
     * 连接超时时间(毫秒)
     */
	@Value("${job.registry.connect.timeout:300000}")
	private int connectTimeout;
	/**
     * 会话超时时间(毫秒)
     */
	@Value("${job.registry.session.timeout:30000}")
	private int sessionTimeout;
	/**
	 * 重试次数(毫秒)
	 */
	@Value("${job.registry.retries:3}")
	private int retries;
	/**
     * 等待重试的间隔时间的初始值(毫秒)
     */
	@Value("${job.registry.minSleepTime:1000}")
	private int baseSleepTime;
	/**
     * 等待重试的间隔时间的最大值(毫秒)
     */
	@Value("${job.registry.maxSleepTime:3000}")
	private int maxSleepTime;
	/**
     * 连接Zookeeper的权限令牌.
     * 缺省为不需要权限验证.
     */
	@Value("${job.registry.digest:}")
    private String digest;
	
	public String getServerAddrs() {
		return serverAddrs;
	}
	public void setServerAddrs(String serverAddrs) {
		this.serverAddrs = serverAddrs;
	}
	public int getConnectTimeout() {
		return connectTimeout;
	}
	public void setConnectTimeout(int connectTimeout) {
		this.connectTimeout = connectTimeout;
	}
	public int getSessionTimeout() {
		return sessionTimeout;
	}
	public void setSessionTimeout(int sessionTimeout) {
		this.sessionTimeout = sessionTimeout;
	}
	public int getRetries() {
		return retries;
	}
	public void setRetries(int retries) {
		this.retries = retries;
	}
	public int getBaseSleepTime() {
		return baseSleepTime;
	}
	public void setBaseSleepTime(int baseSleepTime) {
		this.baseSleepTime = baseSleepTime;
	}
	public int getMaxSleepTime() {
		return maxSleepTime;
	}
	public void setMaxSleepTime(int maxSleepTime) {
		this.maxSleepTime = maxSleepTime;
	}
	public String getDigest() {
		return digest;
	}
	public void setDigest(String digest) {
		this.digest = digest;
	}

}
