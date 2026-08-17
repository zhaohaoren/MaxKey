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
package org.dromara.maxkey.authn.support.socialsignon;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import me.zhyd.oauth.request.AuthMaxkeyRequest;
import org.apache.commons.lang3.StringUtils;
import org.dromara.maxkey.authn.LoginCredential;
import org.dromara.maxkey.authn.annotation.CurrentUser;
import org.dromara.maxkey.authn.jwt.AuthJwt;
import org.dromara.maxkey.constants.ConstsLoginType;
import org.dromara.maxkey.constants.ConstsPasswordSetType;
import org.dromara.maxkey.constants.ConstsStatus;
import org.dromara.maxkey.entity.Message;
import org.dromara.maxkey.entity.SocialsAssociate;
import org.dromara.maxkey.entity.SocialsProvider;
import org.dromara.maxkey.entity.idm.UserInfo;
import org.dromara.maxkey.id.uuid.UUID;
import org.dromara.maxkey.persistence.service.UserInfoService;
import org.dromara.maxkey.web.WebContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import me.zhyd.oauth.request.AuthRequest;

import java.util.Map;

/**
 * @author Crystal.Sea
 *
 */
@RestController
@RequestMapping(value = "/logon/oauth20")
public class SocialSignOnEndpoint  extends AbstractSocialSignOnEndpoint{
    static final  Logger _logger = LoggerFactory.getLogger(SocialSignOnEndpoint.class);
    // todo 抽离到配置文件
    private static final String FEISHU_ALLOWED_EMAIL_SUFFIX = "@snowx.com";
    private static final String FEISHU_PASSWORD_STATE_MARKER = "feishu_password_state_v1";
    private static final long PROVISIONING_TIME_TOLERANCE_MILLIS = 5 * 60 * 1000L;

    @Autowired
    UserInfoService userInfoService;

    private String getFrontendUrl(HttpServletRequest request) {
        String frontendUri = applicationConfig.getFrontendUri();
        return frontendUri.startsWith("http")
                ? frontendUri
                : WebContext.getContextPath(request, false) + frontendUri;
    }

    private String getFeishuEnterpriseEmail(SocialsAssociate socialsAssociate) {
        if (socialsAssociate == null || StringUtils.isBlank(socialsAssociate.getSocialUserInfo())) {
            return null;
        }
        JSONObject feishuUser = JSON.parseObject(socialsAssociate.getSocialUserInfo());
        return StringUtils.trimToNull(feishuUser.getString("enterprise_email"));
    }

    private String getFeishuAvatar(SocialsAssociate socialsAssociate) {
        if (socialsAssociate == null || StringUtils.isBlank(socialsAssociate.getSocialUserInfo())) {
            return null;
        }
        JSONObject feishuUser = JSON.parseObject(socialsAssociate.getSocialUserInfo());
        return StringUtils.trimToNull(feishuUser.getString("avatar_url"));
    }

    private boolean isAllowedFeishuUser(SocialsAssociate socialsAssociate) {
        String enterpriseEmail = getFeishuEnterpriseEmail(socialsAssociate);
        return enterpriseEmail != null
                && enterpriseEmail.length() > FEISHU_ALLOWED_EMAIL_SUFFIX.length()
                && StringUtils.endsWithIgnoreCase(enterpriseEmail, FEISHU_ALLOWED_EMAIL_SUFFIX);
    }

    private SocialsAssociate provisionFeishuUser(SocialsAssociate socialsAssociate) {
        JSONObject feishuUser = JSON.parseObject(socialsAssociate.getSocialUserInfo());
        String enterpriseEmail = StringUtils.lowerCase(getFeishuEnterpriseEmail(socialsAssociate));
        if (StringUtils.isBlank(enterpriseEmail)) {
            return null;
        }

        // Reuse an existing account with the verified enterprise email instead of
        // creating a duplicate user when the social mapping is missing.
        UserInfo existingUser = userInfoService.findByEmailAndInstId(
                enterpriseEmail, socialsAssociate.getInstId());
        if (existingUser != null) {
            if (existingUser.getStatus() != ConstsStatus.ACTIVE) {
                return null;
            }
            socialsAssociate.setUserId(existingUser.getId());
            socialsAssociate.setUsername(existingUser.getUsername());
            socialsAssociate.setExAttribute(FEISHU_PASSWORD_STATE_MARKER);
            try {
                if (socialsAssociateService.insert(socialsAssociate)) {
                    return socialsAssociate;
                }
            } catch (RuntimeException e) {
                _logger.warn("Feishu social mapping insert raced for {}", enterpriseEmail, e);
            }
            return socialsAssociateService.get(socialsAssociate);
        }

        UserInfo userInfo = new UserInfo();
        userInfo.setId(userInfo.generateId());
        userInfo.setUsername(enterpriseEmail);
        userInfo.setDisplayName(feishuUser.getString("name"));
        userInfo.setNickName(feishuUser.getString("name"));
        userInfo.setEmail(enterpriseEmail);
        userInfo.setPassword(userInfoService.randomPassword());
        userInfo.setPasswordSetType(ConstsPasswordSetType.PASSWORD_NOT_SET);
        userInfo.setUserType("EMPLOYEE");
        userInfo.setUserState("RESIDENT");
        userInfo.setStatus(ConstsStatus.ACTIVE);
        userInfo.setInstId(socialsAssociate.getInstId());

        if (!userInfoService.insert(userInfo)) {
            return null;
        }

        // passwordSetType is maintained by a dedicated mapper update and is not
        // part of the generic UserInfo insert column set.
        if (!userInfoService.updatePasswordSetType(userInfo)) {
            userInfoService.delete(userInfo);
            return null;
        }

        socialsAssociate.setUserId(userInfo.getId());
        socialsAssociate.setUsername(userInfo.getUsername());
        socialsAssociate.setExAttribute(FEISHU_PASSWORD_STATE_MARKER);
        try {
            if (socialsAssociateService.insert(socialsAssociate)) {
                return socialsAssociate;
            }
        } catch (RuntimeException e) {
            _logger.error("Failed to create Feishu social mapping for {}", enterpriseEmail, e);
        }
        userInfoService.delete(userInfo);
        return socialsAssociateService.get(socialsAssociate);
    }

    /**
     * Accounts provisioned before PASSWORD_NOT_SET existed received an unknown random
     * password. Mark them once, without resetting users who subsequently chose a password.
     */
    private void migrateLegacyFeishuPasswordState(SocialsAssociate socialsAssociate) {
        if (socialsAssociate == null) {
            return;
        }

        UserInfo userInfo = userInfoService.findByUsernameAndInstId(
                socialsAssociate.getUsername(), socialsAssociate.getInstId());
        if (userInfo == null) {
            return;
        }

        boolean feishuProvisionedAccount = StringUtils.startsWith(userInfo.getUsername(), "feishu_")
                || (StringUtils.equals(FEISHU_PASSWORD_STATE_MARKER, socialsAssociate.getExAttribute())
                        && StringUtils.equalsIgnoreCase(
                                userInfo.getUsername(), getFeishuEnterpriseEmail(socialsAssociate)));
        boolean generatedPasswordUnchanged = userInfo.getPasswordSetType() == ConstsPasswordSetType.PASSWORD_NORMAL
                && userInfo.getCreatedDate() != null
                && userInfo.getPasswordLastSetTime() != null
                && (userInfo.getPasswordLastSetTime().before(userInfo.getCreatedDate())
                        || Math.abs(userInfo.getPasswordLastSetTime().getTime()
                                - userInfo.getCreatedDate().getTime()) <= PROVISIONING_TIME_TOLERANCE_MILLIS);
        if (feishuProvisionedAccount && generatedPasswordUnchanged) {
            userInfo.setPasswordSetType(ConstsPasswordSetType.PASSWORD_NOT_SET);
            if (!userInfoService.updatePasswordSetType(userInfo)) {
                _logger.warn("Failed to migrate Feishu password state for user {}", userInfo.getUsername());
                return;
            }
        }
        if (feishuProvisionedAccount) {
            socialsAssociate.setExAttribute(FEISHU_PASSWORD_STATE_MARKER);
        }
    }

    @GetMapping("/authorize/{provider}")
    public Message<Object> authorize( HttpServletRequest request,@PathVariable String provider) {
        _logger.trace("SocialSignOn provider : {}" , provider);
        String instId = WebContext.getInst().getId();
        String authorizationUrl =
                buildAuthRequest(
                        instId,
                        provider,
                        getFrontendUrl(request)
                ).authorize(authTokenService.genRandomJwt());

        _logger.trace("authorize SocialSignOn : {}" , authorizationUrl);
        return new Message<Object>(authorizationUrl);
    }

    @GetMapping("/scanqrcode/{provider}")
    public Message<SocialsProvider> scanQRCode(HttpServletRequest request,@PathVariable String provider) {
        String instId = WebContext.getInst().getId();
        String frontendUrl = getFrontendUrl(request);
        AuthRequest authRequest = 
                buildAuthRequest(
                        instId,
                        provider,
                        frontendUrl);
        SocialsProvider scanQrProvider = null;
        if(authRequest != null ) {
            String state = UUID.generate().toString();
            //String state = authTokenService.genRandomJwt();
            authRequest.authorize(state);
            
            SocialsProvider socialSignOnProvider = socialSignOnProviderService.get(instId,provider);
            scanQrProvider = new SocialsProvider(socialSignOnProvider);
            scanQrProvider.setState(state);
            scanQrProvider.setRedirectUri(
                    socialSignOnProviderService.getRedirectUri(
                            frontendUrl, provider));
            //缓存state票据在缓存或者是redis中五分钟过期
            if (provider.equalsIgnoreCase(AuthMaxkeyRequest.KEY)) {
                socialSignOnProviderService.setToken(state);
            }
        }else {
             _logger.error("build authRequest fail .");
        }
        
        return new Message<>(scanQrProvider);
    }

    @GetMapping("/bind/{provider}")
    public Message<AuthJwt> bind(@PathVariable String provider,
                                  @CurrentUser UserInfo userInfo,
                                  HttpServletRequest request) {
         //auth call back may exception
        try {
            SocialsAssociate socialsAssociate = 
                    this.authCallback(userInfo.getInstId(),provider,getFrontendUrl(request));
            socialsAssociate.setSocialUserInfo(accountJsonString);
            socialsAssociate.setUserId(userInfo.getId());
            socialsAssociate.setUsername(userInfo.getUsername());
            socialsAssociate.setInstId(userInfo.getInstId());
            _logger.debug("Social Bind : {}",socialsAssociate);
            this.socialsAssociateService.delete(socialsAssociate);
            this.socialsAssociateService.insert(socialsAssociate);
            return new Message<>();
        }catch(Exception e) {
            _logger.error("callback Exception  ",e);
        }
        return new Message<>(Message.ERROR);
    }

    @GetMapping("/callback/{provider}")
    public Message<AuthJwt> callback(@PathVariable String provider,HttpServletRequest request) {
         //auth call back may exception
        try {
            String instId = WebContext.getInst().getId();
            SocialsAssociate socialsAssociate = 
                    this.authCallback(instId,provider,getFrontendUrl(request));

            if ("feishu".equalsIgnoreCase(provider) && !isAllowedFeishuUser(socialsAssociate)) {
                _logger.warn("Rejected Feishu login because enterprise_email is missing or outside snowx.com");
                return new Message<>(Message.FAIL, "仅允许使用 @snowx.com 企业邮箱的飞书账号登录");
            }

            SocialsAssociate socialssssociate1 = this.socialsAssociateService.get(socialsAssociate);
        
            _logger.debug("Loaded SocialSignOn Socials Associate : {}",socialssssociate1);
        
            if (null == socialssssociate1) {
                if ("feishu".equalsIgnoreCase(provider)
                        && StringUtils.isNotEmpty(socialsAssociate.getSocialUserInfo())) {
                    socialssssociate1 = provisionFeishuUser(socialsAssociate);
                }
                if ("feishu".equalsIgnoreCase(provider) && socialssssociate1 == null) {
                    return new Message<>(Message.FAIL, "该企业邮箱对应的用户没有访问权限或自动注册失败，请联系管理员授权");
                }
                //如果存在第三方ID并且在数据库无法找到映射关系，则进行绑定逻辑
                if (socialssssociate1 == null && StringUtils.isNotEmpty(socialsAssociate.getSocialUserId())) {
                    //返回message为第三方用户标识
                    return new Message<>(Message.PROMPT,socialsAssociate.getSocialUserId());
                }
            }

            socialsAssociate = socialssssociate1;
            if(socialsAssociate != null) {
                if ("feishu".equalsIgnoreCase(provider)) {
                    migrateLegacyFeishuPasswordState(socialsAssociate);
                }
                _logger.debug("Social Sign On from {} mapping to user {}",
                        socialsAssociate.getProvider(),socialsAssociate.getUsername());
                LoginCredential loginCredential =new LoginCredential(
                        socialsAssociate.getUsername(),"",ConstsLoginType.SOCIALSIGNON);
                SocialsProvider socialSignOnProvider = socialSignOnProviderService.get(instId,provider);
                loginCredential.setProvider(socialSignOnProvider.getProviderName());
                
                Authentication  authentication = authenticationProvider.authenticate(loginCredential,true);
                socialsAssociate.setSocialUserInfo(accountJsonString);
            
                this.socialsAssociateService.update(socialsAssociate);
                AuthJwt authJwt = authTokenService.genAuthJwt(authentication);
                if ("feishu".equalsIgnoreCase(provider)) {
                    authJwt.setEmail(getFeishuEnterpriseEmail(socialsAssociate));
                    authJwt.setAvatar(getFeishuAvatar(socialsAssociate));
                }
                return new Message<>(authJwt);
            }else {
                
            }
        }catch(Exception e) {
             _logger.error("callback Exception  ",e);
             
        }
        return new Message<>(Message.ERROR);
    }


    /**
     * 提供给第三方应用关联用户接口
     * @return
     */
    @PostMapping("/workweixin/qr/auth/login")
    public Message<AuthJwt> qrAuthLogin(
            @RequestParam Map<String, String> param,
            HttpServletRequest request) {

        try {
            if (null == param){
                return new Message<>(Message.ERROR);
            }
            String token = param.get("token");
            String username = param.get("username");
            //判断token是否合法
            String redisusername = this.socialSignOnProviderService.getToken(token);
            if (StringUtils.isNotEmpty(redisusername)){
                //设置token和用户绑定
                boolean flag = this.socialSignOnProviderService.bindtoken(token,username);
                if (flag) {
                    return new Message<>();
                }
            } else {
                return new Message<>(Message.WARNING,"Invalid token");
            }
        }catch(Exception e) {
            _logger.error("qrAuthLogin Exception  ",e);
        }
        return new Message<>(Message.ERROR);
    }


    /**
     * maxkey 监听扫码回调
     * @param provider
     * @param state
     * @param request
     * @return
     */
    @PostMapping("/qrcallback/{provider}/{state}")
    public Message<AuthJwt> qrcallback(@PathVariable String provider,@PathVariable String state,
                                        HttpServletRequest request) {
        try {
            //判断只有maxkey扫码
            if (!provider.equalsIgnoreCase(AuthMaxkeyRequest.KEY)) {
                return new Message<>(Message.ERROR);
            }

            String loginName = socialSignOnProviderService.getToken(state);
            if (StringUtils.isEmpty(loginName)) {
                //二维码过期
                return new Message<>(Message.PROMPT);
            }
            if("-1".equalsIgnoreCase(loginName)){
                //暂无用户扫码
                return new Message<>(Message.WARNING);
            }
            String instId = WebContext.getInst().getId();

            SocialsAssociate socialsAssociate = new SocialsAssociate();
            socialsAssociate.setProvider(provider);
            socialsAssociate.setSocialUserId(loginName);
            socialsAssociate.setInstId(instId);


            socialsAssociate = this.socialsAssociateService.get(socialsAssociate);

            _logger.debug("qrcallback Loaded SocialSignOn Socials Associate : {}",socialsAssociate);

            if(null == socialsAssociate) {
                return new Message<>(Message.ERROR);
            }

            LoginCredential loginCredential =new LoginCredential(
                    socialsAssociate.getUsername(),"",ConstsLoginType.SOCIALSIGNON);
            SocialsProvider socialSignOnProvider = socialSignOnProviderService.get(instId,provider);
            loginCredential.setProvider(socialSignOnProvider.getProviderName());

            Authentication  authentication = authenticationProvider.authenticate(loginCredential,true);
            socialsAssociate.setSocialUserInfo(accountJsonString);

            this.socialsAssociateService.update(socialsAssociate);
            return new Message<>(authTokenService.genAuthJwt(authentication));
        }catch(Exception e) {
            _logger.error("qrcallback Exception  ",e);
            return new Message<>(Message.ERROR);
        }
    }
}
