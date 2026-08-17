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
 

package me.zhyd.oauth.config;

import me.zhyd.oauth.request.AuthDefaultRequest;
import me.zhyd.oauth.request.AuthFeishu2Request;

public enum AuthMxkDefaultSource implements AuthSource {
     FEISHU2 {
            @Override
            public String authorize() {
                return "https://open.feishu.cn/open-apis/authen/v1/index";
            }

            @Override
            public String accessToken() {
                return "https://open.feishu.cn/open-apis/authen/v1/access_token";
            }

            @Override
            public String userInfo() {
                return "https://open.feishu.cn/open-apis/authen/v1/user_info";
            }

            @Override
            public String refresh() {
                return "https://open.feishu.cn/open-apis/authen/v1/refresh_access_token";
            }

            @Override
            public Class<? extends AuthDefaultRequest> getTargetClass() {
                return AuthFeishu2Request.class;
            }
        }

}
