package com.openxsl.elasticjob.eventlog;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.dangdang.ddframe.job.event.rdb.JobEventRdbSearch.Result;
import com.dangdang.ddframe.job.event.type.JobExecutionEvent;
import com.dangdang.ddframe.job.event.type.JobStatusTraceEvent;
import com.dangdang.ddframe.job.event.type.JobStatusTraceEvent.State;
import com.dangdang.ddframe.job.util.IDGenerator;

import com.openxsl.elasticjob.jpa.JdbcUtils;
import com.openxsl.elasticjob.jpa.QueryMap;

/**
 * 保存作业的执行日志或状态信息（被线程池调用）
 * @author xiongsl
 */
public class EventLogRepository {
	private final Logger logger = LoggerFactory.getLogger(getClass());
	/** <jobName, taskId> */
	private final ConcurrentMap<String,String> jobTaskMap = new ConcurrentHashMap<String,String>();
	private final DataSource dataSource;
	
	public EventLogRepository(DataSource dataSource) throws SQLException{
		this.dataSource = dataSource;
		if (dataSource != null) {
			JdbcUtils.createTableIfNecessary(JobTaskInfoBean.class, dataSource);
			JdbcUtils.createTableIfNecessary(JobStatusLogBean.class, dataSource);
			JdbcUtils.createTableIfNecessary(JobExecutionBean.class, dataSource);
		}
	}
	
	void saveJobExecution(JobExecutionEvent execEvent) throws SQLException{
		if (dataSource == null) {
			logger.info("save JobExecutionEvent: {}", JSON.toJSONString(execEvent));
			return;
		}
		JobExecutionBean execution = new JobExecutionBean();
		String taskId = execEvent.getContextId(); //this.getTaskId(execEvent.getJobName());
		if (execEvent.getCompleteTime() == null) {
			execution.setId(IDGenerator.getTraceID());
			execution.setTaskId(taskId);
			execution.setShardId(execEvent.getShardingItem());
			execution.setStartTime(execEvent.getStartTime());
			execution.setExecSource(execEvent.getSource().name());
			JdbcUtils.insert(execution, dataSource);
		} else {
			execution.setErrorMsg(execEvent.getFailureCause());
			execution.setFinishTime(execEvent.getCompleteTime());
			execution.setSuccess(execEvent.isSuccess());
			QueryMap<Object> wheres = new QueryMap<Object>(2);
			wheres.put("taskId", taskId);
			wheres.put("shardId", execEvent.getShardingItem());
			JdbcUtils.update(execution, wheres, true, dataSource);
		}
	}
	
	//STAGING, PREPARE, RUNNING, FINISHED/ERROR, POSTED, KILLED, PAUSE
	void addJobStatusLog(final JobStatusTraceEvent statusEvent) throws SQLException{
		if (dataSource == null) {
			logger.info("save JobStatusTraceEvent: {}", JSON.toJSONString(statusEvent));
			return;
		}
		JobTaskInfoBean taskInfo = new JobTaskInfoBean();
		taskInfo.setState(statusEvent.getState().name());
		String jobName = statusEvent.getJobName();
		String taskId = statusEvent.getContextId();
		if (State.TASK_STAGING == statusEvent.getState()) {
			//第一条状态信息，新建Task
			taskInfo.initFromJobRegistry(jobName);
			taskInfo.setStartTime(statusEvent.getCreationTime());
			taskInfo.setTaskId(taskId);
			if (JdbcUtils.insert(taskInfo, dataSource)) {
				jobTaskMap.put(jobName, taskId);
			}
		} else {
			QueryMap<Object> wheres = new QueryMap<Object>("taskId", taskId);
			JdbcUtils.update(taskInfo, wheres, true, dataSource);
		}
		
		JobStatusLogBean statusLog = new JobStatusLogBean();
		statusLog.setId(IDGenerator.getTraceID());
		statusLog.setTaskId(taskId);
		statusLog.setEventTime(statusEvent.getCreationTime());
		statusLog.setState(statusEvent.getState().name());
		statusLog.setMessage(statusEvent.getMessage());
		JdbcUtils.insert(statusLog, dataSource);
	}
	
	public Result<JobTaskInfoBean> queryTaskInfo(QueryMap<?> wheres, String orders,
						int pageNo, int pageSize) throws SQLException{
		return JdbcUtils.query(wheres, JobTaskInfoBean.class, orders, 
						pageNo, pageSize, dataSource);
	}
	
	public List<JobExecutionBean> getExecutions(String taskId) throws SQLException{
		QueryMap<Object> wheres = new QueryMap<Object>("taskId", taskId);
		String orders = "shardId asc, startTime asc";
		return JdbcUtils.query(wheres, JobExecutionBean.class, orders, dataSource);
	}
	
	public List<JobStatusLogBean> getStatusLogs(String taskId) throws SQLException{
		QueryMap<Object> wheres = new QueryMap<Object>("taskId", taskId);
		String orders = "eventTime asc";
		return JdbcUtils.query(wheres, JobStatusLogBean.class, orders, dataSource);
	}
	
//	private boolean isDuplicateRecord(final SQLException ex) {
//        return DatabaseType.MySQL.equals(databaseType) && 1062 == ex.getErrorCode() 
//        	      || DatabaseType.H2.equals(databaseType) && 23505 == ex.getErrorCode() 
//                || DatabaseType.SQLServer.equals(databaseType) && 1 == ex.getErrorCode() 
//                || DatabaseType.DB2.equals(databaseType) && -803 == ex.getErrorCode()
//                || DatabaseType.PostgreSQL.equals(databaseType) && 0 == ex.getErrorCode() 
//                || DatabaseType.Oracle.equals(databaseType) && 1 == ex.getErrorCode();
//    }
	
}
