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


package com.snowx.iam.web.auth.controller;

import com.nimbusds.jwt.JWTClaimsSet;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import com.snowx.iam.authn.LoginCredential;
import com.snowx.iam.authn.SignPrincipal;
import com.snowx.iam.authn.jwt.AuthJwt;
import com.snowx.iam.authn.jwt.AuthTokenService;
import com.snowx.iam.authn.provider.AbstractAuthenticationProvider;
import com.snowx.iam.authn.session.SessionManager;
import com.snowx.iam.authn.session.Session;
import com.snowx.iam.authn.support.kerberos.KerberosService;
import com.snowx.iam.authn.support.rememberme.AbstractRemeberMeManager;
import com.snowx.iam.authn.support.rememberme.RemeberMe;
import com.snowx.iam.authn.support.socialsignon.service.SocialSignOnProviderService;
import com.snowx.iam.configuration.ApplicationConfig;
import com.snowx.iam.constants.ConstsLoginType;
import com.snowx.iam.constants.ConstsTwoFactor;
import com.snowx.iam.entity.Institutions;
import com.snowx.iam.entity.Message;
import com.snowx.iam.entity.SocialsAssociate;
import com.snowx.iam.entity.SocialsProvider;
import com.snowx.iam.entity.idm.UserInfo;
import com.snowx.iam.passkey.config.PasskeyProperties;
import com.snowx.iam.password.onetimepwd.AbstractOtpAuthn;
import com.snowx.iam.password.onetimepwd.MailOtpAuthnService;
import com.snowx.iam.password.sms.SmsOtpAuthnService;
import com.snowx.iam.persistence.service.SocialsAssociatesService;
import com.snowx.iam.persistence.service.UserInfoService;
import com.snowx.iam.web.WebConstants;
import com.snowx.iam.web.WebContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.util.HashMap;

/**
 * @author Crystal.Sea
 *
 */
@Tag(name = "1-1-登录接口文档模块")
@RestController
@RequestMapping(value = "/login")
public class LoginEntryPoint {
    private static Logger logger = LoggerFactory.getLogger(LoginEntryPoint.class);

    @Autowired
    AuthTokenService authTokenService;

    @Autowired
      ApplicationConfig applicationConfig;
    
    @Autowired
    PasskeyProperties passkeyProperties;

    @Autowired
    AbstractAuthenticationProvider authenticationProvider ;

    @Autowired
    SocialSignOnProviderService socialSignOnProviderService;

    @Autowired
    SocialsAssociatesService socialsAssociatesService;

    @Autowired
    KerberosService kerberosService;

    @Autowired
    UserInfoService userInfoService;

    @Autowired
    AbstractOtpAuthn tfaOtpAuthn;

    @Autowired
    SmsOtpAuthnService smsAuthnService;

    @Autowired
    MailOtpAuthnService mailOtpAuthnService;

    @Autowired
    AbstractRemeberMeManager remeberMeManager;

    @Autowired
    SessionManager sessionManager;

    /**
     * init login
     * @return
     */
    @Operation(summary  = "登录接口", description  = "用户登录地址",method="GET")
    @GetMapping(value={"/get"})
    public Message<?> get(@RequestParam(value = "remember_me", required = false) String rememberMeJwt) {
        logger.debug("/get.");
        //Remember Me
        if(authTokenService.validateJwtToken(rememberMeJwt)) {
            try {
                RemeberMe remeberMe = remeberMeManager.resolve(rememberMeJwt);
                if(remeberMe != null) {
                    LoginCredential credential = new LoginCredential();
                    String remeberMeJwt = remeberMeManager.updateRemeberMe(remeberMe);
                    credential.setUsername(remeberMe.getUsername());
                    Authentication  authentication = authenticationProvider.authenticate(credential,true);
                    if(authentication != null) {
                         AuthJwt authJwt = authTokenService.genAuthJwt(authentication);
                         authJwt.setRemeberMe(remeberMeJwt);
                         return new Message<AuthJwt>(authJwt);
                    }
                }
            } catch (ParseException e) {
            }
        }
        //for normal login
        HashMap<String , Object> model = new HashMap<>();
        model.put("isRemeberMe", applicationConfig.getLoginConfig().isRemeberMe());
        model.put("isKerberos", applicationConfig.getLoginConfig().isKerberos());
        if(applicationConfig.getLoginConfig().isMfa()) {
            model.put("otpType", tfaOtpAuthn.getOtpType());
            model.put("otpInterval", tfaOtpAuthn.getInterval());
        }
        model.put("passkeyEnabled", passkeyProperties.isEnabled());
        model.put("passkeyAllowedOrigins", passkeyProperties.getRelyingParty().getAllowedOrigins());

        if( applicationConfig.getLoginConfig().isKerberos()){
            model.put("userDomainUrlJson", kerberosService.buildKerberosProxys());
        }

        Institutions inst = (Institutions)WebContext.getAttribute(WebConstants.CURRENT_INST);
        model.put("inst", inst);
        if(applicationConfig.getLoginConfig().isCaptcha()) {
            model.put("captcha", applicationConfig.getLoginConfig().getCaptchaType());
        }else {
            model.put("captcha", "NONE");
        }
        model.put("state", authTokenService.genRandomJwt());
        //load Social Sign On Providers
        model.put("socials", socialSignOnProviderService.loadSocials(inst.getId()));

        return new Message<HashMap<String , Object>>(model);
    }


     @GetMapping(value={"/sendotp/{mobile}"}, produces = {MediaType.APPLICATION_JSON_VALUE})
     public Message<AuthJwt> produceOtp(@PathVariable String mobile) {
        UserInfo userInfo=userInfoService.findByEmailMobile(mobile);
        if(userInfo != null) {
            smsAuthnService.getByInstId(WebContext.getInst().getId()).produce(userInfo);
            return new Message<AuthJwt>(Message.SUCCESS);
        }

        return new Message<AuthJwt>(Message.FAIL);
    }

    @PostMapping(value={"/sendTwoFactorCode"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    public Message<?> sendTwoFactorCode(@RequestBody LoginCredential credential) {
        try {
            if (!authTokenService.validateJwtToken(credential.getJwtToken())) {
                return new Message<>(Message.FAIL, "二次认证令牌已失效");
            }
            JWTClaimsSet claims = authTokenService.resolve(credential.getJwtToken());
            Session session = sessionManager.getTwoFactor(claims.getJWTID());
            if (session == null || session.getAuthentication() == null) {
                return new Message<>(Message.FAIL, "二次认证会话已失效");
            }
            SignPrincipal principal = (SignPrincipal) session.getAuthentication().getPrincipal();
            UserInfo userInfo = principal.getUserInfo();
            if (principal.getTwoFactor() == ConstsTwoFactor.EMAIL) {
                AbstractOtpAuthn mailOtpAuthn = mailOtpAuthnService.getMailOtpAuthn(userInfo.getInstId());
                if (mailOtpAuthn == null) {
                    return new Message<>(Message.FAIL, "邮件验证码服务未配置");
                }
                mailOtpAuthn.produce(userInfo);
                return new Message<>(Message.SUCCESS);
            }
            if (principal.getTwoFactor() == ConstsTwoFactor.SMS) {
                AbstractOtpAuthn smsOtpAuthn = smsAuthnService.getByInstId(userInfo.getInstId());
                if (smsOtpAuthn == null) {
                    return new Message<>(Message.FAIL, "短信验证码服务未配置");
                }
                smsOtpAuthn.produce(userInfo);
                return new Message<>(Message.SUCCESS);
            }
            return new Message<>(Message.FAIL, "当前二次认证类型无需发送验证码");
        } catch (Exception e) {
            logger.error("send two factor code failed", e);
            return new Message<>(Message.FAIL, "二次认证验证码发送失败");
        }
    }

    @PostMapping(value={"/signin/bindusersocials"})
    public Message<AuthJwt> bindusersocials(@RequestBody LoginCredential credential) {
        //短信验证码
        String code = credential.getCode();
        //映射社交服务的账号
        String username = credential.getUsername();
        //maxkey存储的手机号
        String mobile = credential.getMobile();
        //社交服务类型
        String authType = credential.getAuthType();

        UserInfo userInfo = userInfoService.findByEmailMobile(mobile);
        //验证码验证是否合法
        if (smsAuthnService.getByInstId(WebContext.getInst().getId()).validate(userInfo,code)) {
            //合法进行用户绑定
            SocialsAssociate socialsAssociate = new SocialsAssociate();
            socialsAssociate.setUserId(userInfo.getId());
            socialsAssociate.setUsername(userInfo.getUsername());
            socialsAssociate.setProvider(authType);
            socialsAssociate.setSocialUserId(username);
            socialsAssociate.setInstId(userInfo.getInstId());
            //插入Maxkey和社交服务的用户映射表
            socialsAssociatesService.insert(socialsAssociate);

            //设置完成后，进行登录认证
            LoginCredential loginCredential =new LoginCredential(
                    socialsAssociate.getUsername(),"", ConstsLoginType.SOCIALSIGNON);

            SocialsProvider socialSignOnProvider = socialSignOnProviderService.get(socialsAssociate.getInstId(),socialsAssociate.getProvider());

            loginCredential.setProvider(socialSignOnProvider.getProviderName());

            Authentication  authentication = authenticationProvider.authenticate(loginCredential,true);

            return new Message<AuthJwt>(authTokenService.genAuthJwt(authentication));

        }
        return new Message<AuthJwt>(Message.FAIL);
    }


     /**
      * normal
      * @param credential
      * @return
      */
    @Operation(summary = "登录接口", description = "登录接口",method="POST")
     @PostMapping(value={"/signin"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    public Message<AuthJwt> signin( HttpServletRequest request, HttpServletResponse response,@RequestBody LoginCredential credential) {
    	 logger.debug("signin credential {}" , credential);
         Message<AuthJwt> authJwtMessage = new Message<>(Message.FAIL);
         if(authTokenService.validateJwtToken(credential.getState())){
             String authType =  credential.getAuthType();
             if (StringUtils.isNotBlank(authType)){
                 Authentication  authentication = authenticationProvider.authenticate(credential);
                 if(authentication != null) {
                     AuthJwt authJwt = authTokenService.genAuthJwt(authentication);
                     if(StringUtils.isNotBlank(credential.getRemeberMe())
                             && "true".equalsIgnoreCase(credential.getRemeberMe())) {
                         String remeberMe = remeberMeManager.createRemeberMe(authentication, request, response);
                         authJwt.setRemeberMe(remeberMe);
                     }
                     if(WebContext.getAttribute(WebConstants.CURRENT_USER_PASSWORD_SET_TYPE)!=null) {
                         authJwt.setPasswordSetType(
                             (Integer)WebContext.getAttribute(WebConstants.CURRENT_USER_PASSWORD_SET_TYPE));
                     }
                     authJwtMessage = new Message<>(authJwt);

                 }else {//fail
                     String errorMsg = WebContext.getAttribute(WebConstants.LOGIN_ERROR_SESSION_MESSAGE) == null ?
                              "" : WebContext.getAttribute(WebConstants.LOGIN_ERROR_SESSION_MESSAGE).toString();
                     authJwtMessage.setMessage(errorMsg);
                     logger.debug("login fail , message {}",errorMsg);
                 }
             }else {
                 logger.error("Login AuthN type must eq normal , tfa or mobile . ");
             }
         }
         return authJwtMessage;
     }

     /**
      * for congress
      * @param credential
      * @return
      */
     @PostMapping(value={"/congress"})
    public Message<AuthJwt> congress( @RequestBody LoginCredential credential) {
         if(StringUtils.isNotBlank(credential.getCongress())){
             AuthJwt authJwt = authTokenService.consumeCongress(credential.getCongress());
             if(authJwt != null) {
                 return new Message<>(authJwt);
             }
         }
         return new Message<>(Message.FAIL);
     }
}
