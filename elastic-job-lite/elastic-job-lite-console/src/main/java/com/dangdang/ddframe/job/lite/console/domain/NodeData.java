package com.dangdang.ddframe.job.lite.console.domain;

import org.apache.zookeeper.data.Stat;

/**
 * ZooKeeper数据
 * @author xiongsl
 */
public class NodeData  {
	private Stat stat;
	private String data;

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public Stat getStat() {
		return stat;
	}

	public void setStat(Stat stat) {
		this.stat = stat;
	}
	
	
}
