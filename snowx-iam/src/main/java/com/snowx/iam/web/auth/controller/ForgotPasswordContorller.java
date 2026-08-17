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

import org.apache.commons.lang3.StringUtils;
import com.snowx.iam.authn.jwt.AuthTokenService;
import com.snowx.iam.configuration.EmailConfig;
import com.snowx.iam.constants.ConstsAct;
import com.snowx.iam.constants.ConstsActResult;
import com.snowx.iam.constants.ConstsEntryType;
import com.snowx.iam.constants.ConstsRegex;
import com.snowx.iam.entity.ChangePassword;
import com.snowx.iam.entity.Message;
import com.snowx.iam.entity.cnf.CnfPasswordPolicy;
import com.snowx.iam.entity.idm.UserInfo;
import com.snowx.iam.password.onetimepwd.AbstractOtpAuthn;
import com.snowx.iam.password.onetimepwd.MailOtpAuthnService;
import com.snowx.iam.password.sms.SmsOtpAuthnService;
import com.snowx.iam.persistence.service.CnfPasswordPolicyService;
import com.snowx.iam.persistence.service.HistorySystemLogsService;
import com.snowx.iam.persistence.service.UserInfoService;
import com.snowx.iam.web.WebContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = { "/forgotpassword" })
public class ForgotPasswordContorller {
    private static Logger logger = LoggerFactory.getLogger(ForgotPasswordContorller.class);

    @Autowired
    EmailConfig emailConfig;
    
    public class ForgotType{
        public static final  int NOTFOUND             = 1;
        public static final  int EMAIL                 = 2;
        public static final  int MOBILE             = 3;
        public static final  int CAPTCHAERROR         = 4;
    }
    
    public class PasswordResetResult{
        public static final  int SUCCESS             = 1;
        public static final  int CAPTCHAERROR         = 2;
        public static final  int PASSWORDERROR         = 3;
    }
    
    @Autowired
    AuthTokenService authTokenService;
    
    @Autowired
    UserInfoService userInfoService;
    
    @Autowired
    MailOtpAuthnService mailOtpAuthnService;
    
    @Autowired
    SmsOtpAuthnService smsOtpAuthnService;

    @Autowired
    HistorySystemLogsService historySystemLogsService;

    @Autowired
    CnfPasswordPolicyService passwordPolicyService;

    @GetMapping(value={"/passwordpolicy"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    public Message<CnfPasswordPolicy> passwordpolicy(){
        CnfPasswordPolicy passwordPolicy = passwordPolicyService.get(WebContext.getInst().getId());
        //构建密码强度说明
        passwordPolicyService.buildTipMessage(passwordPolicy);
        return new Message<>(passwordPolicy);
    }

    @GetMapping(value = { "/validateCaptcha" }, produces = {MediaType.APPLICATION_JSON_VALUE})
    public Message<ChangePassword> validateCaptcha(
            @RequestParam(required = false, defaultValue = "mobile") String forgotType,
            @RequestParam String userId,
            @RequestParam String state,
            @RequestParam String captcha,
            @RequestParam String otpCaptcha) {
        logger.debug("forgotpassword  /forgotpassword/validateCaptcha.");
        logger.debug(" userId {}: " ,userId);
        UserInfo userInfo = userInfoService.get(userId);
        if(userInfo != null) {
            AbstractOtpAuthn otpAuthn = "email".equalsIgnoreCase(forgotType)
                    ? mailOtpAuthnService.getMailOtpAuthn(userInfo.getInstId())
                    : smsOtpAuthnService.getByInstId(userInfo.getInstId());
            if (otpCaptcha == null || otpAuthn == null || !otpAuthn.validate(userInfo, otpCaptcha)) {
                return new Message<>(Message.FAIL);
            }
            return new Message<>(Message.SUCCESS);
        }
        return new Message<>(Message.FAIL);
    }

    @GetMapping(value = { "/produceOtp" }, produces = {MediaType.APPLICATION_JSON_VALUE})
    public Message<ChangePassword> produceOtp(
                @RequestParam String mobile,
                @RequestParam String state,
                @RequestParam String captcha) {
        logger.debug("forgotpassword  /forgotpassword/produceOtp.");
        logger.debug(" Mobile {}: " ,mobile);
        if (!authTokenService.validateCaptcha(state,captcha)) {    
            logger.debug("login captcha valid error.");
            return new Message<>(Message.FAIL);
        }
        
        ChangePassword change = null;
        logger.debug("Mobile Regex matches {}",ConstsRegex.MOBILE_PATTERN.matcher(mobile).matches());
        if(StringUtils.isNotBlank(mobile) && ConstsRegex.MOBILE_PATTERN.matcher(mobile).matches()) {
            UserInfo userInfo = userInfoService.findByEmailMobile(mobile);
            if(userInfo != null) {
                change = new ChangePassword(userInfo);
                change.clearPassword();
                AbstractOtpAuthn smsOtpAuthn = smsOtpAuthnService.getByInstId(userInfo.getInstId());
                smsOtpAuthn.produce(userInfo);
                return new Message<>(change);
            }
        }
            
        return new Message<>(Message.FAIL);
    }
    
    @GetMapping(value = { "/produceEmailOtp" }, produces = {MediaType.APPLICATION_JSON_VALUE})
    public Message<ChangePassword> produceEmailOtp(
                @RequestParam String email,
                @RequestParam String state,
                @RequestParam String captcha) {
        logger.debug("/forgotpassword/produceEmailOtp Email {} : " , email);
        if (!authTokenService.validateCaptcha(state,captcha)) {
            logger.debug("captcha valid error.");
            return new Message<>(Message.FAIL);
        }
        
        ChangePassword change = null;
        if(StringUtils.isNotBlank(email) && ConstsRegex.EMAIL_PATTERN.matcher(email).matches()) {
            UserInfo userInfo = userInfoService.findByEmailMobile(email);
            if(userInfo != null) {
                change = new ChangePassword(userInfo);
                change.clearPassword();
                AbstractOtpAuthn mailOtpAuthn =  mailOtpAuthnService.getMailOtpAuthn(userInfo.getInstId());
                mailOtpAuthn.produce(userInfo);
                return new Message<>(change);
            }
        }
        return new Message<>(Message.FAIL);
    }

    @GetMapping({ "/setpassword" })
    public Message<ChangePassword> setPassWord(
                        @ModelAttribute ChangePassword changePassword,
                        @RequestParam String forgotType,
                        @RequestParam String otpCaptcha,
                        @RequestParam String state) {
        logger.debug("forgotPassword  /forgotpassword/setpassword.");
        if (StringUtils.isNotBlank(changePassword.getPassword() )
                && changePassword.getPassword().equals(changePassword.getConfirmPassword())) {
            UserInfo loadedUserInfo = userInfoService.get(changePassword.getUserId());
            if(loadedUserInfo != null) {
                AbstractOtpAuthn smsOtpAuthn = smsOtpAuthnService.getByInstId(loadedUserInfo.getInstId());
                AbstractOtpAuthn mailOtpAuthn =  mailOtpAuthnService.getMailOtpAuthn(loadedUserInfo.getInstId());
                if (
                        ("email".equalsIgnoreCase(forgotType)
                                && mailOtpAuthn !=null 
                                && mailOtpAuthn.validate(loadedUserInfo, otpCaptcha)) 
                        ||
                        ("mobile".equalsIgnoreCase(forgotType)
                                && smsOtpAuthn !=null 
                                && smsOtpAuthn.validate(loadedUserInfo, otpCaptcha))
                   ) {
                    
                    if(userInfoService.changePassword(changePassword,true)) {
                        historySystemLogsService.insert(
                                ConstsEntryType.USERINFO,
                                changePassword,
                                ConstsAct.FORGOT_PASSWORD,
                                ConstsActResult.SUCCESS,
                                loadedUserInfo);
                        return new Message<>(Message.SUCCESS);
                    }else {
                        return new Message<>(Message.FAIL);
                    }
                } else {
                    return new Message<>(Message.FAIL);
                }
            } 
        }
        return new Message<>(Message.FAIL);
    }
}
