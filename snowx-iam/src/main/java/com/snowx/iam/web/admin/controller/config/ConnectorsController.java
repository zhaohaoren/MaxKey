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
 

package com.snowx.iam.web.admin.controller.config;

import org.apache.commons.lang3.StringUtils;
import com.snowx.iam.authn.annotation.CurrentUser;
import com.snowx.iam.crypto.password.PasswordReciprocal;
import com.snowx.iam.entity.Connectors;
import com.snowx.iam.entity.Message;
import com.snowx.iam.entity.idm.UserInfo;
import com.snowx.iam.persistence.service.ConnectorsService;
import com.snowx.iam.persistence.mybatis.PageResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value={"/admin/config/connectors"})
public class ConnectorsController {
    static final  Logger logger = LoggerFactory.getLogger(ConnectorsController.class);
    
    @Autowired
    ConnectorsService connectorsService;
    
    @GetMapping(value = { "/fetch" }, produces = {MediaType.APPLICATION_JSON_VALUE})
    public Message<PageResults<Connectors>> fetch(Connectors connector,@CurrentUser UserInfo currentUser) {
        logger.debug("fetch {}" , connector);
        connector.setInstId(currentUser.getInstId());
        return new Message<>(connectorsService.fetchPageResults(connector));
    }
    
    @GetMapping(value = { "/get/{id}" }, produces = {MediaType.APPLICATION_JSON_VALUE})
    public Message<Connectors> get(@PathVariable String id,@CurrentUser UserInfo currentUser) {
        Connectors connector = connectorsService.get(id,currentUser.getInstId());
        if(StringUtils.isNotBlank(connector.getCredentials())) {
            connector.setCredentials(PasswordReciprocal.getInstance().decoder(connector.getCredentials()));
        }
        return new Message<>(connector);
    }
    
    @PostMapping(value={"/add"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    public Message<Connectors> insert(@RequestBody  Connectors connector,@CurrentUser UserInfo currentUser) {
        logger.debug("-Add  : {}" , connector);
        connector.setId(connector.generateId());
        connector.setInstId(currentUser.getInstId());
        if(StringUtils.isNotBlank(connector.getCredentials())) {
            connector.setCredentials(PasswordReciprocal.getInstance().encode(connector.getCredentials()));
        }
        if (connectorsService.insert(connector)) {
            return new Message<>(Message.SUCCESS);
        } else {
            return new Message<>(Message.FAIL);
        }
    }
    
    @PutMapping(value={"/update"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    public Message<Connectors> update(@RequestBody  Connectors connector,@CurrentUser UserInfo currentUser) {
        logger.debug("-update  : {}" , connector);
        connector.setInstId(currentUser.getInstId());
        connector.setCredentials(PasswordReciprocal.getInstance().encode(connector.getCredentials()));
        if (connectorsService.update(connector)) {
            return new Message<>(Message.SUCCESS);
        } else {
            return new Message<>(Message.FAIL);
        }
    }
    
    @DeleteMapping(value={"/delete"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    public Message<Connectors> delete(@RequestParam List<String> ids,@CurrentUser UserInfo currentUser) {
        logger.debug("-delete  ids : {} " , ids);
        if (connectorsService.deleteBatch(ids,currentUser.getInstId())) {
             return new Message<>(Message.SUCCESS);
        } else {
            return new Message<>(Message.FAIL);
        }
    }

}
