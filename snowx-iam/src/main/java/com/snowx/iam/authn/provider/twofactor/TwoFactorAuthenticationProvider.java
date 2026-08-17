/*
 * Copyright [2025] [MaxKey of copyright http://www.maxkey.top]
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
 



 

package com.snowx.iam.authn.provider.twofactor;

import java.util.HashMap;
import java.util.Map;

import com.snowx.iam.authn.LoginCredential;
import com.snowx.iam.authn.SignPrincipal;
import com.snowx.iam.authn.jwt.AuthTokenService;
import com.snowx.iam.authn.provider.AbstractAuthenticationProvider;
import com.snowx.iam.authn.realm.AbstractAuthenticationRealm;
import com.snowx.iam.authn.session.Session;
import com.snowx.iam.authn.session.SessionManager;
import com.snowx.iam.authn.web.AuthorizationUtils;
import com.snowx.iam.constants.ConstsJwt;
import com.snowx.iam.constants.ConstsLoginType;
import com.snowx.iam.constants.ConstsTwoFactor;
import com.snowx.iam.entity.idm.UserInfo;
import com.snowx.iam.persistence.service.LoginService;
import com.snowx.iam.web.WebConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import com.nimbusds.jwt.JWTClaimsSet;

/**
 * TwoFactor Authentication provider.双因素认证提供者
 * 
 * @author Crystal.Sea
 *
 */
public class TwoFactorAuthenticationProvider extends AbstractAuthenticationProvider {
    private static final Logger logger =LoggerFactory.getLogger(TwoFactorAuthenticationProvider.class);

    Map<String,AbstractAuthenticationProvider> twoFactorProvider = new HashMap<>();
    
    @Override
    public String getProviderName() {
        return "twoFactor" + PROVIDER_SUFFIX;
    }
    
    public TwoFactorAuthenticationProvider(
            AbstractAuthenticationRealm authenticationRealm,
            SessionManager sessionManager,
            LoginService loginService,
            AuthTokenService authTokenService) {
        this.authenticationRealm = authenticationRealm;
        this.sessionManager = sessionManager;
        this.authTokenService = authTokenService;
    }
    
    public void addProvider(int twoFactor,AbstractAuthenticationProvider provider) {
        twoFactorProvider.put(twoFactor+"", provider);
    }

    @Override
    public Authentication doAuthenticate(LoginCredential credential) {
        logger.debug("Credential {}" , credential);
        emptyOtpCaptchaValid(credential.getOtpCaptcha());
        try {
            if(authTokenService.validateJwtToken(credential.getJwtToken())) {
                 //解析refreshToken，转换会话id
                 JWTClaimsSet claim = authTokenService.resolve(credential.getJwtToken());
                 String sessionId = claim.getJWTID();
                 String userId = claim.getClaim(ConstsJwt.USER_ID).toString();
                 //String style = claim.getClaim(AuthorizationUtils.STYLE).toString();
                 //尝试刷新会话
                 logger.trace("Try to  get user {} , sessionId [{}]" , userId, sessionId);
                 Session session  = sessionManager.getTwoFactor(sessionId);
                 if(session != null) {//有会话
                     Authentication twoFactorAuth  = null;
                     SignPrincipal principal =(SignPrincipal)  session.getAuthentication().getPrincipal();
                     String loginType;
                     switch(principal.getTwoFactor()) {
                         case ConstsTwoFactor.TOTP -> {
                             loginType = ConstsLoginType.TwoFactor.TWO_FACTOR_TOTP;
                         }
                         case ConstsTwoFactor.EMAIL -> {
                             loginType = ConstsLoginType.TwoFactor.TWO_FACTOR_EMAIL;
                         }
                         case ConstsTwoFactor.SMS -> {
                             loginType = ConstsLoginType.TwoFactor.TWO_FACTOR_MOBILE;
                         }
                         default ->{
                             loginType = ConstsLoginType.TwoFactor.TWO_FACTOR_TOTP;
                         }
                     }
                     logger.debug("loginType {}",loginType);
                     AbstractAuthenticationProvider authenticationProvider = twoFactorProvider.get(principal.getTwoFactor()+"");
                     logger.debug("Provider {}",authenticationProvider.getProviderName());
                     UserInfo user = authenticationRealm.loadUserInfoById(userId);
                     //进行二次认证校验
                     twoFactorAuth = authenticationProvider.doTwoFactorAuthenticate(credential , user);

                     if(twoFactorAuth != null) {
                         logger.debug("twoFactorAuth success .");
                         //设置正常状态
                         principal.clearTwoFactor();
                         //重新设置令牌参数
                         sessionManager.create(sessionId, session);
                         sessionManager.removeTwoFactor(sessionId);
                         AuthorizationUtils.setAuthentication(session.getAuthentication());
                         authenticationRealm.insertLoginHistory(user, 
                                 loginType, 
                                "", 
                                "xe00000004",
                                WebConstants.LOGIN_RESULT.SUCCESS);
                         return session.getAuthentication();
                     }else {
                         logger.debug("twoFactorAuth fail .");
                     }
                 }else {//无会话
                     logger.debug("Session is timeout , sessionId [{}]" , sessionId);
                 }
             }else {//验证失效
                 logger.debug("jwt token is not validate .");
             }
        }catch(Exception e) {
            logger.error("Exception !",e);
         }
        
        return  null;
    }
    
}
