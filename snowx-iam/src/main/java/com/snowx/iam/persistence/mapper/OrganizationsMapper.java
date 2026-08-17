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

import com.snowx.iam.entity.idm.Organizations;
import com.snowx.iam.persistence.mybatis.SnowxMapper;

public interface OrganizationsMapper extends SnowxMapper<Organizations,String> {

    public List<Organizations> queryOrgs(Organizations organization);
    
    boolean updateNamePath(Organizations organization);
}
