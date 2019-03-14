package com.centit.support.metadata.po;

import com.alibaba.fastjson.annotation.JSONField;
import com.centit.framework.core.dao.DictionaryMap;
import com.centit.support.database.metadata.TableReference;
import com.centit.support.database.orm.GeneratorCondition;
import com.centit.support.database.orm.GeneratorTime;
import com.centit.support.database.orm.GeneratorType;
import com.centit.support.database.orm.ValueGenerator;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author zouwy
 */
@ApiModel
@Data
@Entity
@Table(name = "F_MD_RELATION")
public class MetaRelation implements TableReference, java.io.Serializable {

    private static final long serialVersionUID = -2136097274479560955L;
    /**
     * 关联代码 关联关系，类似与外键，但不创建外键
     */
    @Id
    @Column(name = "RELATION_ID")
    @ApiModelProperty(hidden = true)
    @ValueGenerator(strategy = GeneratorType.UUID)
    private String relationId;

    /**
     * 主表表ID 表单主键
     */
    @ApiModelProperty(value = "主表ID")
    @Column(name = "PARENT_TABLE_ID")
    private String parentTableId;

    /**
     * 从表表ID 表单主键
     */
    @ApiModelProperty(value = "从表ID")
    @Column(name = "CHILD_TABLE_ID")
    private String childTableId;

    /**
     * 关联名称
     */
    @ApiModelProperty(value = "关联名称")
    @Column(name = "RELATION_NAME")
    @NotBlank(message = "字段不能为空")
    @Length(max = 64, message = "字段长度不能大于{max}")
    private String relationName;

    /**
     * 状态
     */
    @ApiModelProperty(value = "状态")
    @Column(name = "RELATION_STATE")
    private String relationState = "T";

    /**
     * 关联说明
     */
    @ApiModelProperty(value = "描述")
    @Column(name = "RELATION_COMMENT")
    @Length(max = 256, message = "字段长度不能大于{max}")
    private String relationComment;

    @ApiModelProperty(value = "更改人员", hidden = true)
    @Column(name = "RECORDER")
    @Length(max = 8, message = "字段长度不能大于{max}")
    @DictionaryMap(fieldName = "recorderName", value = "userCode")
    private String  recorder;

    /**
     * 更改时间
     */
    @ApiModelProperty(hidden = true)
    @ValueGenerator(strategy = GeneratorType.FUNCTION, occasion = GeneratorTime.NEW_UPDATE, condition = GeneratorCondition.ALWAYS, value = "today()")
    @Column(name = "LAST_MODIFY_DATE")
    private Date lastModifyDate;

    @ApiModelProperty(value = "关联明细")
    @OneToMany(targetEntity = MetaRelDetail.class)
    @JoinColumn(name = "relationId", referencedColumnName = "relationId")
    private List<MetaRelDetail> relationDetails;

    @Override
    @ApiModelProperty(hidden = true)
    @JSONField(serialize = false)
    public String getReferenceCode() {
        return String.valueOf(this.relationId);
    }

    @Override
    @ApiModelProperty(hidden = true)
    @JSONField(serialize = false)
    public String getReferenceName() {
        return this.relationName;
    }

    @Override
    @ApiModelProperty(value = "子表名称")
    public String getTableName() {
        return String.valueOf(this.childTableId);
    }

    @Override
    @ApiModelProperty(value = "父表名称")
    public String getParentTableName() {
        return String.valueOf(this.parentTableId);
    }

    @Override
    @ApiModelProperty(hidden = true)
    public Map<String, String> getReferenceColumns() {
        return null;
    }

    @Override
    @ApiModelProperty(hidden = true)
    public boolean containColumn(String sCol) {
        return false;
    }
}
