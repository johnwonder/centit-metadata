package com.centit.product.dbdesign.dao;

import com.centit.framework.core.dao.CodeBook;
import com.centit.framework.jdbc.dao.BaseDaoImpl;
import com.centit.framework.jdbc.dao.DatabaseOptUtils;
import com.centit.product.dbdesign.po.PendingMetaRelation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;


/**
 * PendingMdRelationDao  Repository.
 * create by scaffold 2016-06-01

 * 未落实表关联关系表null
*/

@Repository
public class PendingMetaRelationDao extends BaseDaoImpl<PendingMetaRelation, String>
    {

    public static final Log log = LogFactory.getLog(PendingMetaRelationDao.class);

    @Override
    public Map<String, String> getFilterField() {
        if( filterField == null){
            filterField = new HashMap<String, String>();

            filterField.put("relationId" , CodeBook.EQUAL_HQL_ID);


            filterField.put("parentTableId" , CodeBook.EQUAL_HQL_ID);

            filterField.put("childTableId" , CodeBook.EQUAL_HQL_ID);

            filterField.put("relationName" , CodeBook.EQUAL_HQL_ID);

            filterField.put("relationState" , CodeBook.EQUAL_HQL_ID);

            filterField.put("relationComment" , CodeBook.EQUAL_HQL_ID);

            filterField.put("lastModifyDate" , CodeBook.EQUAL_HQL_ID);

            filterField.put("recorder" , CodeBook.EQUAL_HQL_ID);

        }
        return filterField;
    }

        public Long getNextKey(){
            return DatabaseOptUtils.getSequenceNextValue(this, "SEQ_PENDINGRELATIONID");
        }
    }
