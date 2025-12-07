package com.yupi.yupicture.interfaces.dto.space;

import lombok.Data;

import java.io.Serializable;

@Data
public class SpaceEditRequest implements Serializable {


    private static final long serialVersionUID = -7502219825472679547L;

    /**
     * id
     */
    private Long id;

    /**
     * 空间名称
     */
    private String spaceName;

}
