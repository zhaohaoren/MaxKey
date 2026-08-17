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
 

package com.snowx.iam.authz.cas.endpoint;

import com.snowx.iam.authn.session.SessionManager;
import com.snowx.iam.authz.cas.ticket.TicketServices;
import com.snowx.iam.authz.endpoint.AuthorizeBaseEndpoint;
import com.snowx.iam.http.HttpRequestAdapter;
import com.snowx.iam.http.HttpResponseAdapter;
import com.snowx.iam.persistence.service.AppsCasDetailsService;
import com.snowx.iam.persistence.service.UserInfoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

public class CasBaseAuthorizeEndpoint  extends AuthorizeBaseEndpoint{
    static final  Logger _logger = LoggerFactory.getLogger(CasBaseAuthorizeEndpoint.class);
    
    @Autowired
    protected AppsCasDetailsService casDetailsService;
    
    @Autowired
    protected UserInfoService userInfoService;
    
    @Autowired
    @Qualifier("casTicketServices")
    protected TicketServices ticketServices;
    
    @Autowired
    @Qualifier("casTicketGrantingTicketServices")
    protected TicketServices casTicketGrantingTicketServices;
    
    @Autowired
    protected SessionManager sessionManager;
    
    @Autowired
    @Qualifier("casProxyGrantingTicketServices")
    protected TicketServices casProxyGrantingTicketServices;
    
    @Autowired
    protected HttpResponseAdapter httpResponseAdapter;
    
    @Autowired
    protected HttpRequestAdapter httpRequestAdapter; 
    
}
