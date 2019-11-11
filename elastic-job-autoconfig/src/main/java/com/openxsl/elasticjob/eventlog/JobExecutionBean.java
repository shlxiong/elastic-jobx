package com.openxsl.elasticjob.eventlog;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

/**
 * 作业状态信息
 * @author xiongsl
 */
@Entity
@Table(name="JOB_EXECUTION_LOG", indexes= {
		@Index(name="IDX_JOBEXEC_TASKID", columnList="taskId")
})
public class JobExecutionBean {
	@Id
	@Column(length=32)
	private String id;
	@Column(length=32)
	private String taskId;         //任务ID
	@Column(length=32)
	private int shardId;           //shard信息
	@Column
	private Date startTime;        //开始时间
	@Column
	private Date finishTime;       //完成时间
	@Column
	private boolean success;       //是否成功
	@Column(length=4000)
	private String errorMsg;       //错误信息
	
	/** NORMAL, MISFIRE, FAILOVER*/
	@Column(length=16)
	private String execSource;     //执行场景
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getTaskId() {
		return taskId;
	}
	public void setTaskId(String taskId) {
		this.taskId = taskId;
	}
	public int getShardId() {
		return shardId;
	}
	public void setShardId(int shardId) {
		this.shardId = shardId;
	}
	public String getErrorMsg() {
		return errorMsg;
	}
	public void setErrorMsg(String errorMsg) {
		this.errorMsg = errorMsg;
	}
	public String getExecSource() {
		return execSource;
	}
	public void setExecSource(String execSource) {
		this.execSource = execSource;
	}
	public Date getStartTime() {
		return startTime;
	}
	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}
	public Date getFinishTime() {
		return finishTime;
	}
	public void setFinishTime(Date finishTime) {
		this.finishTime = finishTime;
	}
	public boolean isSuccess() {
		return success;
	}
	public void setSuccess(boolean success) {
		this.success = success;
	}
	
}
