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
 

package com.snowx.iam.web.admin.controller.idm;

import org.apache.commons.lang3.StringUtils;
import com.snowx.iam.authn.annotation.CurrentUser;
import com.snowx.iam.entity.Message;
import com.snowx.iam.entity.idm.GroupMember;
import com.snowx.iam.entity.idm.Groups;
import com.snowx.iam.entity.idm.UserInfo;
import com.snowx.iam.persistence.service.GroupMemberService;
import com.snowx.iam.persistence.service.GroupsService;
import com.snowx.iam.persistence.service.HistorySystemLogsService;
import com.snowx.iam.persistence.service.UserInfoService;
import com.snowx.iam.web.WebContext;
import com.snowx.iam.persistence.mybatis.PageResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping(value={"/admin/access/groupmembers"})
public class GroupMemberController {
    static final  Logger logger = LoggerFactory.getLogger(GroupMemberController.class);
    
    @Autowired
    GroupMemberService service;

    @Autowired
    GroupsService rolesService;
    
    @Autowired
    UserInfoService userInfoService;
    
    @Autowired
    HistorySystemLogsService systemLog;
    
    @RequestMapping(value = { "/fetch" }, produces = {MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    public Message<?> fetch(
            @ModelAttribute GroupMember groupMember,
            @CurrentUser UserInfo currentUser) {
        logger.debug("fetch {}" , groupMember);
        groupMember.setInstId(currentUser.getInstId());
        return new Message<PageResults<GroupMember>>(
                service.fetchPageResults(groupMember));
    }

    @RequestMapping(value = { "/memberIn" })
    @ResponseBody
    public Message<?> memberInRole(@ModelAttribute GroupMember groupMember,@CurrentUser UserInfo currentUser) {
        logger.debug("groupMember : {}" , groupMember);
        groupMember.setInstId(currentUser.getInstId());
        return new Message<PageResults<GroupMember>>(
                service.fetchPageResults("memberIn",groupMember));

    }

    
    @RequestMapping(value = { "/memberNotIn" })
    @ResponseBody
    public Message<?> memberNotIn(@ModelAttribute  GroupMember groupMember,@CurrentUser UserInfo currentUser) {
        groupMember.setInstId(currentUser.getInstId());
        return new Message<PageResults<GroupMember>>(
                service.fetchPageResults("memberNotIn",groupMember));
    }

    @RequestMapping(value = { "/noMember" })
    @ResponseBody
    public Message<?> noMember(@ModelAttribute  GroupMember groupMember,@CurrentUser UserInfo currentUser) {
        groupMember.setInstId(currentUser.getInstId());
        return new Message<PageResults<Groups>>(
                service.noMember(groupMember));
    }
    
    /**
     * Members add to the Role
     * @param groupMember
     * @param currentUser
     * @return
     */
    @RequestMapping(value = {"/add"})
    @ResponseBody
    public Message<?> addGroupMember(@RequestBody GroupMember groupMember,@CurrentUser UserInfo currentUser) {
        if (groupMember == null || groupMember.getGroupId() == null) {
            return new Message<GroupMember>(Message.FAIL);
        }
        String groupId = groupMember.getGroupId();
        
        
        boolean result = true;
        String memberIds = groupMember.getMemberId();
        String memberNames = groupMember.getMemberName();
        if (memberIds != null) {
            String[] arrMemberIds = memberIds.split(",");
            String[] arrMemberNames = memberNames.split(",");
            //set default as USER
            if(StringUtils.isBlank(groupMember.getType())) {
                groupMember.setType("USER");
            }
            for (int i = 0; i < arrMemberIds.length; i++) {
                if(StringUtils.isNotBlank(arrMemberIds[i])) {
                    GroupMember newGroupMember = 
                            new GroupMember(
                                    groupId,
                                groupMember.getGroupName(), 
                                arrMemberIds[i], 
                                arrMemberNames[i],
                                groupMember.getType(),
                                currentUser.getInstId());
                    newGroupMember.setId(WebContext.genId());
                    result = service.insert(newGroupMember);
                }
            }
            if(result) {
                return new Message<GroupMember>(Message.SUCCESS);
            }
        }
        return new Message<GroupMember>(Message.FAIL);
    }
    
    
    /**
     * Member add to Roles
     * @param groupMember
     * @param currentUser
     * @return
     */
    @RequestMapping(value = {"/addMember2Groups"})
    @ResponseBody
    public Message<?> addMember2Groups(@RequestBody GroupMember groupMember,@CurrentUser UserInfo currentUser) {
        if (groupMember == null || StringUtils.isBlank(groupMember.getUsername())) {
            return new Message<GroupMember>(Message.FAIL);
        }
        UserInfo userInfo = userInfoService.findByUsername(groupMember.getUsername());
        
        boolean result = true;
        String groupIds = groupMember.getGroupId();
        String groupNames = groupMember.getGroupName();
        if (groupIds != null && userInfo != null) {
            String[] arrGroupIds = groupIds.split(",");
            String[] arrGroupNames = groupNames.split(",");
            
            for (int i = 0; i < arrGroupIds.length; i++) {
                if(StringUtils.isNotBlank(arrGroupIds[i])) {
                    GroupMember newGroupMember = 
                            new GroupMember(
                                    arrGroupIds[i],
                                    arrGroupNames[i], 
                                    userInfo.getId(), 
                                    userInfo.getDisplayName(),
                                    "USER",
                                    currentUser.getInstId());
                    newGroupMember.setId(WebContext.genId());
                    result = service.insert(newGroupMember);
                }
            }
            if(result) {
                return new Message<GroupMember>(Message.SUCCESS);
            }
        }
        return new Message<GroupMember>(Message.FAIL);
    }
    
    @ResponseBody
    @RequestMapping(value={"/delete"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    public Message<?> delete(@RequestParam List<String> ids,@CurrentUser UserInfo currentUser) {
        logger.debug("-delete ids : {}" , ids);
        if (service.deleteBatch(ids)) {
             return new Message<GroupMember>(Message.SUCCESS);
        } else {
            return new Message<GroupMember>(Message.FAIL);
        }
    }
}
