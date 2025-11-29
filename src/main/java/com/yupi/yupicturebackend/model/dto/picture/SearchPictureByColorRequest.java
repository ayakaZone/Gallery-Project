package com.yupi.yupicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class SearchPictureByColorRequest implements Serializable {


    private static final long serialVersionUID = -7502219825472679547L;

    /**
     * 空间 ID
     */
    private Long spaceId;

    /**
     * 16进制图片颜色
     */
    private String picColor;
}
