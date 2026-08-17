/*
 * Copyright [2025] [MaxKey of copyright http://www.maxkey.top]
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

import com.snowx.iam.entity.SynchroAssociation;

import com.snowx.iam.persistence.mapper.SynchroAssociationMapper;
import com.snowx.iam.persistence.service.SynchroAssociationService;
import com.snowx.iam.persistence.mybatis.SnowxServiceImpl;
import org.springframework.stereotype.Repository;

import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

@Repository
public class SynchroAssociationServiceImpl extends SnowxServiceImpl<SynchroAssociationMapper,SynchroAssociation,String>  implements SynchroAssociationService {

    @Override
    public List<SynchroAssociation> findBySyncIdAndObjectType(Long syncId, String objectType) {

        return getMapper().findBySyncIdAndObjectType(syncId,objectType);
    }

    @Override
    public void deleteFieldMapById(Long id){
        ArrayList<String> ids = new ArrayList<>();
        ids.add(String.valueOf(id));
        super.deleteBatch(ids);
    }

    @Override
    public List<SynchroAssociation> findBySyncId(Long syncId) {
        List<SynchroAssociation> list = find(" syncId = ? ",
                new Object[]{syncId},
                new int[]{Types.BIGINT,Types.VARCHAR});
        return list;

    }
    
    @Override
    public List<SynchroAssociation> findBySyncId(Long syncId, String instId) {
        List<SynchroAssociation> list = find(" syncId = ? and instId = ? ",
                new Object[]{syncId,instId},
                new int[]{Types.BIGINT,Types.VARCHAR});
        return list;

    }


}
