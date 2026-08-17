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
 

/**
 * 
 */
package com.snowx.iam.authz.endpoint;

import org.apache.commons.lang3.StringUtils;
import com.snowx.iam.authn.annotation.CurrentUser;
import com.snowx.iam.constants.ConstsStatus;
import com.snowx.iam.crypto.password.PasswordReciprocal;
import com.snowx.iam.entity.Accounts;
import com.snowx.iam.entity.Message;
import com.snowx.iam.entity.apps.Apps;
import com.snowx.iam.entity.idm.UserInfo;
import org.springframework.web.bind.annotation.*;

/**
 * @author Crystal.Sea
 *
 */
@RestController
@RequestMapping(value = { "/authz/credential" })
public class AuthorizeCredentialEndpoint extends AuthorizeBaseEndpoint{

    @GetMapping("/get/{appId}")
    public Message<Accounts>  get(
            @PathVariable String appId,
            @CurrentUser UserInfo currentUser){
        Apps app = getApp(appId);
        Accounts account = getAccounts(app,currentUser);
        if(account == null) {
            account =new Accounts ();
            account.setId(account.generateId());
            
            account.setUserId(currentUser.getId());
            account.setUsername(currentUser.getUsername());
            account.setDisplayName(currentUser.getDisplayName());
            
            account.setAppId(appId);
            account.setAppName(app.getAppName());
            account.setInstId(currentUser.getInstId());
            account.setCreateType("manual");
            account.setStatus(ConstsStatus.ACTIVE);
        }
        return new Message<>(account);
    }
    
    @PutMapping("/update")
    public Message<Accounts>  update(
            @RequestBody  Accounts account,
            @CurrentUser UserInfo currentUser){
        if(StringUtils.isNotEmpty(account.getRelatedUsername())
                &&StringUtils.isNotEmpty(account.getRelatedPassword())){
            account.setInstId(currentUser.getInstId());
            account.setRelatedPassword(
                    PasswordReciprocal.getInstance().encode(account.getRelatedPassword()));
            if(accountsService.get(account.getId()) == null) {
                if(accountsService.insert(account)){
                    return new Message<>();
                }
            }else {
                if(accountsService.update(account)){
                    return new Message<>();
                }
            }
        }
        
        return new Message<>(Message.FAIL);
    }
            
}
