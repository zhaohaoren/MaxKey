/*
 * Copyright [2022] [MaxKey of copyright http://www.maxkey.top]
 * 
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
 */
 

package com.snowx.iam.web.config;

import com.snowx.iam.authn.listener.SessionListenerAdapter;
import com.snowx.iam.authn.session.SessionCategory;
import com.snowx.iam.authn.session.SessionManager;
import com.snowx.iam.persistence.service.GroupsService;
import com.snowx.iam.persistence.service.OrganizationsService;
import com.snowx.iam.persistence.service.RolesService;
import com.snowx.iam.schedule.ScheduleAdapterBuilder;
import com.snowx.iam.web.admin.listener.DynamicGroupsListenerAdapter;
import com.snowx.iam.web.admin.listener.DynamicRolesListenerAdapter;
import com.snowx.iam.web.admin.listener.ReorgDeptListenerAdapter;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MaxKeyMgtListenerConfig  {
    private static final  Logger logger = LoggerFactory.getLogger(MaxKeyMgtListenerConfig.class);

    @Bean
    String managementSessionListenerAdapter(
            Scheduler scheduler,
            SessionManager sessionManager) throws SchedulerException {
        new ScheduleAdapterBuilder()
            .setScheduler(scheduler)
            .setIdentity("ManagementSessionListenerAdapter")
            .setCron("0 0/10 * * * ?")
            .setJobClass(SessionListenerAdapter.class)
            .setJobData("sessionManager",sessionManager)
            .setJobData("category", SessionCategory.MGMT)
            .build();
        logger.debug("Session ListenerAdapter inited .");
        return "managementSessionListenerAdapter";
    }

    @Bean
    String reorgDeptListenerAdapter(
            Scheduler scheduler,
            OrganizationsService organizationsService) throws SchedulerException {
        new ScheduleAdapterBuilder()
            .setScheduler(scheduler)
            .setCron("0 0/30 * * * ?")
            .setJobClass(ReorgDeptListenerAdapter.class)
            .setJobData("organizationsService",organizationsService)
            .build();
        logger.debug("ReorgDept ListenerAdapter inited .");
        return "reorgDeptListenerAdapter";
    }

    @Bean
    String dynamicGroupsListenerAdapter(
            Scheduler scheduler,
            GroupsService groupsService,
            @Value("${maxkey.job.cron.schedule}") String cronSchedule
    ) throws SchedulerException {
        new ScheduleAdapterBuilder()
            .setScheduler(scheduler)
            .setCron(cronSchedule)
            .setJobClass(DynamicGroupsListenerAdapter.class)
            .setJobData("groupsService",groupsService)
            .build();

        logger.debug("DynamicGroups ListenerAdapter inited .");
        return "dynamicGroupsListenerAdapter";
    }
    
    @Bean
    String dynamicRolesListenerAdapter(
            Scheduler scheduler,
            RolesService rolesService,
            @Value("${maxkey.job.cron.schedule}") String cronSchedule
    ) throws SchedulerException {
        new ScheduleAdapterBuilder()
            .setScheduler(scheduler)
            .setCron(cronSchedule)
            .setJobClass(DynamicRolesListenerAdapter.class)
            .setJobData("rolesService",rolesService)
            .build();

        logger.debug("Dynamic Roles ListenerAdapter inited .");
        return "dynamicRolesListenerAdapter";
    }
}
