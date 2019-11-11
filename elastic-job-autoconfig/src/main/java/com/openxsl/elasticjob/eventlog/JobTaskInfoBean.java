package com.openxsl.elasticjob.eventlog;

import java.util.Date;
import java.util.Properties;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

import com.alibaba.fastjson.JSON;

/**
 * 作业信息
 * @author xiongsl
 */
@Entity
@Table(name="JOB_TASK_INFO", indexes= {
		@Index(name="IDX_JOBTASK_NAME", columnList="jobName"),
		@Index(name="IDX_JOBTASK_INSTANCE", columnList="instanceId"),
		@Index(name="IDX_JOBTASK_STATE", columnList="state"),
})
public class JobTaskInfoBean {
	@Id
    @Column(length=32)
	private String taskId;         //任务ID
	@Column(length=64)
	private String jobName;        //作业名称
	@Column(length=512)
	private String jobProps;       //作业属性（包括cron,jobParam[32],shardParams[256],namespace[32],instances[128]等）
	@Column(length=128)
	private String instanceId;     //运行的shard实例
	@Column(length=16)
	private String state;          //当前状态
	@Column
	private Date startTime;        //执行开始时间
	
	/**SIMPLE, DATA_FLOW, SCRIPT*/
	@Column(length=16)
	private String jobType;        //作业类型
	
	public void initFromJobRegistry(String jobName) {
		Properties jobProps = JobSchedulerRegistry.getJobContext(jobName);
		this.setJobName(jobName);
		this.setJobType((String)jobProps.remove("jobType"));
		this.setInstanceId((String)jobProps.remove("instanceId"));
		this.setJobProps(JSON.toJSONString(jobProps));
	}
	
	//============================ JavaBean Method ===========================//
	public String getTaskId() {
		return taskId;
	}
	public void setTaskId(String taskId) {
		this.taskId = taskId;
	}
	public String getJobName() {
		return jobName;
	}
	public void setJobName(String jobName) {
		this.jobName = jobName;
	}
	public String getJobProps() {
		return jobProps;
	}
	public void setJobProps(String jobProps) {
		this.jobProps = jobProps;
	}
	public String getInstanceId() {
		return instanceId;
	}
	public void setInstanceId(String instanceId) {
		this.instanceId = instanceId;
	}
	public Date getStartTime() {
		return startTime;
	}
	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}
	public String getJobType() {
		return jobType;
	}
	public void setJobType(String jobType) {
		this.jobType = jobType;
	}
	public String getState() {
		return state;
	}
	public void setState(String state) {
		this.state = state;
	}

}
