package com.yupi.yupicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class PictureEditByBatchRequest implements Serializable {


    private static final long serialVersionUID = -7502219825472679547L;

    /**
     * 空间ID
     */
    private Long spaceId;

    /**
     * 图片ID列表
     */
    private List<Long> pictureIdList;

    /**
     * 分类
     */
    private String category;

    /**
     * 标签
     */
    private List<String> tags;

    /**
     * 命名规则
     */
    private String nameRule;
}
