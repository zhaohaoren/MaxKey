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
 

package com.snowx.iam.password.sms;

import java.util.concurrent.TimeUnit;

import com.snowx.iam.configuration.EmailConfig;
import com.snowx.iam.constants.ConstsBoolean;
import com.snowx.iam.crypto.password.PasswordReciprocal;
import com.snowx.iam.entity.cnf.CnfEmailSenders;
import com.snowx.iam.entity.cnf.CnfSmsProvider;
import com.snowx.iam.password.onetimepwd.AbstractOtpAuthn;
import com.snowx.iam.password.onetimepwd.impl.MailOtpAuthn;
import com.snowx.iam.password.onetimepwd.token.RedisOtpTokenStore;
import com.snowx.iam.password.sms.impl.SmsOtpAuthnAliyun;
import com.snowx.iam.password.sms.impl.SmsOtpAuthnTencentCloud;
import com.snowx.iam.password.sms.impl.SmsOtpAuthnYunxin;
import com.snowx.iam.persistence.service.CnfEmailSendersService;
import com.snowx.iam.persistence.service.CnfSmsProviderService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

public class SmsOtpAuthnService {

    static final Cache<String, AbstractOtpAuthn> smsAuthnStore = 
            Caffeine.newBuilder().expireAfterWrite(60, TimeUnit.MINUTES).build();

    CnfSmsProviderService smsProviderService;
    
    CnfEmailSendersService emailSendersService;
    
    RedisOtpTokenStore redisOptTokenStore;
    
    public SmsOtpAuthnService(CnfSmsProviderService smsProviderService, CnfEmailSendersService emailSendersService) {
        this.smsProviderService = smsProviderService;
        this.emailSendersService = emailSendersService;
    }

    public SmsOtpAuthnService(CnfSmsProviderService smsProviderService,CnfEmailSendersService emailSendersService,RedisOtpTokenStore redisOptTokenStore) {
        this.smsProviderService = smsProviderService;
        this.emailSendersService = emailSendersService;
        this.redisOptTokenStore = redisOptTokenStore;
    }

    public AbstractOtpAuthn getByInstId(String instId) {
        AbstractOtpAuthn smsOtpAuthn = smsAuthnStore.getIfPresent(instId);
        if(smsOtpAuthn == null) {
            LambdaQueryWrapper<CnfSmsProvider> lambdaQuery = new LambdaQueryWrapper<CnfSmsProvider>();
            lambdaQuery.eq(CnfSmsProvider::getInstId, instId);
            CnfSmsProvider smsProvider = smsProviderService.get(lambdaQuery);
            if(smsProvider != null ) {
                if("aliyun".equalsIgnoreCase(smsProvider.getProvider())) {
                    smsOtpAuthn = new SmsOtpAuthnAliyun(
                            smsProvider.getAppKey(),
                            PasswordReciprocal.getInstance().decoder(smsProvider.getAppSecret()),
                            smsProvider.getTemplateId(), 
                            smsProvider.getSignName());
                }else if("tencentcloud".equalsIgnoreCase(smsProvider.getProvider())) {
                    smsOtpAuthn = new SmsOtpAuthnTencentCloud(
                            smsProvider.getAppKey(),
                            PasswordReciprocal.getInstance().decoder(smsProvider.getAppSecret()),
                            smsProvider.getSmsSdkAppId(), 
                            smsProvider.getTemplateId(), smsProvider.getSignName());
                }else if("neteasesms".equalsIgnoreCase(smsProvider.getProvider())) {
                    smsOtpAuthn = new SmsOtpAuthnYunxin(
                            smsProvider.getAppKey(),
                            PasswordReciprocal.getInstance().decoder(smsProvider.getAppSecret()),
                            smsProvider.getTemplateId());
                }else if("email".equalsIgnoreCase(smsProvider.getProvider())) {
                    LambdaQueryWrapper<CnfEmailSenders> emailSenderslambdaQuery = new LambdaQueryWrapper<CnfEmailSenders>();
                    emailSenderslambdaQuery.eq(CnfEmailSenders::getInstId, instId);
                    CnfEmailSenders emailSender = emailSendersService.get(emailSenderslambdaQuery);
                    String credentials = PasswordReciprocal.getInstance().decoder(emailSender.getCredentials());
                    EmailConfig emailConfig = new EmailConfig(
                            emailSender.getAccount(), 
                            credentials,
                            emailSender.getSmtpHost(),
                            emailSender.getPort(),
                            ConstsBoolean.isTrue(emailSender.getSslSwitch()), 
                            emailSender.getSender());
                    smsOtpAuthn = new MailOtpAuthn(emailConfig);
                }
                
                if(redisOptTokenStore != null) {
                    smsOtpAuthn.setOptTokenStore(redisOptTokenStore);
                }
                smsAuthnStore.put(instId, smsOtpAuthn);    
            }
        }
        return smsOtpAuthn;
    }

    public void setRedisOptTokenStore(RedisOtpTokenStore redisOptTokenStore) {
        this.redisOptTokenStore = redisOptTokenStore;
    }
    
    
}
