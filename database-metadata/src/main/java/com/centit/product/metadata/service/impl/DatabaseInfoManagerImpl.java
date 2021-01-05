package com.centit.product.metadata.service.impl;

import com.alibaba.fastjson.JSONArray;

import com.centit.framework.jdbc.service.BaseEntityManagerImpl;
import com.centit.product.metadata.dao.DatabaseInfoDao;
import com.centit.product.metadata.po.DatabaseInfo;
import com.centit.product.metadata.service.DatabaseInfoManager;
import com.centit.support.database.utils.PageDesc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service("databaseInfoManager")
@Transactional
public class DatabaseInfoManagerImpl extends BaseEntityManagerImpl<DatabaseInfo, String, DatabaseInfoDao>
        implements DatabaseInfoManager {

    //private static final SysOptLog sysOptLog = SysOptLogFactoryImpl.getSysOptLog();

    @Override
    @Autowired
    public void setBaseDao(DatabaseInfoDao baseDao) {
        super.baseDao = baseDao;
    }

    @Override
    public boolean connectionTest(DatabaseInfo databaseInfo) {
        return baseDao.connectionTest(databaseInfo);
    }

    @Override
    public List<DatabaseInfo> listDatabase() {
        List<DatabaseInfo> database = baseDao.listDatabase();
        return database;
    }

    @Override
    public void saveNewObject(DatabaseInfo databaseInfo) {
        baseDao.saveNewObject(databaseInfo);
    }

    @Override
    public String getNextKey() {
        return baseDao.getNextKey();
    }

    @Override
    public void mergeObject(DatabaseInfo databaseInfo){

        baseDao.mergeObject(databaseInfo);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, DatabaseInfo> listDatabaseToDBRepo() {
        List<DatabaseInfo> dbList=baseDao.listObjects();
        Map<String, DatabaseInfo> dbmap = new HashMap<>();
        if(dbList != null){
            for(DatabaseInfo db : dbList){
                dbmap.put(db.getDatabaseCode(),db);
            }
        }
        return dbmap;
    }

    @Override
    public List<DatabaseInfo> listObjects(Map<String, Object> map){
        return baseDao.listObjects(map);
    }

    @Override
    public JSONArray listDatabaseAsJson(Map<String, Object> filterMap, PageDesc pageDesc){
        return baseDao.listObjectsAsJson(filterMap, pageDesc);
    }

    @Override
    public JSONArray queryDatabaseAsJson(String databaseName, PageDesc pageDesc){
        return baseDao.queryDatabaseAsJson(databaseName, pageDesc);
    }

    @Override
    @Transactional
    public List<DatabaseInfo> listDatabaseByOsId(String osId) {
        return baseDao.listObjectsByProperty("osId",osId);
    }
}

