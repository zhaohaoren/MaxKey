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
 

package com.snowx.iam.web.auth.controller;

import com.snowx.iam.authn.annotation.CurrentUser;
import com.snowx.iam.entity.Message;
import com.snowx.iam.entity.history.HistoryLoginApps;
import com.snowx.iam.entity.idm.UserInfo;
import com.snowx.iam.persistence.service.HistoryLoginAppsService;
import com.snowx.iam.util.DateUtils;
import com.snowx.iam.persistence.mybatis.PageResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 单点登录日志查询
 * 
 * @author Crystal.sea
 *
 */

@Controller
@RequestMapping(value={"/historys"})
public class LoginAppsHistoryController {
    static final Logger logger = LoggerFactory.getLogger(LoginAppsHistoryController.class);
    
    @Autowired
      HistoryLoginAppsService historyLoginAppsService;
    
    /**
     * @param loginAppsHistory
     * @return
     */
    @GetMapping({"/loginAppsHistory/fetch"})
    @ResponseBody
    public Message<?> fetch(
                @ModelAttribute HistoryLoginApps historyLoginApp,
                @CurrentUser UserInfo currentUser){
        logger.debug("historys/loginAppsHistory/fetch/  {}",historyLoginApp);
        historyLoginApp.setId(null);
        historyLoginApp.setUserId(currentUser.getId());
        historyLoginApp.setInstId(currentUser.getInstId());
        return new Message<PageResults<HistoryLoginApps>>(
                    historyLoginAppsService.fetchPageResults(historyLoginApp)
                );
    }

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(DateUtils.FORMAT_DATE_HH_MM_SS);
        dateFormat.setLenient(false);  
        binder.registerCustomEditor(Date.class, new CustomDateEditor(dateFormat, true));
    }
}
