package com.centit.support.metadata.service.impl;

import com.centit.framework.common.ObjectException;
import com.centit.framework.ip.po.DatabaseInfo;
import com.centit.framework.ip.service.IntegrationEnvironment;
import com.centit.support.algorithm.CollectionsOpt;
import com.centit.support.database.metadata.*;
import com.centit.support.database.utils.DBType;
import com.centit.support.database.utils.PageDesc;
import com.centit.support.metadata.dao.MetaColumnDao;
import com.centit.support.metadata.dao.MetaRelationDao;
import com.centit.support.metadata.dao.MetaTableDao;
import com.centit.support.metadata.po.MetaColumn;
import com.centit.support.metadata.po.MetaRelation;
import com.centit.support.metadata.po.MetaTable;
import com.centit.support.metadata.service.MetaDataService;
import com.centit.support.metadata.utils.JdbcConnect;
import com.centit.support.metadata.vo.MetaTableCascade;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional
public class MetaDataServiceImpl implements MetaDataService {
    private Logger logger = LoggerFactory.getLogger("元数据");

    @Autowired
    private IntegrationEnvironment integrationEnvironment;

    @Autowired
    private MetaTableDao metaTableDao;

    @Autowired
    private MetaColumnDao metaColumnDao;

    @Autowired
    private MetaRelationDao metaRelationDao;

    @Override
    public List<DatabaseInfo> listDatabase() {
        return integrationEnvironment.listDatabaseInfo();
    }

    @Override
    public List<MetaTable> listMetaTables(String databaseCode, PageDesc pageDesc) {
        return metaTableDao.listObjectsByProperties(CollectionsOpt.createHashMap("databaseCode", databaseCode), pageDesc);
    }

    @Override
    public List<SimpleTableInfo> listRealTables(String databaseCode) {
        DatabaseInfo databaseInfo = integrationEnvironment.getDatabaseInfo(databaseCode);
        JdbcMetadata jdbcMetadata = new JdbcMetadata();
        try {
            jdbcMetadata.setDBConfig(JdbcConnect.getConn(databaseInfo));
        }catch (SQLException e){
            logger.error("连接数据库【{}】出错",databaseInfo.getDatabaseName());
            throw new ObjectException("连接数据库出错");
        }
        return jdbcMetadata.listAllTable();
    }

    @Override
    public void syncDb(String databaseCode, String recorder){
        List<SimpleTableInfo> dbTables = listRealTables(databaseCode);
        List<MetaTable> metaTables = metaTableDao.listObjectsByFilter("where DATABASE_CODE = ?", new Object[]{databaseCode});
        Comparator<TableInfo> comparator = (o1, o2) -> StringUtils.compare(o1.getTableName(), o2.getTableName());
        Triple<List<SimpleTableInfo>, List<Pair<MetaTable, SimpleTableInfo>>, List<MetaTable>> triple = compareMetaBetweenDbTables(metaTables, dbTables, comparator);
        if(triple.getLeft() != null && triple.getLeft().size() > 0){
            //新增
            for(SimpleTableInfo table : triple.getLeft()){
                //表
                MetaTable metaTable = new MetaTable().convertFromDbTable(table);
                metaTable.setDatabaseCode(databaseCode);
                metaTable.setRecorder(recorder);
                metaTableDao.saveNewObject(metaTable);
                //列
                List<SimpleTableField> columns = table.getColumns();
                for(SimpleTableField tableField : columns){
                    MetaColumn column = new MetaColumn().convertFromTableField(tableField);
                    column.setTableId(metaTable.getTableId());
                    metaColumnDao.saveNewObject(column);
                }
            }
        }
        if(triple.getRight() != null && triple.getRight().size() > 0) {
            //删除
            for (MetaTable table : triple.getRight()) {
                metaTableDao.deleteObjectReferences(table);
                metaTableDao.deleteObject(table);
            }
        }
        if(triple.getMiddle() != null && triple.getMiddle().size() > 0){
            //更新
            for(Pair<MetaTable, SimpleTableInfo> pair : triple.getMiddle()){
                MetaTable oldTable = pair.getLeft();
                oldTable.setRecorder(recorder);
                SimpleTableInfo newTable = pair.getRight();
                //表
                metaTableDao.updateObject(oldTable.convertFromDbTable(newTable));
                //列
                oldTable = metaTableDao.fetchObjectReferences(oldTable);
                List<MetaColumn> oldColumns = oldTable.getColumns();
                List<SimpleTableField> newColumns = newTable.getColumns();
                Comparator<TableField> columnComparator = (o1, o2) -> StringUtils.compare(o1.getColumnName(), o2.getColumnName());
                Triple<List<SimpleTableField>, List<Pair<MetaColumn, SimpleTableField>>, List<MetaColumn>> columnCompared = compareMetaBetweenDbTables(oldColumns, newColumns, columnComparator);
                if(columnCompared.getLeft() != null && columnCompared.getLeft().size() > 0){
                    //新增
                    for(SimpleTableField tableField : columnCompared.getLeft()){
                        MetaColumn metaColumn = new MetaColumn().convertFromTableField(tableField);
                        metaColumn.setTableId(oldTable.getTableId());
                        metaColumn.setRecorder(recorder);
                        metaColumnDao.saveNewObject(metaColumn);
                    }
                }
                if(columnCompared.getRight() != null && columnCompared.getRight().size() > 0){
                    //删除
                    for(MetaColumn metaColumn : columnCompared.getRight()){
                        metaColumnDao.deleteObject(metaColumn);
                    }
                }
                if(columnCompared.getMiddle() != null && columnCompared.getMiddle().size() > 0){
                    //更新
                    for(Pair<MetaColumn, SimpleTableField> columnPair : columnCompared.getMiddle()){
                        MetaColumn oldColumn = columnPair.getLeft();
                        oldColumn.setRecorder(recorder);
                        SimpleTableField newColumn = columnPair.getRight();
                        metaColumnDao.updateObject(oldColumn.convertFromTableField(newColumn));
                    }
                }
            }
        }
    }

    private <K,V> Triple<List<K>, List<Pair<V, K>>, List<V>>
            compareMetaBetweenDbTables(List<V> metaTables, List<K> simpleTableInfos, Comparator comparator){
        if(metaTables==null ||metaTables.size()==0)
            return new ImmutableTriple<> (
                simpleTableInfos,null,null);
        if(simpleTableInfos==null ||simpleTableInfos.size()==0)
            return new ImmutableTriple<> (
                null,null,metaTables);
        List<V> oldList = CollectionsOpt.cloneList(metaTables);
        List<K> newList = CollectionsOpt.cloneList(simpleTableInfos);
        Collections.sort(oldList, comparator);
        Collections.sort(newList, comparator);
        //---------------------------------------
        int i=0; int sl = oldList.size();
        int j=0; int dl = newList.size();
        List<K> insertList = new ArrayList<>();
        List<V> delList = new ArrayList<>();
        List<Pair<V,K>> updateList = new ArrayList<>();
        while(i<sl&&j<dl){
            int n = comparator.compare(oldList.get(i), newList.get(j));
            if(n<0){
                delList.add(oldList.get(i));
                i++;
            }else if(n==0){
                updateList.add( new ImmutablePair<>(oldList.get(i),newList.get(j)));
                i++;
                j++;
            }else {
                insertList.add(newList.get(j));
                j++;
            }
        }

        while(i<sl){
            delList.add(oldList.get(i));
            i++;
        }

        while(j<dl){
            insertList.add(newList.get(j));
            j++;
        }

        return new ImmutableTriple<>(insertList,updateList,delList);
    }

    @Override
    public void updateMetaTable(String tableId, String tableName, String tableComment, String tableState, String recorder) {
        MetaTable metaTable = metaTableDao.getObjectById(tableId);
        metaTable.setTableComment(tableComment);
        metaTable.setTableLabelName(tableName);
        metaTable.setTableState(tableState);
        metaTable.setRecorder(recorder);
        metaTableDao.updateObject(metaTable);
    }

    @Override
    public MetaTable getMetaTable(String tableId) {
        return metaTableDao.getObjectById(tableId);
    }

    @Override
    public MetaTable getMetaTable(String databaseCode, String tableName){
        return metaTableDao.getMetaTable(databaseCode,tableName);
    }

    @Override
    public List<MetaRelation> listMetaRelation(String tableId, PageDesc pageDesc) {
        List<MetaRelation> list = metaRelationDao.listObjectsByProperty("parentTableId", tableId);
        for(MetaRelation relation : list){
            metaRelationDao.fetchObjectReferences(relation);
        }
        return list;
    }

    @Override
    public List<MetaColumn> listMetaColumns(String tableId, PageDesc pageDesc) {
        return metaColumnDao.listObjectsByProperties(
            CollectionsOpt.createHashMap("tableId", tableId), pageDesc);
    }

    @Override
    public void createRelation(MetaRelation relation) {
        metaRelationDao.saveNewObject(relation);
        metaRelationDao.saveObjectReferences(relation);
    }

    @Override
    public void saveRelations(String tableId, List<MetaRelation> relations) {
        List<MetaRelation> dbRelations = metaRelationDao.listObjectsByProperty("parentTableId", tableId);

        Triple<List<MetaRelation>, List<Pair<MetaRelation,MetaRelation>>, List<MetaRelation>> comparedRelation =
            CollectionsOpt.compareTwoList(dbRelations, relations,
                (o1, o2) -> StringUtils.compare(o1.getChildTableId(), o2.getChildTableId()));

        if(comparedRelation.getLeft() != null){
            //insert
            for(MetaRelation relation : comparedRelation.getLeft()){
                metaRelationDao.saveNewObject(relation);
                metaRelationDao.saveObjectReferences(relation);
            }
        }

        if(comparedRelation.getRight() != null){
            //delete
            for(MetaRelation relation : comparedRelation.getRight()){
                relation = metaRelationDao.fetchObjectReferences(relation);
                metaRelationDao.deleteObject(relation);
                metaRelationDao.deleteObjectReferences(relation);
            }
        }

        if(comparedRelation.getMiddle() != null){
            //update
            for(Pair<MetaRelation, MetaRelation> pair : comparedRelation.getMiddle()){
                MetaRelation oldRelation = pair.getLeft();
                oldRelation = metaRelationDao.fetchObjectReferences(oldRelation);
                MetaRelation newRelation = pair.getRight();
                oldRelation.setRelationName(newRelation.getRelationName());
                oldRelation.setRelationComment(newRelation.getRelationComment());
                metaRelationDao.updateObject(oldRelation);

                metaRelationDao.deleteObjectReferences(oldRelation);
                newRelation.setRelationId(oldRelation.getRelationId());
                metaRelationDao.saveObjectReferences(newRelation);
            }
        }
    }

    @Override
    public MetaColumn getMetaColumn(String tableId, String columnName) {
        return metaColumnDao.getObjectById(new MetaColumn(tableId, columnName));
    }

    @Override
    public void updateMetaColumn(MetaColumn metaColumn) {
        metaColumnDao.updateObject(metaColumn);
    }

    @Override
    public MetaTableCascade getMetaTableCascade(String databaseCode, String tableCode) {
        MetaTableCascade tableCascade = new MetaTableCascade();
        DatabaseInfo dbInfo = integrationEnvironment.getDatabaseInfo(databaseCode);
        DBType dbType = DBType.mapDBType(dbInfo.getDatabaseUrl());
        tableCascade.setDatabaseType(dbType.toString());
        MetaTable metaTable = metaTableDao.getObjectByProperties(
            CollectionsOpt.createHashMap("databaseCode", databaseCode, "tableName", tableCode));
        tableCascade.addTable(metaTable);
        metaTableDao.fetchObjectReferences(metaTable);
        for(MetaRelation relation :metaTable.getMdRelations()){
            String childTableId = relation.getChildTableId();
            MetaTable childTable = metaTableDao.getObjectById(childTableId);

            metaRelationDao.fetchObjectReferences(relation);
            tableCascade.addTable(childTable, relation.getRelationDetails());
        }
        tableCascade.setTableFields(metaTable.getMdColumns());

        return tableCascade;
    }
}
