package com.centit.support.dataopt.core;

import com.alibaba.fastjson.annotation.JSONField;

import java.util.HashMap;
import java.util.Map;

public class SimpleBizModel implements BizModel{
    /**
     * 模型名称
     */
    private String modelName;
    /**
     * 模型的标识， 就是对应的主键
     * 或者对应关系数据库查询的参数（数据源参数）
     */
    private Map<String, Object> modeTag;
    /**
     * 模型数据
     */
    protected Map<String, DataSet> bizData;

    public SimpleBizModel(){

    }

    public SimpleBizModel(String modelName){
        this.modelName = modelName;
    }

    public void checkBizDataSpace(){
        if(this.bizData == null){
            this.bizData = new HashMap<>(6);
        }
    }

    public void addDataSet(DataSet dataSet) {
        checkBizDataSpace();
        this.bizData.put(dataSet.getDataSetName(), dataSet);
    }

    @JSONField(deserialize = false, serialize = false)
    public void setMainDataSet(DataSet dataSet) {
        checkBizDataSpace();
        this.modelName = dataSet.getDataSetName();
        this.bizData.put(this.modelName , dataSet);
    }

    @JSONField(deserialize = false, serialize = false)
    public void setMainDataSet(String modelName, DataSet dataSet) {
        checkBizDataSpace();
        this.modelName = modelName;
        this.bizData.put(this.modelName , dataSet);
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public Map<String, Object> getModeTag() {
        return modeTag;
    }

    public void setModeTag(Map<String, Object> modeTag) {
        this.modeTag = modeTag;
    }

    public Map<String, DataSet> getBizData() {
        return bizData;
    }

    public void setBizData(Map<String, DataSet> bizData) {
        this.bizData = bizData;
    }
}
