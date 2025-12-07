package com.yupi.yupicture.interfaces.dto.spaceuser;

import lombok.Data;

import java.io.Serializable;

@Data
public class SpaceUserAddRequest implements Serializable {


    private static final long serialVersionUID = -7502219825472679547L;

    /**
     * 空间ID
     */
    private Long spaceId;

    /**
     * 用户 id
     */
    private Long userId;

    /**
     * 团队空间角色
     */
    private String spaceRole;

}
