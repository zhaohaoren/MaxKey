/*
 * Copyright [2024] [MaxKey of copyright http://www.maxkey.top]
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

import java.sql.Types;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import com.snowx.iam.constants.ConstsStatus;
import com.snowx.iam.entity.Institutions;
import com.snowx.iam.entity.permissions.Roles;
import com.snowx.iam.persistence.mapper.RolesMapper;
import com.snowx.iam.persistence.service.InstitutionsService;
import com.snowx.iam.persistence.service.RoleMemberService;
import com.snowx.iam.persistence.service.RolesService;
import com.snowx.iam.util.StrUtils;
import com.snowx.iam.persistence.mybatis.SnowxServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class RolesServiceImpl  extends SnowxServiceImpl<RolesMapper,Roles,String> implements RolesService{
    static final  Logger _logger = LoggerFactory.getLogger(RolesServiceImpl.class);
    
    @Autowired
    RoleMemberService roleMemberService;
    
    @Autowired
    InstitutionsService institutionsService;

    
    @Override
    public List<Roles> queryDynamicRoles(Roles groups){
        return this.getMapper().queryDynamicRoles(groups);
    }
    
    @Override
    public boolean deleteById(String groupId) {
        this.delete(groupId);
        roleMemberService.deleteByRoleId(groupId);
        return true;
    }
    
    @Override
    public List<Roles> queryRolesByUserId(String userId){
        return this.getMapper().queryRolesByUserId(userId);
    }
    
    @Override
    public void refreshDynamicRoles(Roles dynamicRole){
        if(dynamicRole.getCategory().equals(Roles.Category.DYNAMIC)) {
        	if(StringUtils.isNotBlank(dynamicRole.getOrgIdsList())) {
        		dynamicRole.setOrgIds(StrUtils.string2List(dynamicRole.getOrgIdsList(), ","));
                _logger.debug("OrgIds {}" , dynamicRole.getOrgIds());
            }
            roleMemberService.deleteDynamicRoleMember(dynamicRole);
            roleMemberService.addDynamicRoleMember(dynamicRole);
        
        }
    }
    
    @Override
    public void refreshAllDynamicRoles(){
        List<Institutions> instList = 
                institutionsService.find("where status = ? ", new Object[]{ConstsStatus.ACTIVE}, new int[]{Types.INTEGER});
        for(Institutions inst : instList) {
            Roles role = new Roles();
            role.setInstId(inst.getId());
            List<Roles>  rolesList = queryDynamicRoles(role);
            for(Roles r : rolesList) {
                _logger.debug("role {}" , r);
                refreshDynamicRoles(r);
            }
        }
    }

}
