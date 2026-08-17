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


package com.snowx.iam.web.config;

import com.snowx.iam.authn.provider.AbstractAuthenticationProvider;
import com.snowx.iam.authn.jwt.AuthTokenService;
import com.snowx.iam.authn.session.SessionManager;
import com.snowx.iam.authn.support.basic.BasicEntryPoint;
import com.snowx.iam.authn.support.httpheader.HttpHeaderEntryPoint;
import com.snowx.iam.authn.support.kerberos.HttpKerberosEntryPoint;
import com.snowx.iam.authn.support.kerberos.KerberosService;
import com.snowx.iam.authn.web.interceptor.PermissionInterceptor;
import com.snowx.iam.configuration.ApplicationConfig;
import com.snowx.iam.web.auth.interceptor.HistorySingleSignOnInterceptor;
import com.snowx.iam.web.auth.interceptor.SingleSignOnInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@EnableWebMvc
@Configuration
public class MaxKeyMvcConfig implements WebMvcConfigurer {
    private static final Logger logger = LoggerFactory.getLogger(MaxKeyMvcConfig.class);

    @Value("${maxkey.login.basic.enable:false}")
    private boolean basicEnable;

    @Value("${maxkey.login.httpheader.enable:false}")
    private boolean httpHeaderEnable;

    @Value("${maxkey.login.httpheader.headername:iv-user}")
    private String httpHeaderName;

    @Autowired
    ApplicationConfig applicationConfig;

    @Autowired
    AbstractAuthenticationProvider authenticationProvider;

    @Autowired
    KerberosService kerberosService;

    @Autowired
    PermissionInterceptor permissionInterceptor;

    @Autowired
    SessionManager sessionManager;

    @Autowired
    AuthTokenService authTokenService;

    @Autowired
    SingleSignOnInterceptor singleSignOnInterceptor;

    @Autowired
    HistorySingleSignOnInterceptor historySingleSignOnInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //addPathPatterns 用于添加拦截规则 ， 先把所有路径都加入拦截， 再一个个排除
        //excludePathPatterns 表示改路径不用拦截

        logger.debug("add Http Kerberos Entry Point");
        registry.addInterceptor(new HttpKerberosEntryPoint(
                        authenticationProvider, kerberosService, applicationConfig, true))
                .addPathPatterns("/login");


        if (httpHeaderEnable) {
            registry.addInterceptor(new HttpHeaderEntryPoint(httpHeaderName, httpHeaderEnable))
                    .addPathPatterns("/*");
            logger.debug("add Http Header Entry Point");
        }

        if (basicEnable) {
            registry.addInterceptor(new BasicEntryPoint(basicEnable))
                    .addPathPatterns("/*");
            logger.debug("add Basic Entry Point");
        }

        //for frontend
        registry.addInterceptor(permissionInterceptor)
                .addPathPatterns("/config/**")
                .addPathPatterns("/users/**")
                .addPathPatterns("/historys/**")
                .addPathPatterns("/access/session/**")
                .addPathPatterns("/access/session/**/**")
                .addPathPatterns("/appList")
                .addPathPatterns("/appList/**")
                .addPathPatterns("/socialsignon/**")
                .addPathPatterns("/authz/credential/**")
                .addPathPatterns("/authz/oauth/v20/approval_confirm/**")
                .addPathPatterns("/authz/oauth/v20/authorize/approval/**")
                .addPathPatterns("/logon/oauth20/bind/**")
                .addPathPatterns("/logout")
                .addPathPatterns("/logout/**")
                .addPathPatterns("/passkey/registration/**")
                .addPathPatterns("/passkey/registration/**/**")
                .addPathPatterns("/passkey/registration/**/**/**")
                .addPathPatterns("/authz/refused")
                .excludePathPatterns("/logon/oauth20/**/**")
                .excludePathPatterns("/swagger-ui/**")
                .excludePathPatterns("/swagger-resources/**")
                .excludePathPatterns("/v3/api-docs/**")
        ;

        logger.debug("add Permission Interceptor");

        //for Single Sign On
        registry.addInterceptor(singleSignOnInterceptor)
                .addPathPatterns("/authz/basic/*")
                //Form based
                .addPathPatterns("/authz/formbased/*")
                //Token based
                .addPathPatterns("/authz/tokenbased/*")
                //JWT
                .addPathPatterns("/authz/jwt/*")
                //SAML
                .addPathPatterns("/authz/saml20/idpinit/*")
                .addPathPatterns("/authz/saml20/assertion")
                .addPathPatterns("/authz/saml20/assertion/")
                //CAS
                .addPathPatterns("/authz/cas/*")
                .addPathPatterns("/authz/cas/*/*")
                .addPathPatterns("/authz/cas/login")
                .addPathPatterns("/authz/cas/login/")
                .addPathPatterns("/authz/cas/granting/*")
                //cas1.0 validate
                .excludePathPatterns("/authz/cas/validate")
                //cas2.0 Validate
                .excludePathPatterns("/authz/cas/serviceValidate")
                .excludePathPatterns("/authz/cas/proxyValidate")
                .excludePathPatterns("/authz/cas/proxy")
                //cas3.0 Validate
                .excludePathPatterns("/authz/cas/p3/serviceValidate")
                .excludePathPatterns("/authz/cas/p3/proxyValidate")
                .excludePathPatterns("/authz/cas/p3/proxy")
                //rest
                .excludePathPatterns("/authz/cas/v1/tickets")
                .excludePathPatterns("/authz/cas/v1/tickets/*")
                .excludePathPatterns("/authz/cas/v1/users")

                //OAuth
                .addPathPatterns("/authz/oauth/v20/authorize")
                .addPathPatterns("/authz/oauth/v20/authorize/*")

                //OAuth TENCENT_IOA
                .addPathPatterns("/oauth2/authorize")
                .addPathPatterns("/oauth2/authorize/*")

                //online ticket Validate
                .excludePathPatterns("/onlineticket/ticketValidate")
                .excludePathPatterns("/onlineticket/ticketValidate/*")
        ;
        logger.debug("add Single SignOn Interceptor");

        registry.addInterceptor(historySingleSignOnInterceptor)
                .addPathPatterns("/authz/basic/*")
                .addPathPatterns("/authz/ltpa/*")
                //Extend api
                .addPathPatterns("/authz/api/*")
                //Form based
                .addPathPatterns("/authz/formbased/*")
                //Token based
                .addPathPatterns("/authz/tokenbased/*")
                //JWT
                .addPathPatterns("/authz/jwt/*")
                //SAML
                .addPathPatterns("/authz/saml20/idpinit/*")
                .addPathPatterns("/authz/saml20/assertion")
                //CAS
                .addPathPatterns("/authz/cas/granting")
                //OAuth
                .addPathPatterns("/authz/oauth/v20/approval_confirm")
        ;
        logger.debug("add history SignOn App Interceptor");

        //for mgt
        //addPathPatterns 用于添加拦截规则 ， 先把所有路径都加入拦截， 再一个个排除
        //excludePathPatterns 表示改路径不用拦截
        logger.debug("add Interceptors");

        PermissionInterceptor managementPermissionInterceptor = new PermissionInterceptor(
                applicationConfig, sessionManager, authTokenService, true);
        registry.addInterceptor(managementPermissionInterceptor)
                .addPathPatterns("/admin/**")
                .addPathPatterns("/file/upload/**")
                .excludePathPatterns("/admin/login/**")
        ;

        logger.debug("add Permission Adapter");

    }

}
