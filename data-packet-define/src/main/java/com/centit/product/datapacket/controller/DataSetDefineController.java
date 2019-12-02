package com.centit.product.datapacket.controller;

import com.alibaba.fastjson.JSONArray;
import com.centit.framework.common.WebOptUtils;
import com.centit.framework.core.controller.BaseController;
import com.centit.framework.core.controller.WrapUpResponseBody;
import com.centit.framework.core.dao.PageQueryResult;
import com.centit.product.datapacket.po.DataSetDefine;
import com.centit.product.datapacket.service.DataSetDefineService;
import com.centit.product.datapacket.vo.ColumnSchema;
import com.centit.support.database.utils.PageDesc;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Api(value = "数据集查询", tags = "数据集查询")
@RestController
@RequestMapping(value = "query")
public class DataSetDefineController extends BaseController {

    @Autowired
    private DataSetDefineService dataSetDefineService;

    @ApiOperation(value = "新增数据集")
    @PostMapping
    @WrapUpResponseBody
    public void createDbQuery(DataSetDefine dataSetDefine, HttpServletRequest request){
        String userCode = WebOptUtils.getCurrentUserCode(request);
        dataSetDefine.setRecorder(userCode);
        dataSetDefineService.createDbQuery(dataSetDefine);
    }

    @ApiOperation(value = "编辑数据集")
    @PutMapping(value = "/{queryId}")
    @WrapUpResponseBody
    public void updateDbQuery(@PathVariable String queryId, DataSetDefine dataSetDefine){
        dataSetDefine.setQueryId(queryId);
        //dataSetDefine.setQuerySql(HtmlUtils.htmlUnescape(dataResource.getQuerySql()));
        dataSetDefineService.updateDbQuery(dataSetDefine);
    }

    @ApiOperation(value = "删除数据集")
    @DeleteMapping(value = "/{queryId}")
    @WrapUpResponseBody
    public void deleteDbQuery(@PathVariable String queryId){
        dataSetDefineService.deleteDbQuery(queryId);
    }

    @ApiOperation(value = "查询数据集")
    @GetMapping
    @WrapUpResponseBody
    public PageQueryResult<DataSetDefine> listDbQuery(PageDesc pageDesc){
        List<DataSetDefine> list = dataSetDefineService.listDbQuery(new HashMap<>(), pageDesc);
        return PageQueryResult.createResult(list, pageDesc);
    }

    @ApiOperation(value = "查询单个数据集")
    @GetMapping(value = "/{queryId}")
    @WrapUpResponseBody
    public DataSetDefine getDbQuery(@PathVariable String queryId){
        return dataSetDefineService.getDbQuery(queryId);
    }

    @ApiOperation(value = "预览数据值返回前20行")
    @ApiImplicitParams(value = {
        @ApiImplicitParam(name = "databaseCode", value = "数据库代码", required = true),
        @ApiImplicitParam(name = "sql", value = "查询SQL", required = true)
    })
    @RequestMapping(value = "/reviewdata", method = {RequestMethod.POST})
    @WrapUpResponseBody
    public JSONArray queryViewSqlData(String databaseCode, String sql, HttpServletRequest request){
        Map<String, Object> params = collectRequestParameters(request);;
        //table.put("column", dataSetDefineService.generateColumn(databaseCode, HtmlUtils.htmlUnescape(sql)));
        return dataSetDefineService.queryViewSqlData(databaseCode, sql, params);
    }

    @ApiOperation(value = "生成查询字段列表")
    @ApiImplicitParam(name = "sql", value = "查询SQL", required = true)
    @RequestMapping(value = "/sqlcolumn", method = {RequestMethod.POST})
    @WrapUpResponseBody
    public List<ColumnSchema> generateSqlcolumn(String databaseCode, String sql,String dataType, HttpServletRequest request){
        Map<String, Object> params = collectRequestParameters(request);
        if ("E".equals(dataType)||databaseCode==null||databaseCode.equals("")){
            params.put("FileId",sql);
            return dataSetDefineService.generateExcelFields(params);
        }
        return dataSetDefineService.generateSqlFields(databaseCode, sql, params);
    }


    @ApiOperation(value = "生成参数名称列表")
    @ApiImplicitParam(name = "sql", value = "查询SQL", required = true)
    @RequestMapping(value = "/param", method = {RequestMethod.POST})
    @WrapUpResponseBody
    public Set<String> generateParam(String sql ){
        return dataSetDefineService.generateSqlParams(sql);
    }

}
