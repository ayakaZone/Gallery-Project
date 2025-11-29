package com.yupi.yupicturebackend.model.vo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 脱敏用户
 * @TableName user
 */
@Data
@AllArgsConstructor
public class SpaceLevelVO implements Serializable {

    /**
     * 说明
     */
    private String text;

    /**
     * 版本值
     */
    private int value;

    /**
     * 最大项目数量
     */
    private long maxCount;

    /**
     * 最大空间容量
     */
    private long maxSize;
}
