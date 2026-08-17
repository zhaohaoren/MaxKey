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
package com.snowx.iam.authz.jwt.endpoint;

import java.lang.reflect.InvocationTargetException;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import com.snowx.iam.authn.annotation.CurrentUser;
import com.snowx.iam.authn.web.AuthorizationUtils;
import com.snowx.iam.authz.endpoint.AuthorizeBaseEndpoint;
import com.snowx.iam.authz.endpoint.adapter.AbstractAuthorizeAdapter;
import com.snowx.iam.authz.jwt.endpoint.adapter.JwtAdapter;
import com.snowx.iam.constants.ConstsBoolean;
import com.snowx.iam.constants.ContentType;
import com.snowx.iam.crypto.jose.keystore.JWKSetKeyStore;
import com.snowx.iam.entity.apps.Apps;
import com.snowx.iam.entity.apps.AppsJwtDetails;
import com.snowx.iam.entity.idm.UserInfo;
import com.snowx.iam.persistence.service.AppsJwtDetailsService;
import com.snowx.iam.util.Instance;
import com.snowx.iam.web.WebConstants;
import com.snowx.iam.web.WebContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @author Crystal.Sea
 *
 */
@Tag(name = "2-5-JWT令牌接口")
@Controller
public class JwtAuthorizeEndpoint  extends AuthorizeBaseEndpoint{

    static final  Logger _logger = LoggerFactory.getLogger(JwtAuthorizeEndpoint.class);
    
    @Autowired
    AppsJwtDetailsService jwtDetailsService;
    
    @Operation(summary = "JWT应用ID认证接口", description = "应用ID")
    @GetMapping("/authz/jwt/{id}")
    public ModelAndView authorize(
            HttpServletRequest request,
            HttpServletResponse response,
            @PathVariable String id,
            @CurrentUser UserInfo currentUser){
        ModelAndView modelAndView=new ModelAndView();
        Apps  application = getApp(id);
        AppsJwtDetails jwtDetails = jwtDetailsService.get(application.getId() , true);
        _logger.debug("jwtDetails {}",jwtDetails);
        jwtDetails.setAdapter(application.getAdapter());
        jwtDetails.setIsAdapter(application.getIsAdapter());
        
        AbstractAuthorizeAdapter adapter;
        if(ConstsBoolean.isTrue(jwtDetails.getIsAdapter())){
            Object jwtAdapter = Instance.newInstance(jwtDetails.getAdapter());
            try {
                BeanUtils.setProperty(jwtAdapter, "jwtDetails", jwtDetails);
            } catch (IllegalAccessException | InvocationTargetException e) {
                _logger.error("setProperty error . ", e);
            }
            adapter = (AbstractAuthorizeAdapter)jwtAdapter;
        }else{
            adapter =new JwtAdapter(jwtDetails);
        }
        
        adapter.setPrincipal(AuthorizationUtils.getPrincipal());
        
        adapter.generateInfo();
        //sign
        adapter.sign(null,jwtDetails.getSignatureKey(), jwtDetails.getSignature());
        //encrypt
        adapter.encrypt(null, jwtDetails.getAlgorithmKey(), jwtDetails.getAlgorithm());
        
        return adapter.authorize(modelAndView);
    }

    @Operation(summary = "JWT JWK元数据接口", description = "参数mxk_metadata_APPID")
    @GetMapping(value = "/metadata/jwt/" + WebConstants.MXK_METADATA_PREFIX + "{appid}.{mediaType}")
    @ResponseBody
    public String  metadata(HttpServletRequest request,
            HttpServletResponse response, 
            @PathVariable("appid") String appId, 
            @PathVariable String mediaType) {
        AppsJwtDetails jwtDetails = jwtDetailsService.get(appId , true);
        if(jwtDetails != null) {
            String jwkSetString = "";
            if(!"none".equalsIgnoreCase(jwtDetails.getSignature())) {
                jwkSetString = jwtDetails.getSignatureKey();
            }
            if(!"none".equalsIgnoreCase(jwtDetails.getAlgorithm())) {
                if(StringUtils.isBlank(jwkSetString)) {
                    jwkSetString = jwtDetails.getAlgorithmKey();
                }else {
                    jwkSetString = jwkSetString + "," +jwtDetails.getAlgorithmKey();
                }
            }
             
            JWKSetKeyStore jwkSetKeyStore = new JWKSetKeyStore("{\"keys\": [" + jwkSetString + "]}");
            if(StringUtils.isNotBlank(mediaType) 
                    && "xml".equalsIgnoreCase(mediaType)) {
                response.setContentType(ContentType.APPLICATION_XML_UTF8);
            }else {
                response.setContentType(ContentType.APPLICATION_JSON_UTF8);
            }
            return jwkSetKeyStore.toString(mediaType);
            
        }
        return appId + " not exist. \n" + WebContext.version();
    }
}
