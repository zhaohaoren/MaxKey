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
 

package com.snowx.iam.authn.provider.impl;

import com.snowx.iam.authn.LoginCredential;
import com.snowx.iam.authn.jwt.AuthTokenService;
import com.snowx.iam.authn.provider.AbstractAuthenticationProvider;
import com.snowx.iam.authn.realm.AbstractAuthenticationRealm;
import com.snowx.iam.authn.session.SessionManager;
import com.snowx.iam.configuration.ApplicationConfig;
import com.snowx.iam.constants.ConstsLoginType;
import com.snowx.iam.entity.idm.UserInfo;
import com.snowx.iam.web.WebConstants;
import com.snowx.iam.web.WebContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

/**
 * @description:
 * @author: orangeBabu
 * @time: 19/8/2024 PM3:41
 */
public class AppAuthenticationProvider extends AbstractAuthenticationProvider {
    private static final Logger _logger = LoggerFactory.getLogger(AppAuthenticationProvider.class);

    public AppAuthenticationProvider() {
        super();
    }

    public AppAuthenticationProvider(
            AbstractAuthenticationRealm authenticationRealm,
            ApplicationConfig applicationConfig,
            SessionManager sessionManager,
            AuthTokenService authTokenService) {
        this.authenticationRealm = authenticationRealm;
        this.applicationConfig = applicationConfig;
        this.sessionManager = sessionManager;
        this.authTokenService = authTokenService;
    }


    @Override
    public String getProviderName() {
        return "app" + PROVIDER_SUFFIX;
    }

    @Override
    public Authentication doAuthenticate(LoginCredential loginCredential) {
        UsernamePasswordAuthenticationToken authenticationToken = null;
        _logger.debug("Trying to authenticate user '{}' via {}",
                loginCredential.getPrincipal(), getProviderName());
        try {

            _logger.debug("authentication {}", loginCredential);

            if(this.applicationConfig.getLoginConfig().isCaptcha()) {
                captchaValid(loginCredential.getState(),loginCredential.getCaptcha());
            }

            emptyPasswordValid(loginCredential.getPassword());

            emptyUsernameValid(loginCredential.getUsername());

            //查询用户
            UserInfo userInfo = loadUserInfo(loginCredential.getUsername(), loginCredential.getPassword());

            //Validate PasswordPolicy
            authenticationRealm.getLoginService().passwordPolicyValid(userInfo);

            statusValid(loginCredential, userInfo);

            //Match password
            authenticationRealm.passwordMatches(userInfo, loginCredential.getPassword());

            //apply PasswordSetType and resetBadPasswordCount
            authenticationRealm.getLoginService().applyPasswordPolicy(userInfo);

            authenticationToken = createOnlineTicket(loginCredential, userInfo);
            // user authenticated
            _logger.debug("'{}' authenticated successfully by {}.",
                    loginCredential.getPrincipal(), getProviderName());

            authenticationRealm.insertLoginHistory(userInfo,
                    ConstsLoginType.LOCAL,
                    "",
                    "xe00000004",
                    WebConstants.LOGIN_RESULT.SUCCESS);

        } catch (
                AuthenticationException e) {
            _logger.error("Failed to authenticate user {} via {}: {}",
                    loginCredential.getPrincipal(),
                    getProviderName(),
                    e.getMessage());
            WebContext.setAttribute(
                    WebConstants.LOGIN_ERROR_SESSION_MESSAGE, e.getMessage());
        } catch (Exception e) {
            _logger.error("Login error Unexpected exception in {} authentication:\n{}",
                    getProviderName(), e.getMessage());
        }

        return authenticationToken;
    }

    protected void captchaValid(String state ,String captcha) {
        // for basic
        if(!authTokenService.validateCaptcha(state,captcha)) {
            throw new BadCredentialsException(WebContext.getI18nValue("login.error.captcha"));
        }
    }
}
