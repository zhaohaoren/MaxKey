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

import org.apache.commons.lang3.StringUtils;
import com.snowx.iam.constants.ConstsStatus;
import com.snowx.iam.crypto.password.PasswordReciprocal;
import com.snowx.iam.entity.Accounts;
import com.snowx.iam.entity.idm.UserInfo;
import com.snowx.iam.persistence.mapper.AccountsMapper;
import com.snowx.iam.persistence.service.AccountsService;
import com.snowx.iam.persistence.service.OrganizationsCastService;
import com.snowx.iam.persistence.service.UserInfoService;
import com.snowx.iam.persistence.mybatis.SnowxServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

@Repository
public class AccountsServiceImpl  extends SnowxServiceImpl<AccountsMapper,Accounts,String>  implements AccountsService{
  
    @Autowired
    UserInfoService  userInfoService;
    
    @Autowired
    OrganizationsCastService organizationsCastService;
    
    @Override
     public boolean insert(Accounts account) {
         if (super.insert(account)) {
                return true;
            }
         return false;
     }
     
    @Override
    public boolean update(Accounts account) {
         if (super.update(account)) {
                return true;
            }
         return false;
     }
   
   @Override
   public boolean updateStatus(Accounts accounts) {
       return this.getMapper().updateStatus(accounts) > 0;
   }
   
   @Override
   public boolean remove(String id) {
       if (super.delete(id)) {
              return true;
          }
       return false;
   }
   
   
   @Override
   public List<Accounts> queryByAppIdAndDate(Accounts account) {
       return getMapper().queryByAppIdAndDate(account);
   }
   
   @Override
   public List<Accounts> queryByAppIdAndAccount(String appId, String relatedUsername){
       return getMapper().queryByAppIdAndAccount(appId,relatedUsername);
   }
   
    public static String getPinYinName(String name) throws BadHanyuPinyinOutputFormatCombination {
        HanyuPinyinOutputFormat pinyinFormat = new        HanyuPinyinOutputFormat();
        pinyinFormat.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        pinyinFormat.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        pinyinFormat.setVCharType(HanyuPinyinVCharType.WITH_V);
        return PinyinHelper.toHanYuPinyinString(name, pinyinFormat, "",false);
    }
    
    public static String getPinYinShortName(String name) throws BadHanyuPinyinOutputFormatCombination {
        char[] strs = name.toCharArray();
        String pinyinName = "";
        for(int i=0;i<strs.length;i++) {
            if(i == 0) {
                pinyinName += getPinYinName(strs[i]+"");
            }else {
                pinyinName += getPinYinName(strs[i]+"").charAt(0);
            }
        }
        return pinyinName;
    }
    
}
