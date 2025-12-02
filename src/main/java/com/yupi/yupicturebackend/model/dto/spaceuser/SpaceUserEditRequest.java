package com.yupi.yupicturebackend.model.dto.spaceuser;

import lombok.Data;

import java.io.Serializable;

@Data
public class SpaceUserEditRequest implements Serializable {


    private static final long serialVersionUID = -7502219825472679547L;

    /**
     * id
     */
    private Long id;

    /**
     * 团队空间角色
     */
    private String spaceRole;

}
