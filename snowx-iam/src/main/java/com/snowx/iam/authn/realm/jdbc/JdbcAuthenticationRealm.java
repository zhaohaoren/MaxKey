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
 

package com.snowx.iam.authn.realm.jdbc;

import com.snowx.iam.authn.realm.AbstractAuthenticationRealm;
import com.snowx.iam.authn.realm.ldap.LdapAuthenticationRealm;
import com.snowx.iam.authn.realm.ldap.LdapAuthenticationRealmService;
import com.snowx.iam.constants.ConstsLoginType;
import com.snowx.iam.constants.ConstsStatus;
import com.snowx.iam.entity.ChangePassword;
import com.snowx.iam.entity.cnf.CnfPasswordPolicy;
import com.snowx.iam.entity.idm.UserInfo;
import com.snowx.iam.ip2location.IpLocationParser;
import com.snowx.iam.persistence.service.HistoryLoginService;
import com.snowx.iam.persistence.service.LoginService;
import com.snowx.iam.persistence.service.PasswordPolicyValidatorService;
import com.snowx.iam.persistence.service.UserInfoService;
import com.snowx.iam.web.WebConstants;
import com.snowx.iam.web.WebContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * JdbcAuthenticationRealm.
 * @author Crystal.Sea
 *
 */
public class JdbcAuthenticationRealm extends AbstractAuthenticationRealm {
    private static final Logger  _logger = LoggerFactory.getLogger(JdbcAuthenticationRealm.class);

    protected PasswordEncoder passwordEncoder;
    
    public JdbcAuthenticationRealm() {
        _logger.debug("init . ");
    }

    public JdbcAuthenticationRealm(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    public JdbcAuthenticationRealm(
            PasswordEncoder passwordEncoder,
            PasswordPolicyValidatorService passwordPolicyValidatorService,
            LoginService loginService,
            HistoryLoginService historyLoginService,
            UserInfoService userInfoService,
            IpLocationParser ipLocationParser,
            JdbcTemplate jdbcTemplate) {
        
        this.passwordEncoder =passwordEncoder;
        this.passwordPolicyValidatorService=passwordPolicyValidatorService;
        this.loginService = loginService;
        this.historyLoginService = historyLoginService;
        this.userInfoService = userInfoService;
        this.ipLocationParser = ipLocationParser;
        this.jdbcTemplate = jdbcTemplate;
    }
  
    public JdbcAuthenticationRealm(
            PasswordEncoder passwordEncoder,
            PasswordPolicyValidatorService passwordPolicyValidatorService,
            LoginService loginService,
            HistoryLoginService historyLoginService,
            UserInfoService userInfoService,
            IpLocationParser ipLocationParser,
            JdbcTemplate jdbcTemplate,
            LdapAuthenticationRealmService ldapAuthenticationRealmService) {
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicyValidatorService = passwordPolicyValidatorService;
        this.loginService = loginService;
        this.historyLoginService = historyLoginService;
        this.userInfoService = userInfoService;
        this.ipLocationParser = ipLocationParser;
        this.jdbcTemplate = jdbcTemplate;
        this.ldapAuthenticationRealmService = ldapAuthenticationRealmService;
    }
    
    /**
     * passwordMatches.
     */
    @Override
    public boolean passwordMatches(UserInfo userInfo, String password) {
        boolean passwordMatches = false;
        //jdbc password check
        //_logger.trace("password : " 
        //        + PasswordReciprocal.getInstance().rawPassword(userInfo.getUsername(), password));
        passwordMatches = passwordEncoder.matches(password,userInfo.getPassword());
        
        if(ldapAuthenticationRealmService != null) {
            //passwordMatches == false and ldapSupport ==true
            //validate password with LDAP
            try {
                LdapAuthenticationRealm ldapRealm = ldapAuthenticationRealmService.getByInstId(userInfo.getInstId());
                if(!passwordMatches && ldapRealm != null 
                        && ldapRealm.isLdapSupport() 
                        && userInfo.getIsLocked() == ConstsStatus.ACTIVE) {
                    passwordMatches = ldapRealm.passwordMatches(userInfo, password);
                    if(passwordMatches) {
                        //write password to database Realm
                        ChangePassword changePassword = new ChangePassword(userInfo);
                        changePassword.setPassword(password);
                        userInfoService.changePassword(changePassword, false);
                    }
                }
            }catch(Exception e) {
                _logger.debug("passwordvalid Exception : {}" , e);
            }
        }
        _logger.debug("passwordvalid : {}" , passwordMatches);
        if (!passwordMatches) {
            loginService.plusBadPasswordCount(userInfo);
            insertLoginHistory(userInfo, ConstsLoginType.LOCAL, "", "xe00000004", WebConstants.LOGIN_RESULT.PASSWORD_ERROE);
            CnfPasswordPolicy passwordPolicy = passwordPolicyValidatorService.getPasswordPolicy();
            if(userInfo.getBadPasswordCount()>=(passwordPolicy.getAttempts()/2)) {
                throw new BadCredentialsException(
                        WebContext.getI18nValue("login.error.password.attempts",
                                new Object[]{
                                        userInfo.getBadPasswordCount() + 1,
                                        passwordPolicy.getAttempts(),
                                        passwordPolicy.getDuration()}));
            }else {
                throw new BadCredentialsException(WebContext.getI18nValue("login.error.password"));
            }
        }
        return passwordMatches;
    }

}
