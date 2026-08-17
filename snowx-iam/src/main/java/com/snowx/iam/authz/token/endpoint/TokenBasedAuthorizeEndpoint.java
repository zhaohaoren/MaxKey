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
 

/**
 * 
 */
package com.snowx.iam.authz.token.endpoint;

import com.snowx.iam.authn.annotation.CurrentUser;
import com.snowx.iam.authn.web.AuthorizationUtils;
import com.snowx.iam.authz.endpoint.AuthorizeBaseEndpoint;
import com.snowx.iam.authz.endpoint.adapter.AbstractAuthorizeAdapter;
import com.snowx.iam.authz.token.endpoint.adapter.TokenBasedDefaultAdapter;
import com.snowx.iam.configuration.ApplicationConfig;
import com.snowx.iam.constants.ConstsBoolean;
import com.snowx.iam.entity.apps.Apps;
import com.snowx.iam.entity.apps.AppsTokenBasedDetails;
import com.snowx.iam.entity.idm.UserInfo;
import com.snowx.iam.persistence.service.AppsTokenBasedDetailsService;
import com.snowx.iam.util.Instance;
import com.snowx.iam.web.WebContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.ModelAndView;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @author Crystal.Sea
 *
 */
@Tag(name = "2-6-TokenBased接口文档模块")
@Controller
public class TokenBasedAuthorizeEndpoint  extends AuthorizeBaseEndpoint{

    static final  Logger _logger = LoggerFactory.getLogger(TokenBasedAuthorizeEndpoint.class);
    @Autowired
    AppsTokenBasedDetailsService tokenBasedDetailsService;

    @Autowired
    ApplicationConfig applicationConfig;
    
    @Operation(summary = "TokenBased认证接口", description = "传递参数应用ID",method="GET")
    @GetMapping("/authz/tokenbased/{id}")
    public ModelAndView authorize(
            HttpServletRequest request,
            HttpServletResponse response,
            @PathVariable String id,
            @CurrentUser UserInfo currentUser){
        ModelAndView modelAndView=new ModelAndView();
        
        
        AppsTokenBasedDetails tokenBasedDetails=null;
        tokenBasedDetails=tokenBasedDetailsService.get(id , true);
        _logger.debug(""+tokenBasedDetails);
        
        Apps  application= getApp(id);
        tokenBasedDetails.setAdapter(application.getAdapter());
        tokenBasedDetails.setIsAdapter(application.getIsAdapter());
        
        AbstractAuthorizeAdapter adapter;
        if(ConstsBoolean.isTrue(tokenBasedDetails.getIsAdapter())){
            adapter =(AbstractAuthorizeAdapter)Instance.newInstance(tokenBasedDetails.getAdapter());
        }else{
            adapter =(AbstractAuthorizeAdapter)new TokenBasedDefaultAdapter();
        }
        adapter.setPrincipal(AuthorizationUtils.getPrincipal());
        adapter.setApp(tokenBasedDetails);
        
        adapter.generateInfo();
        
        adapter.encrypt(
                null, 
                tokenBasedDetails.getAlgorithmKey(), 
                tokenBasedDetails.getAlgorithm());
        
        if("POST".equalsIgnoreCase(tokenBasedDetails.getTokenType())) {
            return adapter.authorize(modelAndView);
        }else {
            _logger.debug("Cookie Name : {}" ,tokenBasedDetails.getCookieName());
            
            Cookie cookie= new Cookie(tokenBasedDetails.getCookieName(),adapter.serialize());
            
            Integer maxAge = tokenBasedDetails.getExpires();
            _logger.debug("Cookie Max Age : {} seconds.",maxAge);
            cookie.setMaxAge(maxAge);
            
            cookie.setPath("/");
            //
            //cookie.setDomain("."+applicationConfig.getBaseDomainName());
            //tomcat 8.5
            cookie.setDomain(applicationConfig.getBaseDomainName());
            
            _logger.debug("Sub Domain Name : .{}",applicationConfig.getBaseDomainName());
            response.addCookie(cookie);
            
            if(tokenBasedDetails.getRedirectUri().indexOf(applicationConfig.getBaseDomainName())>-1){
                return WebContext.redirect(tokenBasedDetails.getRedirectUri());
            }else{
                _logger.error(tokenBasedDetails.getRedirectUri()+" not in domain "+applicationConfig.getBaseDomainName());
                return null;
            }
        }
        
    }

}
