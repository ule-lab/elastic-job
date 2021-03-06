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

package com.dangdang.ddframe.job.cloud.mesos.facade;

import com.dangdang.ddframe.job.cloud.config.CloudJobConfiguration;
import com.dangdang.ddframe.job.cloud.config.ConfigurationService;
import com.dangdang.ddframe.job.cloud.context.JobContext;
import com.dangdang.ddframe.job.cloud.context.TaskContext;
import com.dangdang.ddframe.job.cloud.producer.TaskProducerSchedulerRegistry;
import com.dangdang.ddframe.job.cloud.state.failover.FailoverService;
import com.dangdang.ddframe.job.cloud.state.misfired.MisfiredService;
import com.dangdang.ddframe.job.cloud.state.ready.ReadyService;
import com.dangdang.ddframe.job.cloud.state.running.RunningService;
import com.dangdang.ddframe.reg.base.CoordinatorRegistryCenter;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * 为Mesos提供的门面服务.
 *
 * @author zhangliang
 */
public class FacadeService {
    
    private final ConfigurationService configService;
    
    private final ReadyService readyService;
    
    private final RunningService runningService;
    
    private final FailoverService failoverService;
    
    private final MisfiredService misfiredService;
    
    private final TaskProducerSchedulerRegistry taskProducerSchedulerRegistry;
    
    public FacadeService(final CoordinatorRegistryCenter regCenter) {
        configService = new ConfigurationService(regCenter);
        readyService = new ReadyService(regCenter);
        runningService = new RunningService(regCenter);
        failoverService = new FailoverService(regCenter);
        misfiredService = new MisfiredService(regCenter);
        taskProducerSchedulerRegistry = TaskProducerSchedulerRegistry.getInstance(regCenter);
    }
    
    /**
     * 框架启动.
     */
    public void start() {
        runningService.clear();
        taskProducerSchedulerRegistry.startup();
    }
    
    /**
     * 获取有资格运行的作业.
     * 
     * @return 作业上下文集合
     */
    public Collection<JobContext> getEligibleJobContext() {
        Collection<JobContext> failoverJobContexts = failoverService.getAllEligibleJobContexts();
        Collection<JobContext> misfiredJobContexts = misfiredService.getAllEligibleJobContexts(failoverJobContexts);
        Collection<JobContext> ineligibleJobContexts = new ArrayList<>(failoverJobContexts.size() + misfiredJobContexts.size());
        ineligibleJobContexts.addAll(failoverJobContexts);
        ineligibleJobContexts.addAll(misfiredJobContexts);
        Collection<JobContext> readyJobContexts = readyService.getAllEligibleJobContexts(ineligibleJobContexts);
        Collection<JobContext> result = new ArrayList<>(failoverJobContexts.size() + misfiredJobContexts.size() + readyJobContexts.size());
        result.addAll(failoverJobContexts);
        result.addAll(misfiredJobContexts);
        result.addAll(readyJobContexts);
        return result;
    }
    
    /**
     * 从队列中删除已运行的作业.
     * 
     * @param taskContexts 任务上下文集合
     */
    public void removeLaunchTasksFromQueue(final List<TaskContext> taskContexts) {
        List<TaskContext> failoverTaskContexts = new ArrayList<>(taskContexts.size());
        Collection<String> misfiredJobNames = new HashSet<>(taskContexts.size(), 1);
        Collection<String> readyJobNames = new HashSet<>(taskContexts.size(), 1);
        for (TaskContext each : taskContexts) {
            switch (each.getType()) {
                case FAILOVER:
                    failoverTaskContexts.add(each);
                    break;
                case MISFIRED:
                    misfiredJobNames.add(each.getMetaInfo().getJobName());
                    break;
                case READY:
                    readyJobNames.add(each.getMetaInfo().getJobName());
                    break;
                default:
                    break;
            }
        }
        failoverService.remove(Lists.transform(failoverTaskContexts, new Function<TaskContext, TaskContext.MetaInfo>() {
            
            @Override
            public TaskContext.MetaInfo apply(final TaskContext input) {
                return input.getMetaInfo();
            }
        }));
        misfiredService.remove(misfiredJobNames);
        readyService.remove(readyJobNames);
    }
    
    /**
     * 将任务运行时上下文放入运行时队列.
     *
     * @param taskContext 任务运行时上下文
     */
    public void addRunning(final TaskContext taskContext) {
        runningService.add(taskContext);
    }
    
    /**
     * 将任务从运行时队列删除..
     *
     * @param metaInfo 任务元信息
     */
    public void removeRunning(final TaskContext.MetaInfo metaInfo) {
        runningService.remove(metaInfo);
    }
    
    /**
     * 记录失效转移队列.
     * 
     * @param taskContext 任务上下文
     */
    public void recordFailoverTask(final TaskContext taskContext) {
        TaskContext.MetaInfo metaInfo = taskContext.getMetaInfo();
        Optional<CloudJobConfiguration> jobConfig = configService.load(metaInfo.getJobName());
        if (jobConfig.isPresent() && jobConfig.get().getTypeConfig().getCoreConfig().isFailover()) {
            failoverService.add(taskContext);
        }
        runningService.remove(metaInfo);
    }
    
    /**
     * 框架停止.
     */
    public void stop() {
        // TODO 停止作业调度
        runningService.clear();
    }
}
