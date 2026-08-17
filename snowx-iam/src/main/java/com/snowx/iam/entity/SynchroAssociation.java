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
 

package com.snowx.iam.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import com.snowx.iam.persistence.mybatis.BaseEntity;

import java.io.Serializable;
import java.util.Date;

/**
 * @Description  
 * @Author  Hunter
 * @Date 2024-07-16 
 */
@TableName("MXK_SYNCHRO_ASSOCIATION")
public class SynchroAssociation extends BaseEntity implements Serializable {

    private static final long serialVersionUID =  6784822536779144306L;

    /**
     *
     * ID
     */
    @TableId(type = IdType.INPUT)
    private Long id;

    /**
     * 同步任务ID
     */
    @TableField
    private Long syncId;

    /**
     * 规则名
     */
       @TableField
    private String name;

    /**
     * 类型
     */
       @TableField
    private String objectType;

    /**
     * 目标字段
     */
       @TableField
    private String targetField;

    /**
     * 目标字段描述
     */
       @TableField
    private String targetFieldName;

    /**
     * 来源字段
     */
       @TableField
    private String sourceField;

    /**
     * 来源字段描述
     */
       @TableField
    private String sourceFieldName;

    /**
     * 描述
     */
       @TableField
    private String description;

    /**
     * 创建人
     */
       @TableField
    private Long createUser;

    /**
     * 创建时间
     */
       @TableField
    private Date createTime;

    /**
     * 修改人
     */
       @TableField
    private Long updateUser;

    /**
     * 修改时间
     */
       @TableField
    private Date updateTime;
       
   @TableField
   String instId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

	public Long getSyncId() {
		return syncId;
	}
	
    public void setSyncId(Long syncId) {
		this.syncId = syncId;
	}

	public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getObjectType() {
        return objectType;
    }

    public void setObjectType(String objectType) {
        this.objectType = objectType;
    }

    public String getTargetField() {
        return targetField;
    }

    public void setTargetField(String targetField) {
        this.targetField = targetField;
    }

    public String getTargetFieldName() {
        return targetFieldName;
    }

    public void setTargetFieldName(String targetFieldName) {
        this.targetFieldName = targetFieldName;
    }

    public String getSourceField() {
        return sourceField;
    }

    public void setSourceField(String sourceField) {
        this.sourceField = sourceField;
    }

    public String getSourceFieldName() {
        return sourceFieldName;
    }

    public void setSourceFieldName(String sourceFieldName) {
        this.sourceFieldName = sourceFieldName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getCreateUser() {
        return createUser;
    }

    public void setCreateUser(Long createUser) {
        this.createUser = createUser;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Long getUpdateUser() {
        return updateUser;
    }

    public void setUpdateUser(Long updateUser) {
        this.updateUser = updateUser;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

	public String getInstId() {
		return instId;
	}

	public void setInstId(String instId) {
		this.instId = instId;
	}
    
}
