package com.lion.mall.common.base;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体基类：抽取公共字段
 * <p>
 * 创建时间/更新时间由数据库默认值维护（CURRENT_TIMESTAMP）。
 *
 * @author lion
 */
@Data
public class BaseEntity implements Serializable {

    /** 主键（数据库自增） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
