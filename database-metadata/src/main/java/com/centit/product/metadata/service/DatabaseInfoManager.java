package com.centit.product.metadata.service;

import com.alibaba.fastjson.JSONArray;
import com.centit.framework.ip.po.DatabaseInfo;
import com.centit.framework.jdbc.service.BaseEntityManager;
import com.centit.support.database.utils.PageDesc;

import java.util.List;
import java.util.Map;

public interface DatabaseInfoManager extends BaseEntityManager<DatabaseInfo,String> {
    boolean connectionTest(DatabaseInfo databaseInfo);

    List<DatabaseInfo> listDatabase();

    void saveNewObject(DatabaseInfo databaseInfo);

    void mergeObject(DatabaseInfo databaseInfo);

    String getNextKey();

    Map<String, DatabaseInfo> listDatabaseToDBRepo();

    List<DatabaseInfo> listObjects(Map<String, Object> map);

    JSONArray listDatabaseAsJson(Map<String, Object> filterMap, PageDesc pageDesc);

    JSONArray queryDatabaseAsJson(String databaseName, PageDesc pageDesc);

    List<DatabaseInfo> listDatabaseByOsId(String osId);
}

