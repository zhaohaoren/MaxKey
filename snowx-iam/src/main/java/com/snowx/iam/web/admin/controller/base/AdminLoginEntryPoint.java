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
 

package com.snowx.iam.web.admin.controller.base;

import com.snowx.iam.authn.LoginCredential;
import com.snowx.iam.authn.jwt.AuthJwt;
import com.snowx.iam.authn.jwt.AuthTokenService;
import com.snowx.iam.authn.provider.AbstractAuthenticationProvider;
import com.snowx.iam.configuration.ApplicationConfig;
import com.snowx.iam.entity.Institutions;
import com.snowx.iam.entity.Message;
import com.snowx.iam.web.WebConstants;
import com.snowx.iam.web.WebContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;

/**
 * @author Crystal.Sea
 *
 */
@RestController
@RequestMapping(value = "/admin/login")
public class AdminLoginEntryPoint {
    private static Logger logger = LoggerFactory.getLogger(AdminLoginEntryPoint.class);
    
    @Autowired
    AuthTokenService authTokenService;
    
    @Autowired
      ApplicationConfig applicationConfig;
     
    @Autowired
    AbstractAuthenticationProvider authenticationProvider ;
    
    /**
     * init login
     * @return
     */
     @GetMapping("/get")
    public Message<?> get() {
        logger.debug("/login.");
        
        HashMap<String , Object> model = new HashMap<String , Object>();
        Institutions inst = (Institutions)WebContext.getAttribute(WebConstants.CURRENT_INST);
        model.put("inst", inst);
        if(applicationConfig.getLoginConfig().isCaptcha()) {
            model.put("captcha", applicationConfig.getLoginConfig().getCaptchaType());
        }else {
            model.put("captcha", "NONE");
        }
        model.put("state", authTokenService.genRandomJwt());
        return new Message<HashMap<String , Object>>(model);
    }
     
     @PostMapping("/signin")
    public Message<?> signin( @RequestBody LoginCredential loginCredential) {
         Message<AuthJwt> authJwtMessage = new Message<AuthJwt>(Message.FAIL);
         if(authTokenService.validateJwtToken(loginCredential.getState())){
             Authentication  authentication  = authenticationProvider.authenticate(loginCredential);
             if(authentication != null) {
                 AuthJwt authJwt = authTokenService.genAuthJwt(authentication);
                 authJwtMessage = new Message<AuthJwt>(authJwt);
             }else {//fail
                 String errorMsg = WebContext.getAttribute(WebConstants.LOGIN_ERROR_SESSION_MESSAGE) == null ? 
                          "" : WebContext.getAttribute(WebConstants.LOGIN_ERROR_SESSION_MESSAGE).toString();
                authJwtMessage.setMessage(Message.FAIL,errorMsg);
                logger.debug("login fail , message {}",errorMsg);
             }
         }
         return authJwtMessage;
     }
     
}
