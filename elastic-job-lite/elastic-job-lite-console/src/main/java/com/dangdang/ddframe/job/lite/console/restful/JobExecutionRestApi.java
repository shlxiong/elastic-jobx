package com.dangdang.ddframe.job.lite.console.restful;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dangdang.ddframe.job.event.rdb.JobEventRdbSearch.Result;
import com.dangdang.ddframe.job.lite.console.spring.SpringRegistrySettings;

import cn.sumpay.config.autotect.elasticjob.eventlog.EventLogRepository;
import cn.sumpay.config.autotect.elasticjob.eventlog.JobExecutionBean;
import cn.sumpay.config.autotect.elasticjob.eventlog.JobStatusLogBean;
import cn.sumpay.config.autotect.elasticjob.eventlog.JobTaskInfoBean;
import cn.sumpay.config.autotect.elasticjob.jpa.QueryMap;
import cn.sumpay.config.util.StringUtils;

/**
 * 查询作业的历史状态与执行情况
 * 替换 EventTraceHistoryRestfulApi
 * @author xiongsl
 */
@Path("tracelog")
public class JobExecutionRestApi {
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private EventLogRepository dao;
	
	public JobExecutionRestApi() {
		try {
			dao = new EventLogRepository(SpringRegistrySettings.getEventDataSource());
		} catch (Throwable e) {
			dao = null;
		}
	}
	@GET
	@Path("/taskinfo")
	public Result<JobTaskInfoBean> queryTaskInfos(@Context final UriInfo uriInfo) {
		int pageSize = this.getIntParam(uriInfo, "pageSize", 20);
		int pageNo = this.getIntParam(uriInfo, "pageNo", 1) - 1;   //bootstrap-table页码从1开始
		String orders = null;
		String param = this.getParameter(uriInfo, "sort");
		if (!StringUtils.isEmpty(param)) {
			orders = param + " ";
			param = this.getParameter(uriInfo, "order");
			orders += StringUtils.isEmpty(param) ? "desc" : param;
		} else {
			orders = "startTime desc";
		}
		
		QueryMap<Object> wheres = new QueryMap<Object>(4);
		param = this.getParameter(uriInfo, "instanceId");
		if (!StringUtils.isEmpty(param)) {
			wheres.put("instanceId like", param+"@%");
		}
		param = this.getParameter(uriInfo, "jobName");
		if (!StringUtils.isEmpty(param)) {
			wheres.put("jobName", param);
		}
        
        Date startTime = this.getDateParam(uriInfo, "startTime");
        Date endTime = this.getDateParam(uriInfo, "endTime");
        if (startTime != null) {
        	wheres.put("startTime", endTime);
        }
        if (endTime != null) {
        	wheres.put("endTime", endTime);
        }
        
        try {
        	return dao.queryTaskInfo(wheres, orders, pageNo, pageSize);
        } catch (SQLException e) {
        	logger.error("", e);
        	return null;
        }
	}
	
	@GET
	@Path("/execution")
	public List<JobExecutionBean> getExecutions(@QueryParam("taskId") String taskId){
		try {
			return dao.getExecutions(taskId);
		} catch (SQLException e) {
			logger.error("", e);
			return new ArrayList<JobExecutionBean>(0);
		}
	}
	
	@GET
	@Path("/status")
	public List<JobStatusLogBean> getStatusLogs(@QueryParam("taskId") String taskId){
		try {
			return dao.getStatusLogs(taskId);
		} catch (SQLException e) {
			logger.error("", e);
			return new ArrayList<JobStatusLogBean>(0);
		}
	}
	
//	@Path("/status")
//	public Result<JobStatusLogBean> queryStatusLogs(@Context final UriInfo uriInfo){
//		return null;
//	}
//	
//	@Path("/execution")
//	public Result<JobExecutionBean> queryTaskExecutions(@Context final UriInfo uriInfo){
//		return null;
//	}

	private String getParameter(UriInfo info, String name) {
		return info.getQueryParameters().getFirst(name);
	}
	private int getIntParam(UriInfo info, String name, int defaultValue) {
		String param = info.getQueryParameters().getFirst(name);
		try {
			return Integer.parseInt(param);
		} catch(Exception e) {
			return defaultValue;
		}
	}
	private Date getDateParam(UriInfo info, String name) {
		String param = info.getQueryParameters().getFirst(name);
		SimpleDateFormat sdfrmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		try {
			return sdfrmt.parse(param);
		} catch(Exception e) {
			return null;
		}
	}
	
}
