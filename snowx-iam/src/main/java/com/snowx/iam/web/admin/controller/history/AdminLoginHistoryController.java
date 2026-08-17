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
 

package com.snowx.iam.web.admin.controller.history;

import com.snowx.iam.authn.annotation.CurrentUser;
import com.snowx.iam.entity.Message;
import com.snowx.iam.entity.history.HistoryLogin;
import com.snowx.iam.entity.idm.UserInfo;
import com.snowx.iam.persistence.service.HistoryLoginService;
import com.snowx.iam.util.DateUtils;
import com.snowx.iam.persistence.mybatis.PageResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 登录日志查询
 * 
 * @author Crystal.sea
 *
 */

@RestController
@RequestMapping(value={"/admin/historys"})
public class AdminLoginHistoryController {
    static final Logger logger = LoggerFactory.getLogger(AdminLoginHistoryController.class);
    
    @Autowired
    HistoryLoginService loginHistoryService;
    
    /**
     * @param HistoryLogin
     * @return
     */
    @GetMapping({"/loginHistory/fetch"})
    @ResponseBody
    public Message<?> fetch(
                @ModelAttribute HistoryLogin historyLogin,
                @CurrentUser UserInfo currentUser
            ){
        logger.debug("historys/loginHistory/fetch/ {}",historyLogin);
        historyLogin.setInstId(currentUser.getInstId());
        return new Message<PageResults<HistoryLogin>>(
                    loginHistoryService.fetchPageResults(historyLogin)
                );
    }
    
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(DateUtils.FORMAT_DATE_HH_MM_SS);
        dateFormat.setLenient(false);  
        binder.registerCustomEditor(Date.class, new CustomDateEditor(dateFormat, true));
    }
}
