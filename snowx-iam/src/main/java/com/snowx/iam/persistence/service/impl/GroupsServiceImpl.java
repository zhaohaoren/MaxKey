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
 

package com.snowx.iam.persistence.service.impl;

import java.sql.Types;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import com.snowx.iam.constants.ConstsStatus;
import com.snowx.iam.entity.Institutions;
import com.snowx.iam.entity.idm.Groups;
import com.snowx.iam.entity.permissions.Roles;
import com.snowx.iam.persistence.mapper.GroupsMapper;
import com.snowx.iam.persistence.service.GroupMemberService;
import com.snowx.iam.persistence.service.GroupsService;
import com.snowx.iam.persistence.service.InstitutionsService;
import com.snowx.iam.util.StrUtils;
import com.snowx.iam.persistence.mybatis.SnowxServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class GroupsServiceImpl  extends SnowxServiceImpl<GroupsMapper,Groups,String> implements GroupsService{
    static final  Logger _logger = LoggerFactory.getLogger(GroupsServiceImpl.class);

    @Autowired
    GroupMemberService groupMemberService;
    
    @Autowired
    InstitutionsService institutionsService;
    
    @Override
    public List<Groups> queryDynamicGroups(Groups groups){
        return this.getMapper().queryDynamic(groups);
    }
    
    @Override
    public boolean deleteById(String groupId) {
        this.delete(groupId);
        groupMemberService.deleteByGroupId(groupId);
        return true;
    }
    
    @Override
    public List<Groups> queryByUserId(String userId){
        return this.getMapper().queryByUserId(userId);
    }
    
    @Override
    public void refreshDynamicGroups(Groups dynamicGroup){
        if(dynamicGroup.getCategory().equals(Roles.Category.DYNAMIC)) {
        	if(StringUtils.isNotBlank(dynamicGroup.getOrgIdsList())) {
                dynamicGroup.setOrgIds(StrUtils.string2List(dynamicGroup.getOrgIdsList(), ","));
                _logger.debug("OrgIds {}" , dynamicGroup.getOrgIds());
            }
            groupMemberService.deleteDynamicMember(dynamicGroup);
            groupMemberService.addDynamicMember(dynamicGroup);
        }
    }
    
    @Override
    public void refreshAllDynamicGroups(){
        List<Institutions> instList = 
                institutionsService.find("where status = ? ", new Object[]{ConstsStatus.ACTIVE}, new int[]{Types.INTEGER});
        for(Institutions inst : instList) {
            Groups group = new Groups();
            group.setInstId(inst.getId());
            List<Groups>  groupsList = queryDynamicGroups(group);
            for(Groups g : groupsList) {
                _logger.debug("role {}" , g);
                refreshDynamicGroups(g);
            }
        }
    }

}
