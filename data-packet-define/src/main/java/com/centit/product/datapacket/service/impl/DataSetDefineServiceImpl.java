package com.centit.product.datapacket.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.centit.framework.ip.po.DatabaseInfo;
import com.centit.framework.ip.service.IntegrationEnvironment;
import com.centit.product.dataopt.dataset.ExcelDataSet;
import com.centit.product.dataopt.dataset.FileDataSet;
import com.centit.product.datapacket.dao.DataPacketDao;
import com.centit.product.datapacket.dao.DataSetDefineDao;
import com.centit.product.datapacket.po.DataSetDefine;
import com.centit.product.datapacket.service.DataSetDefineService;
import com.centit.product.datapacket.vo.ColumnSchema;
import com.centit.support.common.ObjectException;
import com.centit.support.database.utils.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.sql.*;
import java.util.Date;
import java.util.*;

@Service
@Transactional
public class DataSetDefineServiceImpl implements DataSetDefineService {

    private final Logger logger = LoggerFactory.getLogger(DataSetDefineServiceImpl.class);

    @Autowired
    private DataPacketDao dataPacketDao;

    @Autowired
    private DataSetDefineDao resourceColumnDao;

    @Autowired
    private IntegrationEnvironment integrationEnvironment;

    @Override
    public void createDbQuery(DataSetDefine dataSetDefine) {
        resourceColumnDao.saveNewObject(dataSetDefine);
        resourceColumnDao.saveObjectReferences(dataSetDefine);
    }

    @Override
    public void updateDbQuery(DataSetDefine dataSetDefine) {
        dataSetDefine.setRecordDate(new Date());
        resourceColumnDao.updateObject(dataSetDefine);
        resourceColumnDao.saveObjectReferences(dataSetDefine);
    }

    @Override
    public void deleteDbQuery(String queryId) {
        DataSetDefine dataSetDefine = resourceColumnDao.getObjectById(queryId);
        resourceColumnDao.deleteObjectById(queryId);
        resourceColumnDao.deleteObjectReferences(dataSetDefine);
    }

    @Override
    public List<DataSetDefine> listDbQuery(Map<String, Object> params, PageDesc pageDesc) {
        return resourceColumnDao.listObjectsByProperties(params, pageDesc);
    }

    @Override
    public DataSetDefine getDbQuery(String queryId) {
        return resourceColumnDao.getObjectWithReferences(queryId);
    }

    @Override
    public JSONArray queryViewSqlData(String databaseCode, String sql, Map<String, Object> params) {
        DatabaseInfo databaseInfo = integrationEnvironment.getDatabaseInfo(databaseCode);
        QueryAndParams qap = QueryAndParams.createFromQueryAndNamedParams(QueryUtils.translateQuery(sql, params));
        try{
            return TransactionHandler.executeQueryInTransaction(JdbcConnect.mapDataSource(databaseInfo),
                (conn) -> DatabaseAccess.findObjectsAsJSON(conn,
                QueryUtils.buildLimitQuerySQL(qap.getQuery(),0,20,false,
                    DBType.mapDBType(databaseInfo.getDatabaseUrl())),
                qap.getParams()));
        }catch (SQLException | IOException e){
            logger.error("执行查询出错，SQL：{},Param:{}", qap.getQuery(), qap.getParams());
            throw new ObjectException("执行查询出错!");
        }
    }

    @Override
    public Set<String> generateSqlParams(String sql) {
        return QueryUtils.getSqlTemplateParameters(sql);
    }

    public List<ColumnSchema> generateExcelFields(Map<String, Object> params){
        ExcelDataSet excelDataSet = new ExcelDataSet();
        try {
            excelDataSet.setFilePath(FileDataSet.downFile((String)params.get("FileId")));
        } catch (IOException e) {
            e.printStackTrace();
        }
        String[] columns= excelDataSet.getColumns();
        List<ColumnSchema> columnSchemas = new ArrayList<>(10);
        if (columns!=null) {
            for (String s : columns) {
                if(s!=null && s.length()!=0) {
                    ColumnSchema col = new ColumnSchema();
                    col.setColumnCode(s);
                    col.setPropertyName(s);
                    col.setColumnName(col.getPropertyName());
                    col.setDataType(FieldType.STRING);
                    col.setIsStatData("F");
                    columnSchemas.add(col);
                }
            }
        }
        return  columnSchemas;
    }
    @Override
    public List<ColumnSchema> generateSqlFields(String databaseCode, String sql, Map<String, Object> params){
        DatabaseInfo databaseInfo = integrationEnvironment.getDatabaseInfo(databaseCode);
        QueryAndParams qap = QueryAndParams.createFromQueryAndNamedParams(QueryUtils.translateQuery(sql, params));
        String sSql = QueryUtils.buildLimitQuerySQL(qap.getQuery(),0,2,false,
            DBType.mapDBType(databaseInfo.getDatabaseUrl()));
        List<ColumnSchema> columnSchemas = new ArrayList<>(50);
        try(Connection conn = JdbcConnect.getConn(databaseInfo);
            PreparedStatement stmt = conn.prepareStatement(sSql)){

            DatabaseAccess.setQueryStmtParameters(stmt,qap.getParams());
            try(ResultSet rs = stmt.executeQuery()) {
                ResultSetMetaData rsd = rs.getMetaData();
                int nc =rsd.getColumnCount();
                for(int i=1; i<=nc; i++){
                    ColumnSchema col = new ColumnSchema();
                    col.setColumnCode(rsd.getColumnName(i));
                    col.setPropertyName(FieldType.mapPropName(rsd.getColumnName(i)));
                    col.setColumnName(col.getPropertyName());
                    col.setDataType(FieldType.mapToFieldType(rsd.getColumnType(i)));
                    col.setIsStatData("F");
                    columnSchemas.add(col);
                }
            }
        }catch (SQLException e){
            logger.error("执行查询出错，SQL：{},Param:{}", sSql, qap.getParams());
            //throw new ObjectException("执行查询出错!");
            List<String> fields = QueryUtils.getSqlFiledNames(sql);
            if(fields==null){
                throw new ObjectException(sSql, PersistenceException.DATABASE_OPERATE_EXCEPTION,
                    "执行查询出错，SQL：{},Param:{}"+ sSql);
            }
            for(String s : QueryUtils.getSqlFiledNames(sql)){
                ColumnSchema col = new ColumnSchema();
                col.setColumnCode(s);
                col.setColumnName(s);
                col.setDataType(FieldType.STRING);
                col.setIsStatData("F");
                columnSchemas.add(col);
            }
        }
        return columnSchemas;
    }
}
