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
 

package com.snowx.iam.web.auth.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.snowx.iam.authn.SignPrincipal;
import com.snowx.iam.authn.web.AuthorizationUtils;
import com.snowx.iam.entity.apps.Apps;
import com.snowx.iam.entity.history.HistoryLoginApps;
import com.snowx.iam.entity.idm.UserInfo;
import com.snowx.iam.persistence.service.AppsService;
import com.snowx.iam.persistence.service.HistoryLoginAppsService;
import com.snowx.iam.web.WebConstants;
import com.snowx.iam.web.WebContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.AsyncHandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.util.Date;

@Component
public class HistorySingleSignOnInterceptor  implements AsyncHandlerInterceptor  {
    private static final Logger logger = LoggerFactory.getLogger(HistorySingleSignOnInterceptor.class);

    @Autowired
    HistoryLoginAppsService historyLoginAppsService;

    @Autowired
    AppsService appsService;

    /**
     * postHandle .
     * @see org.springframework.web.servlet.handler.HandlerInterceptorAdapter#preHandle(
     *          javax.servlet.http.HttpServletRequest, 
     *          javax.servlet.http.HttpServletResponse, Object)
     */
    @Override
    public void postHandle(HttpServletRequest request,
            HttpServletResponse response,
            Object handler,ModelAndView modelAndView) throws Exception {
        logger.debug("postHandle");
       
        final Apps app = (Apps)WebContext.getAttribute(WebConstants.AUTHORIZE_SIGN_ON_APP);
        
        SignPrincipal principal = AuthorizationUtils.getPrincipal();
        if(principal != null && app !=null) {
            final UserInfo userInfo = principal.getUserInfo();
            String sessionId = principal.getSessionId();
             logger.debug("sessionId : {} , appId {}" , sessionId , app.getId());
             HistoryLoginApps historyLoginApps = new HistoryLoginApps();
             historyLoginApps.setAppId(app.getId());
             historyLoginApps.setSessionId(sessionId);
             historyLoginApps.setAppName(app.getAppName());
             historyLoginApps.setUserId(userInfo.getId());
             historyLoginApps.setUsername(userInfo.getUsername());
             historyLoginApps.setDisplayName(userInfo.getDisplayName());
             historyLoginApps.setInstId(userInfo.getInstId());
             historyLoginApps.setLoginTime(new Date());
             historyLoginAppsService.insert(historyLoginApps);
             WebContext.removeAttribute(WebConstants.CURRENT_SINGLESIGNON_URI);
             WebContext.removeAttribute(WebConstants.SINGLE_SIGN_ON_APP_ID);
        }
       
    }
}
