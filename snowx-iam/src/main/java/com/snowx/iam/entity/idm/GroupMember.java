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
 

package com.snowx.iam.entity.idm;

import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
@TableName("mxk_group_member")  
public class GroupMember extends UserInfo implements Serializable{

    /**
     * 
     */
    private static final long serialVersionUID = -8059639972590554760L;
    @TableId(type = IdType.INPUT)
    String id;
    @TableField
    private String groupId;
    @TableField(exist = false)
    private String groupName;
    @TableField(exist = false)
    private String category;
    @TableField
    private String memberId;
    @TableField(exist = false)
    private String memberName;
    @TableField
    private String type;//User or Group

    @TableField
    private String instId;

    @TableField(exist = false)
    private String instName;
    
    public GroupMember(){
        super();
    }

    
    /**
     * @param groupId
     * @param memberId
     * @param type
     */
    public GroupMember(String groupId, String memberId, String type , String instId) {
        super();
        this.groupId = groupId;
        this.memberId = memberId;
        this.type = type;
        this.instId = instId;
    }


    public GroupMember(String groupId, String groupName, String memberId,
            String memberName, String type , String instId) {
        super();
        this.groupId = groupId;
        this.groupName = groupName;
        this.memberId = memberId;
        this.memberName = memberName;
        this.type = type;
        this.instId = instId;
    }


    @Override
    public String getId() {
        return id;
    }


    @Override
    public void setId(String id) {
        this.id = id;
    }


    public String getGroupId() {
        return groupId;
    }


    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }


    public String getGroupName() {
        return groupName;
    }


    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }


    /**
     * @return the memberId
     */
    public String getMemberId() {
        return memberId;
    }

    /**
     * @param memberId the memberId to set
     */
    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }



    public String getMemberName() {
        return memberName;
    }

    public void setMemberName(String memberName) {
        this.memberName = memberName;
    }


    public String getCategory() {
        return category;
    }


    public void setCategory(String category) {
        this.category = category;
    }


    @Override
    public String getInstId() {
        return instId;
    }


    @Override
    public void setInstId(String instId) {
        this.instId = instId;
    }


    @Override
    public String getInstName() {
        return instName;
    }


    @Override
    public void setInstName(String instName) {
        this.instName = instName;
    }


    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("GroupMember [id=");
        builder.append(id);
        builder.append(", groupId=");
        builder.append(groupId);
        builder.append(", groupName=");
        builder.append(groupName);
        builder.append(", category=");
        builder.append(category);
        builder.append(", memberId=");
        builder.append(memberId);
        builder.append(", memberName=");
        builder.append(memberName);
        builder.append(", type=");
        builder.append(type);
        builder.append(", instId=");
        builder.append(instId);
        builder.append(", instName=");
        builder.append(instName);
        builder.append("]");
        return builder.toString();
    }


}
