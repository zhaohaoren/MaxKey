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
 

package com.snowx.iam.entity.permissions;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import com.snowx.iam.persistence.mybatis.BaseEntity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import org.hibernate.validator.constraints.Length;
@TableName("MXK_ROLES")
public class Roles extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 4660258495864814777L;
    
    public class Category{
        public static final String DYNAMIC = "dynamic";
        
        public static final String STATIC  = "static";
        
        public static final String APP     = "app";
    }
    
    @TableId(type = IdType.INPUT)
    String id;

    @Length(max = 60)
    @TableField
    String roleCode;
    
    @Length(max = 60)
    @TableField
    String roleName;
    
    @TableField
    String category;
    
    @TableField
    String orgIdsList;
    
    @TableField
    int isdefault;
    @TableField
    String description;
    @TableField
    String createdBy;
    @TableField
    Date createdDate;
    @TableField
    String modifiedBy;
    @TableField
    Date modifiedDate;
    @TableField
    int status;
    
    @TableField
    String appId;
    
    @TableField
    private String instId;

    @TableField(exist = false)
    private String instName;
    
    @TableField(exist = false)
    List<String> orgIds;

    public Roles() {
    }

    public Roles(String id) {
        this.id = id;
    }

    /**
     * Groups.
     * @param id String
     * @param name String
     * @param isdefault int
     */
    public Roles(String id,String roleCode, String roleName, int isdefault,String appId) {
        super();
        this.id = id;
        this.roleCode = roleCode;
        this.roleName = roleName;
        this.isdefault = isdefault;
        this.appId = appId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }
    
    public int getIsdefault() {
        return isdefault;
    }

    public void setIsdefault(int isdefault) {
        this.isdefault = isdefault;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public Date getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(Date modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    /**
     * ROLE_ALL_USER must be 
     *         1, dynamic 
     *         2, all orgIdsList 
     *        3, not filters
     */
    public void setDefaultAllUser() {
        this.category = Category.DYNAMIC;
        this.orgIdsList ="";
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getOrgIdsList() {
        return orgIdsList;
    }

    public void setOrgIdsList(String orgIdsList) {
        this.orgIdsList = orgIdsList;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getInstId() {
        return instId;
    }

    public void setInstId(String instId) {
        this.instId = instId;
    }

    public String getInstName() {
        return instName;
    }

    public void setInstName(String instName) {
        this.instName = instName;
    }

    public List<String> getOrgIds() {
		return orgIds;
	}

	public void setOrgIds(List<String> orgIds) {
		this.orgIds = orgIds;
	}

	@Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Roles [id=");
        builder.append(id);
        builder.append(", roleCode=");
        builder.append(roleCode);
        builder.append(", roleName=");
        builder.append(roleName);
        builder.append(", category=");
        builder.append(category);
        builder.append(", orgIdsList=");
        builder.append(orgIdsList);
        builder.append(", isdefault=");
        builder.append(isdefault);
        builder.append(", description=");
        builder.append(description);
        builder.append(", createdBy=");
        builder.append(createdBy);
        builder.append(", createdDate=");
        builder.append(createdDate);
        builder.append(", modifiedBy=");
        builder.append(modifiedBy);
        builder.append(", modifiedDate=");
        builder.append(modifiedDate);
        builder.append(", status=");
        builder.append(status);
        builder.append(", instId=");
        builder.append(instId);
        builder.append(", instName=");
        builder.append(instName);
        builder.append("]");
        return builder.toString();
    }

}
