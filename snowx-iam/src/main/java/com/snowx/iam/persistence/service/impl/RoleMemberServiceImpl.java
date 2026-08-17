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

import java.util.List;

import com.snowx.iam.entity.idm.UserInfo;
import com.snowx.iam.entity.permissions.RoleMember;
import com.snowx.iam.entity.permissions.Roles;
import com.snowx.iam.persistence.mapper.RoleMemberMapper;
import com.snowx.iam.persistence.service.RoleMemberService;
import com.snowx.iam.persistence.mybatis.PageResults;
import com.snowx.iam.persistence.mybatis.SnowxServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

@Repository
public class RoleMemberServiceImpl  extends SnowxServiceImpl<RoleMemberMapper,RoleMember,String> implements RoleMemberService{
    static final  Logger _logger = LoggerFactory.getLogger(RoleMemberServiceImpl.class);

    @Override
    public int addDynamicRoleMember(Roles dynamicGroup) {
        return getMapper().addDynamicRoleMember(dynamicGroup);
    }
    
    @Override
    public int deleteDynamicRoleMember(Roles dynamicGroup) {
        return getMapper().deleteDynamicRoleMember(dynamicGroup);
    }
    
    @Override
    public int deleteByRoleId(String groupId) {
        return getMapper().deleteByRoleId(groupId);
    }
    
    @Override
    public List<UserInfo> queryMemberByRoleId(String groupId){
        return getMapper().queryMemberByRoleId(groupId);
    }
    
    
    @Override
    public PageResults<Roles> rolesNoMember(RoleMember entity) {
        entity.build();
        List<Roles> resultslist = null;
        try {
            resultslist = getMapper().rolesNoMember(entity);
        } catch (Exception e) {
            _logger.error("fetchPageResults Exception " , e);
        }
        //当前页记录数
        Integer records = PageResults.parseRecords(resultslist);
        //总页数
        Integer totalCount =fetchCount(entity, resultslist);
        return new PageResults<Roles>(entity.getPageNumber(),entity.getPageSize(),records,totalCount,resultslist);
    }
    
}
