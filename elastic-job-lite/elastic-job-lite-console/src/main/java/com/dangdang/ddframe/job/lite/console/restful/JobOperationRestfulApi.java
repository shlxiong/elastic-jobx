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

package com.dangdang.ddframe.job.lite.console.restful;

import java.util.Collection;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.dangdang.ddframe.job.lite.console.service.JobAPIService;
import com.dangdang.ddframe.job.lite.console.spring.JobAPIServiceSpring;
import com.dangdang.ddframe.job.lite.lifecycle.domain.JobBriefInfo;
import com.dangdang.ddframe.job.lite.lifecycle.domain.ShardingInfo;
import com.google.common.base.Optional;

/**
 * 作业维度操作的RESTful API.
 *
 * @author caohao
 */
@Path("/jobs")
public final class JobOperationRestfulApi {
    
    private JobAPIService jobAPIService = //new JobAPIServiceImpl();
    							JobAPIServiceSpring.getInstance();
    
    /**
     * 获取作业总数.
     * 
     * @return 作业总数
     */
    @GET
    @Path("/count")
    public int getJobsTotalCount() {
        return jobAPIService.getJobStatisticsAPI().getJobsTotalCount();
    }
    
    /**
     * 获取作业详情.
     * 
     * @return 作业详情集合
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<JobBriefInfo> getAllJobsBriefInfo() {
        return jobAPIService.getJobStatisticsAPI().getAllJobsBriefInfo();
    }
    
    /**
     * 触发作业.
     * 
     * @param jobName 作业名称
     */
    @POST
    @Path("/{jobName}/trigger")
    public void triggerJob(@PathParam("jobName") final String jobName) {
    	String fullName = jobName.replace("_", "/");
        jobAPIService.getJobOperatorAPI().trigger(Optional.of(fullName), Optional.<String>absent());
    }
    
    /**
     * 禁用作业.
     * 
     * @param jobName 作业名称
     */
    @POST
    @Path("/{jobName}/disable")
    @Consumes(MediaType.APPLICATION_JSON)
    public void disableJob(@PathParam("jobName") final String jobName) {
    	String fullName = jobName.replace("_", "/");
        jobAPIService.getJobOperatorAPI().disable(Optional.of(fullName), Optional.<String>absent());
    }
    
    /**
     * 启用作业.
     *
     * @param jobName 作业名称
     */
    @DELETE
    @Path("/{jobName}/disable")
    @Consumes(MediaType.APPLICATION_JSON)
    public void enableJob(@PathParam("jobName") final String jobName) {
    	String fullName = jobName.replace("_", "/");
        jobAPIService.getJobOperatorAPI().enable(Optional.of(fullName), Optional.<String>absent());
    }
    
    /**
     * 终止作业.
     * 
     * @param jobName 作业名称
     */
    @POST
    @Path("/{jobName}/shutdown")
    @Consumes(MediaType.APPLICATION_JSON)
    public void shutdownJob(@PathParam("jobName") final String jobName) {
    	String fullName = jobName.replace("_", "/");
        jobAPIService.getJobOperatorAPI().shutdown(Optional.of(fullName), Optional.<String>absent());
    }
    
    /**
     * 获取分片信息.
     * 
     * @param jobName 作业名称
     * @return 分片信息集合
     */
    @GET
    @Path("/{jobName}/sharding")
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<ShardingInfo> getShardingInfo(@PathParam("jobName") final String jobName) {
    	String fullName = jobName.replace("_", "/");
        return jobAPIService.getShardingStatisticsAPI().getShardingInfo(fullName);
    }
    
    @POST
    @Path("/{jobName}/sharding/{item}/disable")
    @Consumes(MediaType.APPLICATION_JSON)
    public void disableSharding(@PathParam("jobName") final String jobName, @PathParam("item") final String item) {
    	String fullName = jobName.replace("_", "/");
        jobAPIService.getShardingOperateAPI().disable(fullName, item);
    }
    
    @DELETE
    @Path("/{jobName}/sharding/{item}/disable")
    @Consumes(MediaType.APPLICATION_JSON)
    public void enableSharding(@PathParam("jobName") final String jobName, @PathParam("item") final String item) {
    	String fullName = jobName.replace("_", "/");
        jobAPIService.getShardingOperateAPI().enable(fullName, item);
    }
}
