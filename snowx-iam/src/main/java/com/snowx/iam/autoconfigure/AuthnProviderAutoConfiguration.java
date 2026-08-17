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


package com.snowx.iam.autoconfigure;

import com.snowx.iam.authn.jwt.AuthTokenService;
import com.snowx.iam.authn.provider.AbstractAuthenticationProvider;
import com.snowx.iam.authn.provider.AuthenticationProviderFactory;
import com.snowx.iam.authn.provider.impl.*;
import com.snowx.iam.authn.provider.twofactor.TwoFactorAuthenticationProvider;
import com.snowx.iam.authn.provider.twofactor.impl.TwoFactorEmailAuthenticationProvider;
import com.snowx.iam.authn.provider.twofactor.impl.TwoFactorMobileAuthenticationProvider;
import com.snowx.iam.authn.provider.twofactor.impl.TwoFactorTotpAuthenticationProvider;
import com.snowx.iam.authn.realm.AbstractAuthenticationRealm;
import com.snowx.iam.authn.session.SessionManager;
import com.snowx.iam.authn.support.rememberme.AbstractRemeberMeManager;
import com.snowx.iam.authn.support.rememberme.JdbcRemeberMeManager;
import com.snowx.iam.configuration.ApplicationConfig;
import com.snowx.iam.constants.ConstsTwoFactor;
import com.snowx.iam.ip2location.IpLocationParser;
import com.snowx.iam.password.onetimepwd.AbstractOtpAuthn;
import com.snowx.iam.password.onetimepwd.MailOtpAuthnService;
import com.snowx.iam.password.sms.SmsOtpAuthnService;
import com.snowx.iam.persistence.service.CnfPasswordPolicyService;
import com.snowx.iam.persistence.service.LoginService;
import com.snowx.iam.persistence.service.PasswordPolicyValidatorService;
import com.snowx.iam.persistence.service.impl.PasswordPolicyValidatorServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;


@AutoConfiguration
public class AuthnProviderAutoConfiguration {
    static final  Logger _logger = LoggerFactory.getLogger(AuthnProviderAutoConfiguration.class);

    @Bean
     AbstractAuthenticationProvider authenticationProvider(
            NormalAuthenticationProvider normalAuthenticationProvider,
            MobileAuthenticationProvider mobileAuthenticationProvider,
            TrustedAuthenticationProvider trustedAuthenticationProvider,
            ScanCodeAuthenticationProvider scanCodeAuthenticationProvider,
            AppAuthenticationProvider appAuthenticationProvider,
            TwoFactorAuthenticationProvider twoFactorAuthenticationProvider
            ) {
        AuthenticationProviderFactory authenticationProvider = new AuthenticationProviderFactory();
        authenticationProvider.addAuthenticationProvider(normalAuthenticationProvider);
        authenticationProvider.addAuthenticationProvider(mobileAuthenticationProvider);
        authenticationProvider.addAuthenticationProvider(trustedAuthenticationProvider);
        authenticationProvider.addAuthenticationProvider(scanCodeAuthenticationProvider);
        authenticationProvider.addAuthenticationProvider(appAuthenticationProvider);

        //二次认证
        authenticationProvider.addAuthenticationProvider(twoFactorAuthenticationProvider);
        
        return authenticationProvider;
    }

    @Bean
    NormalAuthenticationProvider normalAuthenticationProvider(
            AbstractAuthenticationRealm authenticationRealm,
            ApplicationConfig applicationConfig,
            SessionManager sessionManager,
            AuthTokenService authTokenService
            ) {
        _logger.debug("init authentication Provider .");
        return new NormalAuthenticationProvider(
                authenticationRealm,
                applicationConfig,
                sessionManager,
                authTokenService
            );
    }

    @Bean
    ScanCodeAuthenticationProvider scanCodeAuthenticationProvider(
            AbstractAuthenticationRealm authenticationRealm,
            SessionManager sessionManager
    ) {
        return new ScanCodeAuthenticationProvider(
                authenticationRealm,
                sessionManager
        );
    }

    @Bean
    AppAuthenticationProvider appAuthenticationProvider(
            AbstractAuthenticationRealm authenticationRealm,
            ApplicationConfig applicationConfig,
            SessionManager sessionManager,
            AuthTokenService authTokenService
    ) {
        return new AppAuthenticationProvider(
                authenticationRealm,
                applicationConfig,
                sessionManager,
                authTokenService
        );
    }

    @Bean
    MobileAuthenticationProvider mobileAuthenticationProvider(
            AbstractAuthenticationRealm authenticationRealm,
            ApplicationConfig applicationConfig,
            SmsOtpAuthnService smsAuthnService,
            SessionManager sessionManager
            ) {
        _logger.debug("init Mobile authentication Provider .");
        return new MobileAuthenticationProvider(
                authenticationRealm,
                applicationConfig,
                smsAuthnService,
                sessionManager
            );
    }

    @Bean
    TrustedAuthenticationProvider trustedAuthenticationProvider(
            AbstractAuthenticationRealm authenticationRealm,
            ApplicationConfig applicationConfig,
            SessionManager sessionManager
            ) {
        _logger.debug("init Mobile authentication Provider .");
        return new TrustedAuthenticationProvider(
                authenticationRealm,
                applicationConfig,
                sessionManager
            );
    }

    @Bean
    PasswordPolicyValidatorService passwordPolicyValidatorService(
            CnfPasswordPolicyService cnfPasswordPolicyService,
            MessageSource messageSource) {
        return new PasswordPolicyValidatorServiceImpl(cnfPasswordPolicyService,messageSource);
    }

    /**
     * remeberMeService .
     * @return
     */
    @Bean
    AbstractRemeberMeManager remeberMeManager(
            @Value("${maxkey.server.persistence}") int persistence,
            @Value("${maxkey.login.remeberme.validity}") int validity,
            ApplicationConfig applicationConfig,
            AuthTokenService authTokenService,
            JdbcTemplate jdbcTemplate) {
        _logger.trace("init RemeberMeManager , validity {}." , validity);
        return new  JdbcRemeberMeManager(
                jdbcTemplate,applicationConfig,authTokenService,validity);
    }
    
    @Bean
    TwoFactorAuthenticationProvider twoFactorAuthenticationProvider(
            AbstractAuthenticationRealm authenticationRealm,
            SessionManager sessionManager,
            LoginService loginService,
            AuthTokenService authTokenService,
            IpLocationParser ipLocationParser,
            TwoFactorTotpAuthenticationProvider twoFactorTotpAuthenticationProvider,
            TwoFactorMobileAuthenticationProvider twoFactorMobileAuthenticationProvider,
            TwoFactorEmailAuthenticationProvider twoFactorEmailAuthenticationProvider) {
        _logger.debug("init TwoFactor authentication Provider .");
        TwoFactorAuthenticationProvider  twoFactorProvider =new TwoFactorAuthenticationProvider(
                authenticationRealm,
                sessionManager,
                loginService,
                authTokenService
            );
        
        twoFactorProvider.addProvider(ConstsTwoFactor.TOTP, twoFactorTotpAuthenticationProvider);
        twoFactorProvider.addProvider(ConstsTwoFactor.EMAIL, twoFactorEmailAuthenticationProvider);
        twoFactorProvider.addProvider(ConstsTwoFactor.SMS, twoFactorMobileAuthenticationProvider);
        return twoFactorProvider;
    }
    
    @Bean
    TwoFactorTotpAuthenticationProvider twoFactorTotpAuthenticationProvider(@Qualifier("tfaOtpAuthn") AbstractOtpAuthn tfaOtpAuthn,
            AbstractAuthenticationRealm authenticationRealm) {
        _logger.debug("init TwoFactor authentication Provider .");
        return new TwoFactorTotpAuthenticationProvider(authenticationRealm,tfaOtpAuthn);
    }
    
    @Bean
    TwoFactorMobileAuthenticationProvider twoFactorMobileAuthenticationProvider(SmsOtpAuthnService smsOtpAuthnService) {
        _logger.debug("init TwoFactor Mobile authentication Provider .");
        return new TwoFactorMobileAuthenticationProvider(smsOtpAuthnService);
    }
    
    @Bean
    TwoFactorEmailAuthenticationProvider twoFactorEmailAuthenticationProvider(MailOtpAuthnService mailOtpAuthnService) {
        _logger.debug("init TwoFactor Email authentication Provider .");
        return new TwoFactorEmailAuthenticationProvider(mailOtpAuthnService);
    }

}
