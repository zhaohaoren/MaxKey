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

import com.snowx.iam.entity.idm.GroupMember;
import com.snowx.iam.entity.idm.Groups;
import com.snowx.iam.entity.idm.UserInfo;
import com.snowx.iam.persistence.mapper.GroupMemberMapper;
import com.snowx.iam.persistence.service.GroupMemberService;
import com.snowx.iam.persistence.mybatis.PageResults;
import com.snowx.iam.persistence.mybatis.SnowxServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

@Repository
public class GroupMemberServiceImpl  extends SnowxServiceImpl<GroupMemberMapper,GroupMember,String> implements GroupMemberService{
    static final  Logger _logger = LoggerFactory.getLogger(GroupMemberServiceImpl.class);

    @Override
    public int addDynamicMember(Groups dynamicGroup) {
        return getMapper().addDynamicMember(dynamicGroup);
    }
    
    @Override
    public int deleteDynamicMember(Groups dynamicGroup) {
        return getMapper().deleteDynamicMember(dynamicGroup);
    }
    
    @Override
    public int deleteByGroupId(String groupId) {
        return getMapper().deleteByGroupId(groupId);
    }
    
    @Override
    public List<UserInfo> queryMemberByGroupId(String groupId){
        return getMapper().queryMemberByGroupId(groupId);
    }
    
    
    @Override
    public PageResults<Groups> noMember(GroupMember entity) {
        entity.build();
        List<Groups> resultslist = null;
        try {
            resultslist = getMapper().noMember(entity);
        } catch (Exception e) {
            _logger.error("queryPageResults Exception " , e);
        }
        //当前页记录数
        Integer records = PageResults.parseRecords(resultslist);
        //总页数
        Integer totalCount =fetchCount(entity, resultslist);
        return new PageResults<Groups>(entity.getPageNumber(),entity.getPageSize(),records,totalCount,resultslist);
    }
    
}
