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

import java.sql.Types;
import java.util.Date;
import java.util.List;

import com.snowx.iam.entity.SynchroRelated;
import com.snowx.iam.entity.Synchronizers;
import com.snowx.iam.entity.idm.Organizations;
import com.snowx.iam.persistence.mapper.SynchroRelatedMapper;
import com.snowx.iam.persistence.service.SynchroRelatedService;
import com.snowx.iam.util.DateUtils;
import com.snowx.iam.persistence.mybatis.SnowxServiceImpl;
import org.springframework.stereotype.Repository;

@Repository
public class SynchroRelatedServiceImpl  extends SnowxServiceImpl<SynchroRelatedMapper,SynchroRelated,String> implements SynchroRelatedService{

    @Override
    public int updateSyncTime(SynchroRelated synchroRelated) {
        return getMapper().updateSyncTime(synchroRelated);
    }
    
    @Override
    public List<SynchroRelated> findOrgs(Synchronizers synchronizer) {
        return find(
                "instid = ? and syncid = ? and objecttype = ? ",
                 new Object[] { synchronizer.getInstId() ,synchronizer.getId(),Organizations.CLASS_TYPE},
                new int[] { Types.VARCHAR,Types.VARCHAR,Types.VARCHAR}
                );
    }
    
    @Override
    public SynchroRelated findByOriginId(Synchronizers synchronizer, String originId, String classType) {
        return findOne("instid = ? and syncId = ? and originid = ? and objecttype = ? ",
                 new Object[] { synchronizer.getInstId(),synchronizer.getId(),originId,classType },
                new int[] { Types.VARCHAR, Types.VARCHAR, Types.VARCHAR,Types.VARCHAR});
    }
    
    @Override
    public void updateSynchroRelated(Synchronizers synchronizer, SynchroRelated synchroRelated, String classType) {
        SynchroRelated loadSynchroRelated = 
                findByOriginId(
                        synchronizer,synchroRelated.getOriginId(),classType );
        if(loadSynchroRelated == null) {
            insert(synchroRelated);
        }else {
            synchroRelated.setId(loadSynchroRelated.getId());
            synchroRelated.setSyncTime(DateUtils.formatDateTime(new Date()));
            updateSyncTime(synchroRelated);
        }
    }
}
