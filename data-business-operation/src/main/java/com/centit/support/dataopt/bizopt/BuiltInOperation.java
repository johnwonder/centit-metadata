package com.centit.support.dataopt.bizopt;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.centit.support.algorithm.StringBaseOpt;
import com.centit.support.dataopt.core.BizModel;
import com.centit.support.dataopt.core.BizOperation;
import com.centit.support.dataopt.core.DataSet;
import com.centit.support.dataopt.utils.DataSetOptUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * 数据持久化操作
 */
public class BuiltInOperation implements BizOperation {

    public BuiltInOperation(){

    }

    public BuiltInOperation(JSONObject bizOptJson) {
        this.bizOptJson = bizOptJson;
    }
    /**
     * 操作描述
     */
    protected JSONObject bizOptJson;


    protected String getJsonFieldString(JSONObject bizOptJson, String fieldName, String defalutValue ){
        String targetDSName = bizOptJson.getString(fieldName);
        if(StringUtils.isBlank(targetDSName)){
            return defalutValue;
        }
        return targetDSName;
    }

    protected BizModel runMap(BizModel bizModel, JSONObject bizOptJson) {
        String sourDSName = getJsonFieldString(bizOptJson,"source", bizModel.getModelName());
        String targetDSName = getJsonFieldString(bizOptJson, "target", sourDSName);
        Object mapInfo = bizOptJson.get("map");
        if(mapInfo instanceof Map){
            DataSet dataSet = bizModel.fetchDataSetByName(sourDSName);
            if(dataSet != null) {
                DataSet destDS = DataSetOptUtil.mapDateSetByFormula(dataSet, ((Map) mapInfo).entrySet());
                //if(destDS != null){
                bizModel.addDataSet(targetDSName, destDS);
                //}
            }
        }
        return bizModel;
    }

    protected BizModel runFilter(BizModel bizModel, JSONObject bizOptJson) {
        String sourDSName = getJsonFieldString(bizOptJson,"source", bizModel.getModelName());
        String targetDSName = getJsonFieldString(bizOptJson, "target", sourDSName);
        String formula = bizOptJson.getString("filter");
        if(StringUtils.isNotBlank(formula)){
            DataSet dataSet = bizModel.fetchDataSetByName(sourDSName);
            if(dataSet != null) {
                DataSet destDS = DataSetOptUtil.filterDateSet(dataSet,formula);
                bizModel.addDataSet(targetDSName, destDS);
            }
        }
        return bizModel;
    }

    protected BizModel runStat(BizModel bizModel, JSONObject bizOptJson) {
        String sourDSName = getJsonFieldString(bizOptJson,"source", bizModel.getModelName());
        String targetDSName = getJsonFieldString(bizOptJson, "target", sourDSName);
        Object groupBy = bizOptJson.get("groupBy");
        List<String> groupFields = StringBaseOpt.objectToStringList(groupBy);
        Object stat = bizOptJson.get("stat");
        if(stat instanceof Map){
            DataSet dataSet = bizModel.fetchDataSetByName(sourDSName);
            if(dataSet != null) {
                DataSet destDS = DataSetOptUtil.statDataset2(dataSet, groupFields, ((Map) stat).entrySet());
                bizModel.addDataSet(targetDSName, destDS);
            }
        }
        return bizModel;
    }

    protected BizModel runAnalyse(BizModel bizModel, JSONObject bizOptJson) {
        String sourDSName = getJsonFieldString(bizOptJson,"source", bizModel.getModelName());
        String targetDSName = getJsonFieldString(bizOptJson, "target", sourDSName);

        Object orderBy = bizOptJson.get("orderBy");
        List<String> orderFields = StringBaseOpt.objectToStringList(orderBy);
        Object groupBy = bizOptJson.get("groupBy");
        List<String> groupFields = StringBaseOpt.objectToStringList(groupBy);

        Object analyse = bizOptJson.get("analyse");
        if(analyse instanceof Map){
            DataSet dataSet = bizModel.fetchDataSetByName(sourDSName);
            if(dataSet != null) {
                DataSet destDS = DataSetOptUtil.analyseDataset(dataSet,
                    groupFields, orderFields, ((Map) analyse).entrySet());
                bizModel.addDataSet(targetDSName, destDS);
            }
        }
        return bizModel;
    }

    protected BizModel runCross(BizModel bizModel, JSONObject bizOptJson) {
        String sourDSName = getJsonFieldString(bizOptJson,"source", bizModel.getModelName());
        String targetDSName = getJsonFieldString(bizOptJson, "target", sourDSName);
        Object rowHeader = bizOptJson.get("rowHeader");
        List<String> rows = StringBaseOpt.objectToStringList(rowHeader);
        Object colHeader = bizOptJson.get("colHeader");
        List<String> cols = StringBaseOpt.objectToStringList(colHeader);

        DataSet dataSet = bizModel.fetchDataSetByName(sourDSName);
        if(dataSet != null) {
            DataSet destDS = DataSetOptUtil.crossTabulation(dataSet, rows, cols);
            bizModel.addDataSet(targetDSName, destDS);
        }
        return bizModel;
    }

    protected BizModel runCompare(BizModel bizModel, JSONObject bizOptJson) {
        String sour1DSName = getJsonFieldString(bizOptJson,"source", null);
        String sour2DSName = getJsonFieldString(bizOptJson,"source2", null);
        if(sour1DSName == null || sour2DSName ==null ){
            return bizModel;
        }

        String targetDSName = getJsonFieldString(bizOptJson, "target", bizModel.getModelName());
        Object primaryKey = bizOptJson.get("primaryKey");
        List<String> pks = StringBaseOpt.objectToStringList(primaryKey);
        DataSet dataSet = bizModel.fetchDataSetByName(sour1DSName);
        DataSet dataSet2 = bizModel.fetchDataSetByName(sour2DSName);
        if(dataSet != null && dataSet2 != null) {
            DataSet destDS = DataSetOptUtil.compareTabulation(dataSet, dataSet2, pks);
            bizModel.addDataSet(targetDSName, destDS);
        }
        return bizModel;
    }

    /*private BizModel runPersistence(BizModel bizModel, JSONObject bizOptJson) {
        String sourDSName = getJsonFieldString(bizOptJson,"source", bizModel.getModelName());
        String databaseCode = getJsonFieldString(bizOptJson,"databaseCode", null);
        String tableName = getJsonFieldString(bizOptJson,"tableName", null);
        String writerType = getJsonFieldString(bizOptJson,"writerType", "merge");
        return bizModel;
    }*/

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
            case "stat":
                return runStat(bizModel, bizOptJson);
            case "analyse":
                return runAnalyse(bizModel, bizOptJson);
            case "cross":
                return runCross(bizModel, bizOptJson);
            case "compare":
                return runCompare(bizModel, bizOptJson);
            /*case "persistence":
                return runPersistence(bizModel, bizOptJson);*/
            default:
                return bizModel;
        }
    }

    @Override
    public BizModel apply(BizModel bizModel) {
        JSONArray optSteps = bizOptJson.getJSONArray("steps");
        if(optSteps==null || optSteps.isEmpty()){
            return bizModel;
        }
        BizModel result = bizModel;
        for(Object step : optSteps){
            if(step instanceof JSONObject){
                /*result =*/ runOneStep(result, (JSONObject)step);
            }
        }
        return result;
    }

    public void setBizOptJson(JSONObject bizOptJson) {
        this.bizOptJson = bizOptJson;
    }
}
