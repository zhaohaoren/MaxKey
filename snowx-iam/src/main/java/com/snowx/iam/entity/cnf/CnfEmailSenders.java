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
 

package com.snowx.iam.entity.cnf;



import java.io.Serializable;
import java.util.Date;

import com.snowx.iam.persistence.mybatis.BaseEntity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
@TableName("MXK_CNF_EMAIL_SENDERS")
public class CnfEmailSenders  extends BaseEntity  implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 3689854324601731505L;
    
    @TableId(type = IdType.INPUT)
    private String id;
    
    @TableField
    private String account;
    
    @TableField
    private String credentials;
    
    @TableField
    private String smtpHost;
    
    @TableField
    private Integer port;
    
    @TableField
    private int sslSwitch;
    
    @TableField
    private String sender;
    
    @TableField
    private String encoding;
    
    @TableField
    private String protocol;
    
    @TableField
    private int status;
    
    @TableField
    private String instId;
    
    @TableField(exist = false)
    private String instName;
    
    @TableField
    private String description;
    
    @TableField
    private String createdBy;
    
    @TableField
    private Date createdDate;
    
    @TableField
    private String modifiedBy;
    
    @TableField
    private Date modifiedDate;

    public CnfEmailSenders() {
        super();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getCredentials() {
        return credentials;
    }

    public void setCredentials(String credentials) {
        this.credentials = credentials;
    }

    public String getSmtpHost() {
        return smtpHost;
    }

    public void setSmtpHost(String smtpHost) {
        this.smtpHost = smtpHost;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }


    public int getSslSwitch() {
        return sslSwitch;
    }

    public void setSslSwitch(int sslSwitch) {
        this.sslSwitch = sslSwitch;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
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

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("EmailSenders [id=");
        builder.append(id);
        builder.append(", account=");
        builder.append(account);
        builder.append(", credentials=");
        builder.append(credentials);
        builder.append(", smtpHost=");
        builder.append(smtpHost);
        builder.append(", port=");
        builder.append(port);
        builder.append(", sslSwitch=");
        builder.append(sslSwitch);
        builder.append(", sender=");
        builder.append(sender);
        builder.append(", status=");
        builder.append(status);
        builder.append(", instId=");
        builder.append(instId);
        builder.append(", instName=");
        builder.append(instName);
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
        builder.append("]");
        return builder.toString();
    }
       
}
