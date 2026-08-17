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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.snowx.iam.entity.dto.InstDto;
import com.snowx.iam.persistence.mapper.ReportMapper;
import com.snowx.iam.persistence.service.ReportService;
import org.springframework.stereotype.Repository;

@Repository
public class ReportServiceImpl implements ReportService {

    private final ReportMapper reportMapper;

    public ReportServiceImpl(ReportMapper reportMapper) {
        this.reportMapper = reportMapper;
    }

    @Override
    public Integer analysisDayCount(InstDto inst) {
        return reportMapper.analysisDayCount(inst);
    }
    
    @Override
    public Integer analysisNewUsers(InstDto inst) {
        return reportMapper.analysisNewUsers(inst);
    }
    
    @Override
    public Integer analysisOnlineUsers(InstDto inst) {
        return reportMapper.analysisOnlineUsers(inst);
    }
    
    @Override
    public Integer analysisActiveUsers(InstDto inst) {
        return reportMapper.analysisActiveUsers(inst);
    }
    
    @Override
    public Integer totalUsers(InstDto inst) {
        return reportMapper.totalUsers(inst);
    }
    
    @Override
    public Integer totalDepts(InstDto inst) {
        return reportMapper.totalDepts(inst);
    }
    
    @Override
    public Integer totalApps(InstDto inst) {
        return reportMapper.totalApps(inst);
    }
    
    @Override
    public List<Map<String,Object>> analysisDayHour(InstDto inst){
        return reportMapper.analysisDayHour(inst);
    }
    
    @Override
    public List<Map<String,Object>> analysisMonth(InstDto inst){
        return reportMapper.analysisMonth(inst);
    }
    
    
    @Override
    public List<Map<String,Object>> analysisBrowser(InstDto inst){
        return reportMapper.analysisBrowser(inst);
    }
    
    @Override
    public List<Map<String,Object>> analysisApp(InstDto inst){
        return reportMapper.analysisApp(inst);
    }
    
    @Override
    public List<Map<String,Object>> analysisProvince(InstDto inst){
        List<Map<String,Object>> maps = reportMapper.analysisProvince(inst);
        if(null == maps) {
            return new ArrayList<>();
        }
        for(Map<String,Object> map : maps) {
            if(map.containsKey("reportstring")){
                String name = map.get("reportstring").toString();
                if (name.endsWith("省")
                        || name.endsWith("市")
                        || name.endsWith("特别行政区")
                        || name.endsWith("自治区")) {
                    name = name.replace("省","")
                            .replace("市","")
                            .replace("特别行政区","")
                            .replace("自治区","");
                }
                map.put("name",name);
            }
        }
        return maps;
    }
    
    @Override
    public List<Map<String,Object>> analysisCountry(InstDto inst){
        return reportMapper.analysisCountry(inst);
    }

	@Override
	public Integer analysisMonthCount(InstDto inst) {
		return reportMapper.analysisMonthCount(inst);
	}

	@Override
	public Integer totalGroups(InstDto inst) {
		return reportMapper.totalGroups(inst);
	}
    
}
