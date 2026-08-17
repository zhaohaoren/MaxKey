/*
 * Copyright [2020] [MaxKey of copyright http://www.maxkey.top]
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
 
package com.snowx.iam.web.admin.controller.access;

import org.apache.commons.lang3.StringUtils;
import com.snowx.iam.authn.annotation.CurrentUser;
import com.snowx.iam.entity.Access;
import com.snowx.iam.entity.Message;
import com.snowx.iam.entity.apps.Apps;
import com.snowx.iam.entity.idm.UserInfo;
import com.snowx.iam.persistence.service.AccessService;
import com.snowx.iam.persistence.service.HistorySystemLogsService;
import com.snowx.iam.web.WebContext;
import com.snowx.iam.persistence.mybatis.PageResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value={"/admin/access/access"})
public class AccessController {
    static final Logger logger = LoggerFactory.getLogger(AccessController.class);
    
    @Autowired
    AccessService accessService;

    @Autowired
    HistorySystemLogsService systemLog;
    
    @GetMapping(value = { "/appsInGroup" })
    public Message<PageResults<Access>> appsInRole(
            @ModelAttribute Access groupPermission,
            @CurrentUser UserInfo currentUser) {
        PageResults<Access> groupPermissions;
        groupPermission.setInstId(currentUser.getInstId());
        groupPermissions= accessService.fetchPageResults("appsInGroup",groupPermission);

        if(groupPermissions!=null&&groupPermissions.getRows()!=null){
            for (Apps app : groupPermissions.getRows()){
                app.transIconBase64();
            }
        }
        return new Message<>(groupPermissions);
    }
    
    @GetMapping(value = { "/appsNotInGroup" })
    public Message<PageResults<Access>> appsNotInRole(
                @ModelAttribute Access groupPermission,
                @CurrentUser UserInfo currentUser) {
        PageResults<Access> groupPermissions;
        groupPermission.setInstId(currentUser.getInstId());
        groupPermissions= accessService.fetchPageResults("appsNotInGroup",groupPermission);

        if(groupPermissions!=null&&groupPermissions.getRows()!=null){
            for (Apps app : groupPermissions.getRows()){
                app.transIconBase64();
            }
        }
        return new Message<>(groupPermissions);
    }

    @PostMapping(value = {"/add"})
    public Message<Access> insertPermission(
                @RequestBody Access groupPermission,
                @CurrentUser UserInfo currentUser) {
        if (groupPermission == null || groupPermission.getGroupId() == null) {
            return new Message<>(Message.FAIL);
        }
        String roleId = groupPermission.getGroupId();
        
        boolean result = true;
        String appIds = groupPermission.getAppId();
        if (appIds != null) {
            String[] arrAppIds = appIds.split(",");
            for (int i = 0; i < arrAppIds.length; i++) {
                if(StringUtils.isNotBlank(arrAppIds[i])) {
                    Access newgroupPermissions = 
                            new Access(roleId, arrAppIds[i],currentUser.getInstId());
                    newgroupPermissions.setId(WebContext.genId());
                    result = accessService.insert(newgroupPermissions);
                }
            }
            if(result) {
                return new Message<>(Message.SUCCESS);
            }
        }
        return new Message<>(Message.FAIL);
    }
    
    @DeleteMapping(value={"/delete"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    public Message<Access> delete(@RequestParam List<String> ids,@CurrentUser UserInfo currentUser) {
        logger.debug("-delete ids : {}" , ids);
        if (accessService.deleteBatch(ids)) {
             return new Message<>(Message.SUCCESS);
        } else {
            return new Message<>(Message.FAIL);
        }
    }

}
