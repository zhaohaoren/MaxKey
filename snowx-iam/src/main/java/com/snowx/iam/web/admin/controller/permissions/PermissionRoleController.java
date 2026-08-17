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

import com.snowx.iam.authn.annotation.CurrentUser;
import com.snowx.iam.entity.Message;
import com.snowx.iam.entity.idm.UserInfo;
import com.snowx.iam.entity.permissions.PermissionRole;
import com.snowx.iam.persistence.service.HistorySystemLogsService;
import com.snowx.iam.persistence.service.PermissionRoleService;
import com.snowx.iam.util.StrUtils;
import com.snowx.iam.web.WebContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


@RestController
@RequestMapping(value={"/admin/permissions/permissionRole"})
public class PermissionRoleController {
    static final Logger _logger = LoggerFactory.getLogger(PermissionRoleController.class);
    
    @Autowired
    PermissionRoleService permissionRoleService;
    
    @Autowired
    HistorySystemLogsService systemLog;
    
    @PutMapping(value={"/update"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    public  Message<PermissionRole> update(
            @RequestBody PermissionRole permissionRole,
            @CurrentUser UserInfo currentUser) {
        _logger.debug("-update  : {}" , permissionRole);
        //have
        PermissionRole queryPermissionRole = 
                new PermissionRole(
                        permissionRole.getAppId(),
                        permissionRole.getRoleId(),
                        currentUser.getInstId());
        List<PermissionRole> permissionRolesList = permissionRoleService.queryPermissionRoles(queryPermissionRole);
        
        HashMap<String,String >permissionRolesMap =new HashMap<>();
        for(PermissionRole tempPermissionRole : permissionRolesList) {
            permissionRolesMap.put(tempPermissionRole.getUniqueId(),tempPermissionRole.getId());
        }
        //Maybe insert
        ArrayList<PermissionRole> newPermissionRolesList =new ArrayList<>();
        List<String>resourceIds = StrUtils.string2List(permissionRole.getResourceId(), ",");
        HashMap<String,String >newPermissionRolesMap =new HashMap<>();
        for(String resourceId : resourceIds) {
            PermissionRole newPermissionRole =new PermissionRole(
                    WebContext.genId(),
                    permissionRole.getAppId(),
                    permissionRole.getRoleId(),
                    resourceId,
                    currentUser.getId(),
                    currentUser.getInstId());
            newPermissionRole.setId(newPermissionRole.generateId());
            newPermissionRolesMap.put(newPermissionRole.getUniqueId(), permissionRole.getAppId());
            
            if(!permissionRole.getAppId().equalsIgnoreCase(resourceId) &&
                    !permissionRolesMap.containsKey(newPermissionRole.getUniqueId())) {
                newPermissionRolesList.add(newPermissionRole);
            }
        }
        
        //delete 
        ArrayList<PermissionRole> deletePermissionRolesList =new ArrayList<>();
        for(PermissionRole tempPermissionRole : permissionRolesList) {
           if(!newPermissionRolesMap.containsKey(tempPermissionRole.getUniqueId())) {
               tempPermissionRole.setInstId(currentUser.getInstId());
               deletePermissionRolesList.add(tempPermissionRole);
           }
        }
        if (!deletePermissionRolesList.isEmpty()) {
            _logger.debug("-remove  : {}" , deletePermissionRolesList);
            permissionRoleService.deletePermissionRoles(deletePermissionRolesList);
        }
        
        if (!newPermissionRolesList.isEmpty() && permissionRoleService.insertPermissionRoles(newPermissionRolesList)) {
            _logger.debug("-insert  : {}" ,newPermissionRolesList);
            return new Message<>(Message.SUCCESS);
            
        } else {
            return new Message<>(Message.SUCCESS);
        }
        
    }
    
    @GetMapping(value={"/get"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    public  Message<List<PermissionRole>> get(
            @ModelAttribute PermissionRole permissionRole,
            @CurrentUser UserInfo currentUser) {
        _logger.debug("-get  : {}" , permissionRole);
        //have
        PermissionRole queryPermissionRole = 
                new PermissionRole(
                        permissionRole.getAppId(),
                        permissionRole.getRoleId(),
                        currentUser.getInstId());
        List<PermissionRole>permissionRoleList = permissionRoleService.queryPermissionRoles(queryPermissionRole);
        
        return new Message<>(permissionRoleList);
    }

    
}
