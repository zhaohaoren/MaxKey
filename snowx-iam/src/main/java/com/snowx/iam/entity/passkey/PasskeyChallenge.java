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

package com.snowx.iam.entity.passkey;

import java.io.Serializable;
import java.util.Date;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.snowx.iam.persistence.mybatis.BaseEntity;

/**
 * Passkey挑战实体类，用于存储注册和认证过程中的挑战信息
 */
@TableName("mxk_passkey_challenges")
public class PasskeyChallenge extends BaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.INPUT)
    private String id;

    @TableField("user_id")
    private String userId;

    @TableField("challenge")
    private String challenge;

    @TableField("challenge_type")
    private String challengeType; // REGISTRATION 或 AUTHENTICATION

    @TableField("session_id")
    private String sessionId;

    @TableField("created_date")
    private Date createdDate;

    @TableField("expires_date")
    private Date expiresDate;

    @TableField("status")
    private Integer status = 0; // 0: 未使用, 1: 已使用, 2: 已过期

    @TableField("inst_id")
    private String instId;

    // 构造函数
    public PasskeyChallenge() {
        Date now = new Date();
        this.createdDate = now;
        this.expiresDate = new Date(now.getTime() + 5 * 60 * 1000); // 5分钟过期
    }

    public PasskeyChallenge(String id, String challenge, String challengeType) {
        this.id = id;
        this.challenge = challenge;
        this.challengeType = challengeType;
        Date now = new Date();
        this.createdDate = now;
        this.expiresDate = new Date(now.getTime() + 5 * 60 * 1000);
    }

    // Getter和Setter方法
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getChallenge() {
        return challenge;
    }

    public void setChallenge(String challenge) {
        this.challenge = challenge;
    }

    public String getChallengeType() {
        return challengeType;
    }

    public void setChallengeType(String challengeType) {
        this.challengeType = challengeType;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public Date getExpiresDate() {
        return expiresDate;
    }

    public void setExpiresDate(Date expiresDate) {
        this.expiresDate = expiresDate;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getInstId() {
        return instId;
    }

    public void setInstId(String instId) {
        this.instId = instId;
    }

    // 检查是否过期
    public boolean isExpired() {
        return new Date().after(this.expiresDate);
    }

    @Override
    public String toString() {
        return "PasskeyChallenge{" +
                "id='" + id + '\'' +
                ", userId='" + userId + '\'' +
                ", challengeType='" + challengeType + '\'' +
                ", sessionId='" + sessionId + '\'' +
                ", createdDate=" + createdDate +
                ", expiresDate=" + expiresDate +
                ", status=" + status +
                ", instId='" + instId + '\'' +
                '}';
    }
}
