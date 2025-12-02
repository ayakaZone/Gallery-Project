package com.yupi.yupicturebackend.model.dto.analyze;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@Data
public class SpaceAnalyzeRequest implements Serializable {

    private static final long serialVersionUID = -1L;

    /**
     * 空间id
     */
    private Long spaceId;

    /**
     * 是否查询公共图库
     */
    private boolean queryPublic;

    /**
     * 是否查询全空间
     */
    private boolean queryAll;
}
