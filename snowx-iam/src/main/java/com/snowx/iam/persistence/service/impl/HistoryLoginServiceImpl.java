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
 

package com.snowx.iam.persistence.service.impl;

import org.apache.commons.lang3.StringUtils;
import com.snowx.iam.entity.history.HistoryLogin;
import com.snowx.iam.persistence.mapper.HistoryLoginMapper;
import com.snowx.iam.persistence.service.HistoryLoginService;
import com.snowx.iam.web.WebContext;
import com.snowx.iam.persistence.mybatis.PageResults;
import com.snowx.iam.persistence.mybatis.SnowxServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

@Repository
public class HistoryLoginServiceImpl  extends SnowxServiceImpl<HistoryLoginMapper,HistoryLogin,String> implements HistoryLoginService{
    private static Logger logger = LoggerFactory.getLogger(HistoryLoginServiceImpl.class);
    
    @Override
    public PageResults<HistoryLogin> queryOnlineSession(HistoryLogin historyLogin) {
        return this.fetchPageResults("queryOnlineSession",historyLogin);
    }
    
     @Override
     public void login(HistoryLogin historyLogin) {
            historyLogin.setId(WebContext.genId());
            if(StringUtils.isBlank(historyLogin.getInstId())) {
                historyLogin.setInstId("1");
            }
            //Thread insert 
            new Thread(new HistoryLoginRunnable(this,historyLogin)).start();
        }
        
        public class HistoryLoginRunnable implements Runnable{
            
            HistoryLoginService historyLoginService;
            
            HistoryLogin historyLogin;
            
            public HistoryLoginRunnable(HistoryLoginService historyLoginService, HistoryLogin historyLogin) {
                super();
                this.historyLoginService = historyLoginService;
                this.historyLogin = historyLogin;
            }

            @Override
            public void run() {
                logger.debug("History Login {}" , historyLogin);
                this.historyLoginService.insert(historyLogin);
            }
        }
}
