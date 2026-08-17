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
 

package com.snowx.iam.persistence.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;
import com.snowx.iam.entity.ChangePassword;
import com.snowx.iam.entity.idm.Organizations;
import com.snowx.iam.entity.idm.UserInfo;
import com.snowx.iam.persistence.mybatis.SnowxMapper;


/**
 * @author Crystal.Sea
 *
 */
public interface UserInfoMapper  extends SnowxMapper<UserInfo,String>{
    
    //login query
    public UserInfo findByAppIdAndUsername(UserInfo userInfo);
    
    public UserInfo findByUsername(@Param("username")String username);
    
    public UserInfo findByUsernameAndInstId(@Param("username")String username,@Param("instId")String instId);
    
    public UserInfo findByEmailMobile(String emailMobile);

    public UserInfo findByEmailAndInstId(@Param("email") String email, @Param("instId") String instId);
     
    public List<Organizations> findDeptsByUserId(String userId);
    
    public void updateLocked(UserInfo userInfo);

    public void updateLockout(UserInfo userInfo);

    public void badPasswordCount(UserInfo userInfo);
    
    public void badPasswordCountReset(UserInfo userInfo);
    
    public int     changePassword(ChangePassword changePassword);
    
    public int     updateAppLoginPassword(UserInfo userInfo);
    
    public int     updateProtectedApps(UserInfo userInfo);
    
    public int     updateSharedSecret(UserInfo userInfo);
    
    public int     updatePasswordQuestion(UserInfo userInfo);
    
    public int    updateAuthnType(UserInfo userInfo);
    
    public int     updateEmail(UserInfo userInfo);
    
    public int     updateMobile(UserInfo userInfo);
    
    public int     updateProfile(UserInfo userInfo);
    
    public int     updateGridList(UserInfo userInfo) ;
    
    public int     updateStatus(UserInfo userInfo) ;

    public int updatePasswordSetType(UserInfo userInfo);
}
