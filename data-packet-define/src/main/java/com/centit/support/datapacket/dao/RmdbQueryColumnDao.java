package com.centit.support.datapacket.dao;

import com.centit.framework.jdbc.dao.BaseDaoImpl;
import com.centit.support.datapacket.po.RmdbQueryColumn;
import org.springframework.stereotype.Repository;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Repository
public class RmdbQueryColumnDao extends BaseDaoImpl<RmdbQueryColumn, HashMap<String,Object>> {
    @Override
    public Map<String, String> getFilterField() {
        return null;
    }
}
