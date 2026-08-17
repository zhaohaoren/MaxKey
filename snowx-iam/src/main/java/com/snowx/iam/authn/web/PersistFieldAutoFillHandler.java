/*
 * Copyright [2025] [MaxKey of copyright http://www.maxkey.top]
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
 

package com.snowx.iam.authn.web;

import java.util.Date;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import com.snowx.iam.authn.SignPrincipal;
import org.springframework.stereotype.Component;

@Component
public class PersistFieldAutoFillHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        
        SignPrincipal principal = getPrincipal();
        if(principal != null) {
            this.setFieldValByName("instId", principal.getInstId(), metaObject);
            this.setFieldValByName("createdBy", principal.getUserId(), metaObject);
        }
        this.setFieldValByName("createdDate", new Date(), metaObject);
        
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        SignPrincipal principal = getPrincipal();
        if(principal != null) {
            this.setFieldValByName("modifiedBy", principal.getUserId(), metaObject);
        }
        this.setFieldValByName("modifiedDate", new Date(), metaObject);
    }
    
    /**
     * 获取principal , 忽略异常情况
     * @return
     */
    SignPrincipal getPrincipal() {
        SignPrincipal principal = null;
        try {
            principal = AuthorizationUtils.getPrincipal();
        }catch(Exception e) {
            //
        }
        return principal;
    }

}
