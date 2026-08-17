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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.snowx.iam.authn.LoginCredential;
import com.snowx.iam.authn.QrCodeCredentialDto;
import com.snowx.iam.authn.ScanCode;
import com.snowx.iam.authn.jwt.AuthJwt;
import com.snowx.iam.authn.jwt.AuthTokenService;
import com.snowx.iam.authn.provider.AbstractAuthenticationProvider;
import com.snowx.iam.authn.provider.scancode.ScanCodeService;
import com.snowx.iam.authn.session.Session;
import com.snowx.iam.authn.session.SessionManager;
import com.snowx.iam.authn.web.AuthorizationUtils;
import com.snowx.iam.crypto.Base64Utils;
import com.snowx.iam.crypto.password.PasswordReciprocal;
import com.snowx.iam.entity.Message;
import com.snowx.iam.exception.BusinessException;
import com.snowx.iam.util.QRCodeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.awt.image.BufferedImage;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Objects;

/**
 * @author Crystal.Sea
 *
 */
@Tag(name = "1-1-登录扫码接口文档模块")
@RestController
@RequestMapping(value = "/login")
public class LoginScanCodeEndpoint {
    private static Logger logger = LoggerFactory.getLogger(LoginScanCodeEndpoint.class);

    @Autowired
    AuthTokenService authTokenService;

    @Autowired
    AbstractAuthenticationProvider authenticationProvider ;

    @Autowired
    ScanCodeService scanCodeService;

    @Autowired
    SessionManager sessionManager;

     @Operation(summary = "生成登录扫描二维码", description = "生成登录扫描二维码", method = "GET")
     @GetMapping("/genScanCode")
     public Message<HashMap<String,String>> genScanCode() {
         logger.debug("/genScanCode.");
         String ticket = scanCodeService.createTicket();
         logger.debug("ticket: {}",ticket);
         String encodeTicket = PasswordReciprocal.getInstance().encode(ticket);
         BufferedImage bufferedImage  =  QRCodeUtils.write2BufferedImage(encodeTicket, "gif", 300, 300);
         String rqCode = Base64Utils.encodeImage(bufferedImage);
         HashMap<String,String> codeMap = new HashMap<>();
         codeMap.put("rqCode", rqCode);
         codeMap.put("ticket", encodeTicket);
         return new Message<>(Message.SUCCESS, codeMap);
     }

    @Operation(summary = "web二维码登录", description = "web二维码登录", method = "POST")
    @PostMapping("/sign/qrcode")
    public Message<AuthJwt> signByQrcode(@Validated @RequestBody ScanCode scanCode) {
        LoginCredential loginCredential = new LoginCredential();
        loginCredential.setAuthType(scanCode.getAuthType());
        loginCredential.setUsername(scanCode.getCode());

        if(authTokenService.validateJwtToken(scanCode.getState())){
            try {
                Authentication authentication = authenticationProvider.authenticate(loginCredential);
                if (Objects.nonNull(authentication)) {
                    //success
                    AuthJwt authJwt = authTokenService.genAuthJwt(authentication);
                    return new Message<>(authJwt);
                } else {
                    return new Message<>(Message.FAIL, "尚未扫码");
                }
            } catch (BusinessException businessException) {
                return new Message<>(businessException.getCode(), businessException.getMessage());
            }
        } else {
            return new Message<>(20005, "state失效重新获取");
        }
    }

    @Operation(summary = "app扫描二维码", description = "扫描二维码登录", method = "POST")
    @PostMapping("/scanCode")
    public Message<String> scanCode(@Validated @RequestBody QrCodeCredentialDto credentialDto) throws ParseException {
        logger.debug("/scanCode.");
        String jwtToken = credentialDto.getJwtToken();
        String code = credentialDto.getCode();
        try {
            //获取登录会话
            Session session = AuthorizationUtils.getSession(sessionManager, jwtToken);
            if (Objects.isNull(session)) {
                return new Message<>(Message.FAIL, "登录会话失效，请重新登录");
            }
            //查询二维码是否过期
            String ticketString = PasswordReciprocal.getInstance().decoder(code);
            boolean codeResult = scanCodeService.validateTicket(ticketString, session);
            if (!codeResult) {
                return new Message<>(Message.FAIL, "二维码已过期，请重新获取");
            }

        } catch (ParseException e) {
            logger.error("ParseException.",e);
            return new Message<>(Message.FAIL, "token格式错误");
        }
        return new Message<>(Message.SUCCESS, "成功");
    }
}
