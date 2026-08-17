/*
 * Copyright [2024] [MaxKey of copyright http://www.maxkey.top]
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
 



package com.snowx.iam.web.admin.controller.permissions;

import org.apache.commons.lang3.StringUtils;
import com.snowx.iam.authn.annotation.CurrentUser;
import com.snowx.iam.entity.Message;
import com.snowx.iam.entity.idm.UserInfo;
import com.snowx.iam.entity.permissions.RoleMember;
import com.snowx.iam.entity.permissions.Roles;
import com.snowx.iam.persistence.service.HistorySystemLogsService;
import com.snowx.iam.persistence.service.RoleMemberService;
import com.snowx.iam.persistence.service.RolesService;
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
@RequestMapping(value={"/admin/permissions/rolemembers"})
public class RoleMemberController {
    static final Logger _logger = LoggerFactory.getLogger(RoleMemberController.class);
    
    @Autowired
    RoleMemberService roleMemberService;

    @Autowired
    RolesService rolesService;
    
    @Autowired
    UserInfoService userInfoService;
    
    @Autowired
    HistorySystemLogsService systemLog;
    
    @GetMapping(value = { "/fetch" }, produces = {MediaType.APPLICATION_JSON_VALUE})
    public Message<PageResults<RoleMember>> fetch(
            @ModelAttribute RoleMember roleMember,
            @CurrentUser UserInfo currentUser) {
        _logger.debug("fetch {}",roleMember);
        roleMember.setInstId(currentUser.getInstId());
        return new Message<>(roleMemberService.fetchPageResults(roleMember));
    }

    @RequestMapping(value = { "/memberInRole" })
    public Message<PageResults<RoleMember>> memberInRole(@ModelAttribute RoleMember roleMember,@CurrentUser UserInfo currentUser) {
        _logger.debug("roleMember : {}",roleMember);
        roleMember.setInstId(currentUser.getInstId());

        return new Message<>(roleMemberService.fetchPageResults("memberInRole",roleMember));
    }

    
    @RequestMapping(value = { "/memberNotInRole" })
    public Message<PageResults<RoleMember>> memberNotInRole(@ModelAttribute  RoleMember roleMember,@CurrentUser UserInfo currentUser) {
        roleMember.setInstId(currentUser.getInstId());
        return new Message<>(roleMemberService.fetchPageResults("memberNotInRole",roleMember));
    }
    
    @RequestMapping(value = { "/memberPostNotInRole" })
    public Message<PageResults<RoleMember>> memberPostNotInRole(@ModelAttribute  RoleMember roleMember,@CurrentUser UserInfo currentUser) {
        roleMember.setInstId(currentUser.getInstId());
        return new Message<>(roleMemberService.fetchPageResults("memberPostNotInRole",roleMember));
    }
    
    @RequestMapping(value = { "/rolesNoMember" })
    public Message<PageResults<Roles>> rolesNoMember(@ModelAttribute  RoleMember roleMember,@CurrentUser UserInfo currentUser) {
        roleMember.setInstId(currentUser.getInstId());
        return new Message<>(roleMemberService.rolesNoMember(roleMember));
    }
    
    /**
     * Members add to the Role
     * @param roleMember
     * @param currentUser
     * @return
     */
    @PostMapping(value = {"/add"})
    @ResponseBody
    public Message<RoleMember> addRoleMember(@RequestBody RoleMember roleMember,@CurrentUser UserInfo currentUser) {
        if (roleMember == null || roleMember.getRoleId() == null) {
            return new Message<>(Message.FAIL);
        }
        String roleId = roleMember.getRoleId();
        
        
        boolean result = true;
        String memberIds = roleMember.getMemberId();
        String memberNames = roleMember.getMemberName();
        if (memberIds != null) {
            String[] arrMemberIds = memberIds.split(",");
            String[] arrMemberNames = memberNames.split(",");
            
            for (int i = 0; i < arrMemberIds.length; i++) {
                RoleMember newRoleMember = 
                        new RoleMember(
                                roleId,
                            roleMember.getRoleName(), 
                            arrMemberIds[i], 
                            arrMemberNames[i],
                            roleMember.getType(),
                            currentUser.getId(),
                            currentUser.getInstId());
                newRoleMember.setId(WebContext.genId());
                result = roleMemberService.insert(newRoleMember);
            }
            if(result) {
                return new Message<>(Message.SUCCESS);
            }
        }
        return new Message<>(Message.FAIL);
    }
    
    
    /**
     * Member add to Roles
     * @param roleMember
     * @param currentUser
     * @return
     */
    @RequestMapping(value = {"/addMember2Roles"})
    public Message<RoleMember> addMember2Roles(@RequestBody RoleMember roleMember,@CurrentUser UserInfo currentUser) {
        if (roleMember == null || StringUtils.isBlank(roleMember.getUsername())) {
            return new Message<>(Message.FAIL);
        }
        UserInfo userInfo = userInfoService.findByUsername(roleMember.getUsername());
        
        boolean result = true;
        String roleIds = roleMember.getRoleId();
        String roleNames = roleMember.getRoleName();
        if (roleIds != null && userInfo != null) {
            String[] arrRoleIds = roleIds.split(",");
            String[] arrRoleNames = roleNames.split(",");
            
            for (int i = 0; i < arrRoleIds.length; i++) {
                RoleMember newRoleMember = 
                        new RoleMember(
                            arrRoleIds[i],
                            arrRoleNames[i], 
                            userInfo.getId(), 
                            userInfo.getDisplayName(),
                            "USER",
                            currentUser.getId(),
                            currentUser.getInstId());
                newRoleMember.setId(WebContext.genId());
                result = roleMemberService.insert(newRoleMember);
            }
            if(result) {
                return new Message<>(Message.SUCCESS);
            }
        }
        return new Message<>(Message.FAIL);
    }
    
    @ResponseBody
    @RequestMapping(value={"/delete"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    public Message<RoleMember> delete(@RequestParam List<String> ids,@CurrentUser UserInfo currentUser) {
        _logger.debug("-delete ids : {}" , ids);
        if (roleMemberService.deleteBatch(ids)) {
             return new Message<>(Message.SUCCESS);
        } else {
            return new Message<>(Message.FAIL);
        }
    }
}
