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
 

package com.snowx.iam.persistence.service.impl;

import java.util.List;

import com.snowx.iam.entity.permissions.Permission;
import com.snowx.iam.persistence.mapper.PermissionMapper;
import com.snowx.iam.persistence.service.PermissionService;
import com.snowx.iam.persistence.mybatis.SnowxServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

@Repository
public class PermissionServiceImpl  extends SnowxServiceImpl<PermissionMapper,Permission,String> implements PermissionService{
    static final  Logger _logger = LoggerFactory.getLogger(PermissionServiceImpl.class);

    @Override
    public boolean insertGroupPrivileges(List<Permission> rolePermissionsList) {
        return getMapper().insertGroupPrivileges(rolePermissionsList)>0;
    };
    
    @Override
    public boolean deleteGroupPrivileges(List<Permission> rolePermissionsList) {
         return getMapper().deleteGroupPrivileges(rolePermissionsList)>=0;
     }
    
    @Override
    public List<Permission> queryGroupPrivileges(Permission rolePermissions){
        return getMapper().queryGroupPrivileges(rolePermissions);
    }    

}
