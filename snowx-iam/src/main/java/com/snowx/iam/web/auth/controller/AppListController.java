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
 

package com.snowx.iam.web.auth.controller;

import com.snowx.iam.authn.annotation.CurrentUser;
import com.snowx.iam.constants.ConstsStatus;
import com.snowx.iam.crypto.password.PasswordReciprocal;
import com.snowx.iam.entity.Accounts;
import com.snowx.iam.entity.Message;
import com.snowx.iam.entity.apps.Apps;
import com.snowx.iam.entity.apps.UserApps;
import com.snowx.iam.entity.idm.UserInfo;
import com.snowx.iam.persistence.service.AccountsService;
import com.snowx.iam.persistence.service.AppsService;
import com.snowx.iam.persistence.service.UserInfoService;
import com.snowx.iam.persistence.mybatis.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AppListController.
 * 
 * @author Administrator
 *
 */
@RestController
public class AppListController {
    static final Logger logger = LoggerFactory.getLogger(AppListController.class);
    
    @Autowired
    UserInfoService userInfoService;

    @Autowired
    AccountsService accountsService;

    @Autowired
    AppsService appsService;

    /**
     * gridList.
     * @param gridList 类型
     * @return
     */
    @GetMapping(value = { "/appList" }, produces = {MediaType.APPLICATION_JSON_VALUE})
    public Message<List<UserApps>> appList(
            @RequestParam(required = false) String gridList,
            @CurrentUser UserInfo currentUser) {
        userInfoService.updateGridList(gridList,currentUser);
        UserApps userApps = new UserApps();
        userApps.setUsername(currentUser.getUsername());
        userApps.setInstId(currentUser.getInstId());
        List<UserApps> appList = appsService.queryMyApps(userApps);
        for (UserApps app : appList) {
            app.transIconBase64();
        }
        return new Message<>(appList);
    }
 
    
    @GetMapping(value = { "/account/get" })
    public Message<Accounts> getAccount(
            @RequestParam String credential,
            @RequestParam String appId,
            @CurrentUser UserInfo currentUser) {
        Accounts account = null ;
        
        if (credential.equalsIgnoreCase(Apps.CREDENTIALS.USER_DEFINED)) {
            account = accountsService.get(Query.<Accounts>builder().eq("appId", appId).eq("userid", currentUser.getId()));
            account.setRelatedPassword(
                    PasswordReciprocal.getInstance().decoder(
                            account.getRelatedPassword()));
        }else {
            account = new Accounts();
            account.setAppId(appId);
            account.setUserId(currentUser.getId());
            account.setUsername(currentUser.getUsername());
            account.setDisplayName(currentUser.getDisplayName());
        }
        return new Message<>(account);

    }

    @PutMapping(value = { "/account/update" })
    public Message<Accounts> updateAccount(
            @RequestParam String credential,
            @ModelAttribute Accounts account,
            @CurrentUser UserInfo currentUser) {
        Accounts appUsers = new Accounts();
        if (credential.equalsIgnoreCase(Apps.CREDENTIALS.USER_DEFINED)) {
            appUsers = accountsService.get(Query.<Accounts>builder().eq("appId", account.getAppId()).eq("userid", currentUser.getId()));
            if (appUsers == null) {
                appUsers = new Accounts();
                appUsers.setId(appUsers.generateId());
                appUsers.setUserId(currentUser.getId());
                appUsers.setUsername(currentUser.getUsername());
                appUsers.setDisplayName(currentUser.getDisplayName());

                appUsers.setRelatedPassword(
                        PasswordReciprocal.getInstance().encode(account.getRelatedPassword()));
                appUsers.setInstId(currentUser.getInstId());
                appUsers.setStatus(ConstsStatus.ACTIVE);
                accountsService.insert(appUsers);
            } else {
                appUsers.setRelatedUsername(account.getRelatedUsername());
                appUsers.setRelatedPassword(
                        PasswordReciprocal.getInstance().encode(account.getRelatedPassword()));
                accountsService.update(appUsers);
            }
        }

        return new Message<>();
    }
}
