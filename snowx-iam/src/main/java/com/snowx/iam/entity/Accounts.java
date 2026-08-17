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
 

package com.snowx.iam.entity;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

import com.snowx.iam.entity.idm.UserInfo;
import com.snowx.iam.persistence.mybatis.BaseEntity;
import org.hibernate.validator.constraints.Length;

import com.fasterxml.jackson.annotation.JsonIgnore;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
@TableName("MXK_ACCOUNTS")
public class Accounts extends BaseEntity implements Serializable {
    private static final long serialVersionUID = 6829592256223630307L;
    
    public  static final String DEFAULT_PASSWORD_SUFFIX = UserInfo.DEFAULT_PASSWORD_SUFFIX;
    
    @TableId(type = IdType.INPUT)
    private String id;
    @TableField
    private String userId;
    @TableField
    private String username;
    @TableField
    private String displayName;
    @TableField
    private String appId;
    @TableField
    private String appName;

    @Length(max = 60)
    @TableField
    private String relatedUsername;
    @TableField
    private String relatedPassword;
    @TableField
    private String createType;
    @TableField
    private String strategyId;
    @TableField
    private String strategyName;
    @TableField
    private int status;
    
    @TableField
    private String instId;
    
    @TableField(exist = false)
    private String instName;
    
    @TableField(exist = false)
    UserInfo userInfo;
    
    @JsonIgnore
    @TableField(exist = false)
    private HashMap<String,OrganizationsCast> orgCast =new HashMap<>();

    public Accounts() {
        super();
    }

    public Accounts(String id) {
        this.id = id;
    }

    public Accounts(String userId, String appId) {
        this.userId = userId;
        this.appId = appId;
    }

    public Accounts(String userId, String appId, String password) {
        this.userId = userId;
        this.appId = appId;
        this.relatedPassword = password;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getRelatedUsername() {
        return relatedUsername;
    }

    public void setRelatedUsername(String relatedUsername) {
        this.relatedUsername = relatedUsername;
    }

    public String getRelatedPassword() {
        return relatedPassword;
    }

    public void setRelatedPassword(String relatedPassword) {
        this.relatedPassword = relatedPassword;
    }


    public String getId() {
        return id;
    }


    public void setId(String id) {
        this.id = id;
    }

    public UserInfo getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(UserInfo userInfo) {
        this.userInfo = userInfo;
    }

    
    public String getCreateType() {
        return createType;
    }

    public void setCreateType(String createType) {
        this.createType = createType;
    }

    public String getStrategyId() {
        return strategyId;
    }

    public void setStrategyId(String strategyId) {
        this.strategyId = strategyId;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
    
    public HashMap<String, OrganizationsCast> getOrgCast() {
        return orgCast;
    }

    public void setOrgCast(HashMap<String, OrganizationsCast> orgCast) {
        this.orgCast = orgCast;
    }
    
    public void setOrgCast(List <OrganizationsCast> listOrgCast) {
        for(OrganizationsCast cast : listOrgCast) {
            this.orgCast.put(cast.getProvider(), cast);
        }
    }

    public String getStrategyName() {
        return strategyName;
    }

    public void setStrategyName(String strategyName) {
        this.strategyName = strategyName;
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

    @Override
    public String toString() {
        return "AppAccounts [uid=" + userId + ", username=" + username + ", displayName=" + displayName + ", appId="
                + appId + ", appName=" + appName + ", relatedUsername=" + relatedUsername + ", relatedPassword="
                + relatedPassword + "]";
    }

}
