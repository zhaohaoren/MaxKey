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

import java.util.concurrent.TimeUnit;

import com.snowx.iam.entity.apps.AppsCasDetails;
import com.snowx.iam.persistence.mapper.AppsCasDetailsMapper;
import com.snowx.iam.persistence.service.AppsCasDetailsService;
import com.snowx.iam.persistence.mybatis.SnowxServiceImpl;
import org.springframework.stereotype.Repository;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

@Repository
public class AppsCasDetailsServiceImpl  extends SnowxServiceImpl<AppsCasDetailsMapper,AppsCasDetails,String> implements AppsCasDetailsService{

    protected static final   Cache<String, AppsCasDetails> detailsCache = 
            Caffeine.newBuilder()
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .maximumSize(200000)
                .build();
    
    @Override
    public  AppsCasDetails  get(String id , boolean cached) {
        AppsCasDetails details = null;
        if(cached) {
            details = detailsCache.getIfPresent(id);
            if(details == null) {
                details = getMapper().getAppDetails(id);
                if(details != null) {
                    detailsCache.put(id, details);
                }
            }
        }else {
            details = getMapper().getAppDetails(id);
        }
        return details;
    }
}
