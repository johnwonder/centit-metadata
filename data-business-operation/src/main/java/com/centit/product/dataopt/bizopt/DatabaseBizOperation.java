package com.centit.product.dataopt.bizopt;

import com.alibaba.fastjson.JSONObject;
import com.centit.framework.common.ObjectException;
import com.centit.framework.ip.po.DatabaseInfo;
import com.centit.framework.ip.service.IntegrationEnvironment;
import com.centit.product.metadata.service.MetaDataService;
import com.centit.support.database.metadata.TableInfo;
import com.centit.support.database.utils.JdbcConnect;
import com.centit.support.dataopt.bizopt.BuiltInOperation;
import com.centit.support.dataopt.bizopt.PersistenceOperation;
import com.centit.support.dataopt.core.BizModel;
import com.centit.support.dataopt.core.DataSet;
import com.centit.support.dataopt.core.DataSetWriter;
import com.centit.support.dataopt.dataset.SQLDataSetWriter;
import org.apache.commons.lang3.StringUtils;

public class DatabaseBizOperation extends BuiltInOperation {

    private IntegrationEnvironment integrationEnvironment;
    private MetaDataService metaDataService;

    public DatabaseBizOperation(){

    }

    public DatabaseBizOperation(JSONObject bizOptJson) {
        this.bizOptJson = bizOptJson;
    }

    private BizModel runPersistence(BizModel bizModel, JSONObject bizOptJson) {
        String sourDSName = getJsonFieldString(bizOptJson,"source", bizModel.getModelName());
        String databaseCode = getJsonFieldString(bizOptJson,"databaseCode", null);
        String tableName = getJsonFieldString(bizOptJson,"tableName", null);
        String writerType = getJsonFieldString(bizOptJson,"writerType", "merge");
        if(databaseCode==null || tableName==null){
            throw new ObjectException(bizOptJson,
                ObjectException.DATABASE_OPERATE_EXCEPTION,
                "对应的元数据信息找不到，数据库："+databaseCode + " 表:" + databaseCode);
        }

        DatabaseInfo databaseInfo =integrationEnvironment.getDatabaseInfo(databaseCode);
        if(databaseInfo == null){
            throw new ObjectException(bizOptJson,
                ObjectException.DATABASE_OPERATE_EXCEPTION,
                "数据库信息无效："+databaseCode);
        }
        DataSet dataSet = bizModel.fetchDataSetByName(sourDSName);
        if(dataSet == null) {
            throw new ObjectException(bizOptJson,
                ObjectException.DATABASE_OPERATE_EXCEPTION,
                "数据源信息无效："+sourDSName);
        }
        TableInfo tableInfo = metaDataService.getMetaTable(databaseCode, tableName);
        if(tableInfo==null){
            throw new ObjectException(bizOptJson,
                ObjectException.DATABASE_OPERATE_EXCEPTION,
                "对应的元数据信息找不到，数据库："+databaseCode + " 表:" + databaseCode);
        }
        DataSetWriter dataSetWriter = new SQLDataSetWriter(
            JdbcConnect.mapDataSource(databaseInfo),tableInfo);

        switch (writerType) {
            case PersistenceOperation.WRITER_INDICATE_APPEND:
                dataSetWriter.append(dataSet);
                break;
            case PersistenceOperation.WRITER_INDICATE_MERGE:
            case PersistenceOperation.WRITER_INDICATE_UPDATE:
                dataSetWriter.merge(dataSet);
                break;
            default:
                dataSetWriter.save(dataSet);
                break;
        }
        return bizModel;
    }

    protected BizModel runOneStep(BizModel bizModel, JSONObject bizOptJson) {
        String sOptType = bizOptJson.getString("operation");
        if(StringUtils.isBlank(sOptType)) {
            return bizModel;
        }
        switch (sOptType){
            case "map":
                return runMap(bizModel, bizOptJson);
            case "filter":
                return runFilter(bizModel, bizOptJson);
            case "append":
                return runAppend(bizModel, bizOptJson);
            case "stat":
                return runStat(bizModel, bizOptJson);
            case "analyse":
                return runAnalyse(bizModel, bizOptJson);
            case "cross":
                return runCross(bizModel, bizOptJson);
            case "compare":
                return runCompare(bizModel, bizOptJson);
            case "join":
                return runJoin(bizModel, bizOptJson);
            case "union":
                return runUnion(bizModel, bizOptJson);
            case "persistence":
                return runPersistence(bizModel, bizOptJson);
            default:
                return bizModel;
        }
    }

    public void setIntegrationEnvironment(IntegrationEnvironment integrationEnvironment) {
        this.integrationEnvironment = integrationEnvironment;
    }

    public void setMetaDataService(MetaDataService metaDataService) {
        this.metaDataService = metaDataService;
    }
}
