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


package com.snowx.iam.web.config;

import com.snowx.iam.authn.listener.SessionListenerAdapter;
import com.snowx.iam.authn.realm.jdbc.JdbcAuthenticationRealm;
import com.snowx.iam.authn.realm.ldap.LdapAuthenticationRealmService;
import com.snowx.iam.authn.session.SessionCategory;
import com.snowx.iam.authn.session.SessionManager;
import com.snowx.iam.authn.support.kerberos.KerberosProxy;
import com.snowx.iam.authn.support.kerberos.RemoteKerberosService;
import com.snowx.iam.configuration.ApplicationConfig;
import com.snowx.iam.ip2location.IpLocationParser;
import com.snowx.iam.password.onetimepwd.MailOtpAuthnService;
import com.snowx.iam.persistence.service.*;
import com.snowx.iam.schedule.ScheduleAdapterBuilder;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class MaxKeyConfig {
    private static final Logger logger = LoggerFactory.getLogger(MaxKeyConfig.class);

    //可以在此实现其他的登陆认证方式，请实现AbstractAuthenticationRealm
    @Bean
    JdbcAuthenticationRealm authenticationRealm(
            @Qualifier("passwordEncoder") PasswordEncoder passwordEncoder,
            PasswordPolicyValidatorService passwordPolicyValidatorService,
            LoginService loginService,
            HistoryLoginService historyLoginService,
            UserInfoService userInfoService,
            IpLocationParser ipLocationParser,
            JdbcTemplate jdbcTemplate,
            MailOtpAuthnService otpAuthnService,
            CnfLdapContextService ldapContextService) {
        LdapAuthenticationRealmService ldapRealmService = new LdapAuthenticationRealmService(ldapContextService);
        return new JdbcAuthenticationRealm(
                passwordEncoder,
                passwordPolicyValidatorService,
                loginService,
                historyLoginService,
                userInfoService,
                ipLocationParser,
                jdbcTemplate,
                ldapRealmService
        );
    }

    @Bean
    RemoteKerberosService kerberosService(
            @Value("${maxkey.login.kerberos.default.userdomain}")
            String userDomain,
            @Value("${maxkey.login.kerberos.default.fulluserdomain}")
            String fullUserDomain,
            @Value("${maxkey.login.kerberos.default.crypto}")
            String crypto,
            @Value("${maxkey.login.kerberos.default.redirecturi}")
            String redirectUri
    ) {
        RemoteKerberosService kerberosService = new RemoteKerberosService();
        KerberosProxy kerberosProxy = new KerberosProxy();

        kerberosProxy.setCrypto(crypto);
        kerberosProxy.setFullUserdomain(fullUserDomain);
        kerberosProxy.setUserdomain(userDomain);
        kerberosProxy.setRedirectUri(redirectUri);

        List<KerberosProxy> kerberosProxysList = new ArrayList<>();
        kerberosProxysList.add(kerberosProxy);
        kerberosService.setKerberosProxys(kerberosProxysList);

        logger.debug("RemoteKerberosService inited.");
        return kerberosService;
    }

    @Bean
    String signSessionListenerAdapter(
            Scheduler scheduler,
            ApplicationConfig applicationConfig,
            SessionManager sessionManager) throws SchedulerException {
        if (applicationConfig.isPersistenceInmemory()) {
            new ScheduleAdapterBuilder()
                    .setScheduler(scheduler)
                    .setIdentity("SignSessionListenerAdapter")
                    .setCron("0 0/10 * * * ?")
                    .setJobClass(SessionListenerAdapter.class)
                    .setJobData("sessionManager", sessionManager)
                    .setJobData("category", SessionCategory.SIGN)
                    .build();
            logger.debug("Session ListenerAdapter inited .");
        }
        return "signSessionListenerAdapter";
    }

}
