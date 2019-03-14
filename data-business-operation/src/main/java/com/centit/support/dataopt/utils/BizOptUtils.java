package com.centit.support.dataopt.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.centit.support.algorithm.CollectionsOpt;
import com.centit.support.dataopt.core.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public abstract class BizOptUtils {

    @SuppressWarnings("unchecked")
    public static BizModel castObjectToBizModel(Object obj) {
        if(obj==null){
            return null;
        }
        if(obj instanceof BizModel){
            return (BizModel)obj;
        }
        if(obj instanceof Map) {
            Map<String, Object> objectMap = (Map<String, Object>) obj;
            if (objectMap.containsKey("bizData")) {
                JSONObject jobj = (JSONObject) JSON.toJSON(obj);
                return JSON.toJavaObject(jobj, BizModel.class);
            }
        }
        DataSet dataSet = castObjectToDataSet(obj);
        return new SingleDataSetModel(dataSet);
    }

    @SuppressWarnings("unchecked")
    public static DataSet castObjectToDataSet(Object obj) {
        if(obj==null){
            return null;
        }
        if(obj instanceof DataSet){
            return (DataSet)obj;
        }

        if(obj instanceof Map){
            Map<String, Object> objectMap = (Map<String, Object>)obj;
            if( objectMap.containsKey("dataSetName") && objectMap.containsKey("data")){
                JSONObject jobj = (JSONObject)JSON.toJSON(obj);
                return JSON.toJavaObject(jobj, SimpleDataSet.class);
            } else {
                return new SingleRowDataSet(objectMap);
            }
        } else if(obj instanceof Collection){
            Collection<Object> objs = (Collection<Object>)obj;
            List<Map<String, Object>> data = new ArrayList<>(objs.size());
            for(Object object : objs){
                if(object instanceof Map){
                    data.add((Map<String, Object>)object);
                } else {
                    data.add(CollectionsOpt.createHashMap(
                        SingleObjectDataSet.SINGLE_DATA_FIELD_NAME,object));
                }
            }
            SimpleDataSet dataSet = new SimpleDataSet();
            dataSet.setData(data);
            return dataSet;
        } else {
            return new SingleObjectDataSet(obj);
        }
    }
}
