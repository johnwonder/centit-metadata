package com.centit.product.dataopt.core;

import com.alibaba.fastjson.JSONObject;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class SimpleBizModel implements BizModel, Serializable {
    private static final long serialVersionUID = -2048746719360025014L;
    /**
     * 模型名称
     */
    private String modelName;
    /**
     * 模型的标识， 就是对应的主键
     * 或者对应关系数据库查询的参数（数据源参数）
     */
    private Map<String, Object> modelTag;
    /**
     * 模型数据
     */
    protected Map<String, DataSet> bizData;

    public SimpleBizModel(){
        modelName = BizModel.DEFAULT_MODEL_NAME;
    }

    public SimpleBizModel(String modelName){
        this.modelName = modelName;
    }

    public void checkBizDataSpace(){
        if(this.bizData == null){
            this.bizData = new HashMap<>(6);
        }
    }

    /**
     * @param singleRowAsObject 如果为true DataSet中只有一行记录的就作为JSONObject；
     *                          否则为 JSONArray
     * @return JSONObject
     */
    @Override
    public JSONObject toJSONObject(boolean singleRowAsObject) {
        JSONObject dataObject = new JSONObject();
        if(bizData != null) {
            for (Map.Entry<String, DataSet> datasetEnt : bizData.entrySet()){
                DataSet dataSet = datasetEnt.getValue();
                if(dataSet.getRowCount() == 1 && singleRowAsObject){
                    dataObject.put(datasetEnt.getKey(), dataSet.getFirstRow());
                } else if(!dataSet.isEmpty()){
                    dataObject.put(datasetEnt.getKey(), dataSet.getData());
                }
            }
        }
        if(modelTag != null && !modelTag.isEmpty()){
            dataObject.put("modelTag", modelTag);
        }
        return dataObject;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public Map<String, Object> getModelTag() {
        return modelTag;
    }

    public void setModelTag(Map<String, Object> modelTag) {
        this.modelTag = modelTag;
    }

    public Map<String, DataSet> getBizData() {
        return bizData;
    }

    public void setBizData(Map<String, DataSet> bizData) {
        this.bizData = bizData;
    }
}
