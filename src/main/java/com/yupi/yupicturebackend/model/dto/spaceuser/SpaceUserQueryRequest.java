package com.yupi.yupicturebackend.model.dto.spaceuser;

import lombok.Data;

import java.io.Serializable;

@Data
public class SpaceUserQueryRequest implements Serializable {


    private static final long serialVersionUID = -7502219825472679547L;

    /**
     * ID
     */
    private Long id;

    /**
     * 空间ID
     */
    private Long spaceId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 团队空间角色
     */
    private Integer spaceRole;

}
