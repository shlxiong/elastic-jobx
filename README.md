# Elastic-Job - distributed scheduled job solution

[![Build Status](https://secure.travis-ci.org/elasticjob/elastic-job-lite.png?branch=master)](https://travis-ci.org/elasticjob/elastic-job-lite)
[![Maven Status](https://maven-badges.herokuapp.com/maven-central/com.dangdang/elastic-job-lite/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.dangdang/elastic-job-lite)
[![Gitter](https://badges.gitter.im/Elastic-JOB/elastic-job-lite.svg)](https://gitter.im/Elastic-JOB/elasticjob?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)
[![Coverage Status](https://coveralls.io/repos/elasticjob/elastic-job/badge.svg?branch=master&service=github)](https://coveralls.io/github/elasticjob/elastic-job?branch=master)
[![GitHub release](https://img.shields.io/github/release/elasticjob/elastic-job.svg)](https://github.com/elasticjob/elastic-job/releases)
[![License](https://img.shields.io/badge/license-Apache%202-4EB1BA.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)

# [中文主页](http://elasticjob.io/index_zh.html)

# Elastic-Job-Lite Console [![GitHub release](https://img.shields.io/badge/release-download-orange.svg)](https://elasticjob.io/dist/elastic-job-lite-console-2.1.5.tar.gz)

# 修改点

* Namespace增加了一层"elastic-job/{APP_NAME}"/{JOB_NAME}，同步修改elastic-job-lite-console模块
* 重写了调用事件的日志，增加两个Status（prepare、clean），并设置一个启用开关: allowSendEvent
     当本机没有分片时，不生成TaskInfo和日志信息
* 新增一种分片策略，避免只有一个分片时，多个任务尽量分布到多台机器
* 增加Job启动的初始状态-失效，避免一开始就触发任务
* Listener瘦身：将ZooKeeper监听器下移；单例
* 管理控制台：
     使用Spring单例
     使用autoconfig替代全局配置 ${user.home}/.elastic-job/configurations.xml
     bug-fix：当有服务器shutdown，且其他均disable时，作业状态显示为:"分片待调整"，而实际应该是:"已失效"
     校验Quartz表达式

# 特性

√作业注册中心：基于Zookeeper和其客户端Curator实现的全局作业注册控制中心。用于注册，控制和协调分布式作业执行。
√作业分片：将一个任务（按IP）分片成为多个小任务项在多服务器上同时执行。
√弹性扩容缩容：在作业运行中，服务器崩溃或新增加n台作业服务器，将在下次作业执行前重新分片，不影响当前作业执行。
√失效转移：在本次作业执行过程中，监测其他作业服务器空闲，抓取未完成的孤儿分片项执行。
√运行时状态收集：监控作业运行时状态，统计最近一段时间处理的数据成功和失败数量，记录作业上次运行开始时间，结束时间和下次运行时间。
√作业停止，恢复和禁用：用于操作作业启停，并可以禁止某作业运行（上线时常用）。
√被错过执行的作业重触发：自动记录错过执行的作业，并在上次作业完成后自动触发。
√多线程快速处理数据，提升吞吐量。
√幂等性：开启monitorExecution会做作业重复行判定。（不推荐）会对性能有较大影响。
√容错处理：作业服务器与Zookeeper服务器通信失败则立即停止作业运行，防止将失效的分片分项配给其他作业服务器，导致重复执行。

# 架构

## Elastic-Job-Lite

![Elastic-Job-Lite Architecture](http://elasticjob.io/docs/elastic-job-lite/img/architecture/elastic_job_lite.png)


# [Release Notes](https://github.com/elasticjob/elastic-job/releases)

# [Roadmap](ROADMAP.md)

# Quick Start

## Add maven dependency

```xml
<!-- import elastic-job lite core -->
<dependency>
    <groupId>io.elasticjob</groupId>
    <artifactId>elastic-job-lite-core</artifactId>
    <version>${lasted.release.version}</version>
</dependency>

<!-- import other module if need -->
<dependency>
    <groupId>io.elasticjob</groupId>
    <artifactId>elastic-job-lite-spring</artifactId>
    <version>${lasted.release.version}</version>
</dependency>
```
## Job development

```java
public class MyElasticJob implements SimpleJob {
    
    @Override
    public void execute(ShardingContext context) {
        switch (context.getShardingItem()) {
            case 0: 
                // do something by sharding item 0
                break;
            case 1: 
                // do something by sharding item 1
                break;
            case 2: 
                // do something by sharding item 2
                break;
            // case n: ...
        }
    }
}
```

## Job configuration

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:reg="http://www.dangdang.com/schema/ddframe/reg"
    xmlns:job="http://www.dangdang.com/schema/ddframe/job"
    xsi:schemaLocation="http://www.springframework.org/schema/beans
                        http://www.springframework.org/schema/beans/spring-beans.xsd
                        http://www.dangdang.com/schema/ddframe/reg
                        http://www.dangdang.com/schema/ddframe/reg/reg.xsd
                        http://www.dangdang.com/schema/ddframe/job
                        http://www.dangdang.com/schema/ddframe/job/job.xsd
                        ">
    <!--configure registry center -->
    <reg:zookeeper id="regCenter" server-lists="yourhost:2181" namespace="dd-job" base-sleep-time-milliseconds="1000" max-sleep-time-milliseconds="3000" max-retries="3" />

    <!--configure job -->
    <job:simple id="myElasticJob" class="xxx.MyElasticJob" registry-center-ref="regCenter" cron="0/10 * * * * ?"   sharding-total-count="3" sharding-item-parameters="0=A,1=B,2=C" />
</beans>
```
