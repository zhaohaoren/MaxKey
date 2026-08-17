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
 

package com.snowx.iam.web.admin.controller.apps;

import com.snowx.iam.authn.annotation.CurrentUser;
import com.snowx.iam.constants.ConstsProtocols;
import com.snowx.iam.crypto.ReciprocalUtils;
import com.snowx.iam.entity.Message;
import com.snowx.iam.entity.apps.AppsJwtDetails;
import com.snowx.iam.entity.apps.AppsTokenBasedDetails;
import com.snowx.iam.entity.idm.UserInfo;
import com.snowx.iam.persistence.service.AppsTokenBasedDetailsService;
import com.snowx.iam.util.StringGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping(value={"/admin/apps/tokenbased"})
public class TokenBasedDetailsController  extends BaseAppContorller {
    static final  Logger logger = LoggerFactory.getLogger(TokenBasedDetailsController.class);
    
    @Autowired
    AppsTokenBasedDetailsService tokenBasedDetailsService;
    
    @GetMapping(value = { "/init" }, produces = {MediaType.APPLICATION_JSON_VALUE})
    public Message<?> init() {
        AppsTokenBasedDetails tokenBasedDetails =new AppsTokenBasedDetails();
        tokenBasedDetails.setId(tokenBasedDetails.generateId());
        tokenBasedDetails.setProtocol(ConstsProtocols.TOKENBASED);
        tokenBasedDetails.setSecret(StringGenerator.generateKey(ReciprocalUtils.Algorithm.AES));
        tokenBasedDetails.setAlgorithmKey(tokenBasedDetails.getSecret());
        tokenBasedDetails.setUserPropertys("userPropertys");
        return new Message<AppsTokenBasedDetails>(tokenBasedDetails);
    }
    
    @GetMapping(value = { "/get/{id}" }, produces = {MediaType.APPLICATION_JSON_VALUE})
    public Message<?> get(@PathVariable String id) {
        AppsTokenBasedDetails tokenBasedDetails=tokenBasedDetailsService.get(id , false);
        decoderSecret(tokenBasedDetails);
        String algorithmKey=passwordReciprocal.decoder(tokenBasedDetails.getAlgorithmKey());
        tokenBasedDetails.setAlgorithmKey(algorithmKey);
        tokenBasedDetails.transIconBase64();
        return new Message<AppsTokenBasedDetails>(tokenBasedDetails);
    }
    
    @ResponseBody
    @PostMapping(value={"/add"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    public Message<?> add(
            @RequestBody AppsTokenBasedDetails tokenBasedDetails,
            @CurrentUser UserInfo currentUser) {
        logger.debug("-Add  : {}" , tokenBasedDetails);
        
        transform(tokenBasedDetails);
        
        tokenBasedDetails.setAlgorithmKey(tokenBasedDetails.getSecret());
        tokenBasedDetails.setInstId(currentUser.getInstId());
        if (tokenBasedDetailsService.insert(tokenBasedDetails)
                &&appsService.insertApp(tokenBasedDetails)) {
            return new Message<AppsJwtDetails>(Message.SUCCESS);
        } else {
            return new Message<AppsJwtDetails>(Message.FAIL);
        }
    }
    
    @ResponseBody
    @PutMapping(value={"/update"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    public Message<?> update(
            @RequestBody AppsTokenBasedDetails tokenBasedDetails,
            @CurrentUser UserInfo currentUser) {
        logger.debug("-update  : {}" , tokenBasedDetails);
        transform(tokenBasedDetails);
        tokenBasedDetails.setAlgorithmKey(tokenBasedDetails.getSecret());
        tokenBasedDetails.setInstId(currentUser.getInstId());
        if (tokenBasedDetailsService.update(tokenBasedDetails)
                &&appsService.updateApp(tokenBasedDetails)) {
            return new Message<AppsJwtDetails>(Message.SUCCESS);
        } else {
            return new Message<AppsJwtDetails>(Message.FAIL);
        }
    }
    
    @ResponseBody
    @DeleteMapping(value={"/delete"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    public Message<?> delete(
            @RequestParam List<String> ids,
            @CurrentUser UserInfo currentUser) {
        logger.debug("-delete  ids : {} " , ids);
        if (tokenBasedDetailsService.deleteBatch(ids)&&appsService.deleteBatch(ids)) {
             return new Message<AppsJwtDetails>(Message.SUCCESS);
        } else {
            return new Message<AppsJwtDetails>(Message.FAIL);
        }
    }
    
}
