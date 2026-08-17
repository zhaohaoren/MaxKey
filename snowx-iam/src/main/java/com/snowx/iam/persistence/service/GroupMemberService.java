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
 

package com.snowx.iam.persistence.service;

import java.util.List;

import com.snowx.iam.entity.idm.GroupMember;
import com.snowx.iam.entity.idm.Groups;
import com.snowx.iam.entity.idm.UserInfo;
import com.snowx.iam.persistence.mybatis.SnowxService;
import com.snowx.iam.persistence.mybatis.PageResults;

public interface GroupMemberService  extends SnowxService<GroupMember,String>{

    public int addDynamicMember(Groups dynamicGroup) ;
    
    public int deleteDynamicMember(Groups dynamicGroup) ;
    
    public int deleteByGroupId(String groupId);
    
    public List<UserInfo> queryMemberByGroupId(String groupId);
    
    public PageResults<Groups> noMember(GroupMember entity) ;
    
}
