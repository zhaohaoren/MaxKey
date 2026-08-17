/*
 * Copyright [2024] [MaxKey of copyright http://www.maxkey.top]
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
 

package com.snowx.iam.persistence.service;

import com.snowx.iam.entity.apps.AppsJwtDetails;
import com.snowx.iam.persistence.mybatis.SnowxService;

public interface AppsJwtDetailsService  extends SnowxService<AppsJwtDetails,String>{

    public  AppsJwtDetails  get(String id , boolean cached) ;
}
