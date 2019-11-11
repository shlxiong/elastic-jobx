package com.openxsl.elasticjob.eventlog;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

@Entity
@Table(name="JOB_STATUS_LOG", indexes= {
		@Index(name="IDX_JOBSTS_TASKID", columnList="taskId")
})
public class JobStatusLogBean {
	@Id
    @Column(length=32)
	private String id;
	@Column(length=32)
	private String taskId;      //任务ID
	@Column(length=16)
	private String state;       //状态
	@Column
	private Date eventTime;     //时间
	@Column(length=128)
	private String message;     //提示信息
	
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
	public String getState() {
		return state;
	}
	public void setState(String state) {
		this.state = state;
	}
	public Date getEventTime() {
		return eventTime;
	}
	public void setEventTime(Date eventTime) {
		this.eventTime = eventTime;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}

}
