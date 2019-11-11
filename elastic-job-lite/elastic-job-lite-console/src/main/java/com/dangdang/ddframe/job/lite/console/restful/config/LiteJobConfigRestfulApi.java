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

package com.dangdang.ddframe.job.lite.console.restful.config;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.dangdang.ddframe.job.lite.console.service.JobAPIService;
import com.dangdang.ddframe.job.lite.console.spring.JobAPIServiceSpring;
import com.dangdang.ddframe.job.lite.lifecycle.domain.JobSettings;

/**
 * 作业配置的RESTful API
 *
 * @author caohao
 * @author xiongsl 转换 fullName=jobName.replace("_", "/")
 */
@Path("/jobs/config")
public final class LiteJobConfigRestfulApi {
    
    private JobAPIService jobAPIService = //new JobAPIServiceImpl();
    								JobAPIServiceSpring.getInstance();
    
    /**
     * 获取作业配置.
     * 
     * @param jobName 作业名称
     * @return 作业配置
     */
    @GET
    @Path("/{jobName}")
    @Produces(MediaType.APPLICATION_JSON)
    public JobSettings getJobSettings(@PathParam("jobName") final String jobName) {
    	String fullName = jobName.replace("_", "/");
        return jobAPIService.getJobSettingsAPI().getJobSettings(fullName);
    }
    
    /**
     * 修改作业配置
     * 
     * @param jobSettings 作业配置
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public void updateJobSettings(final JobSettings jobSettings) {
        jobAPIService.getJobSettingsAPI().updateJobSettings(jobSettings);
    }
    
    /**
     * 删除作业配置
     * 
     * @param jobName 作业名称
     */
    @DELETE
    @Path("/{jobName}")
    public void removeJob(@PathParam("jobName") final String jobName) {
    	String fullName = jobName.replace("_", "/");
        jobAPIService.getJobSettingsAPI().removeJobSettings(fullName);
    }
}