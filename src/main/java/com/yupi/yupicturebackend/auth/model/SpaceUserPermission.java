package com.yupi.yupicturebackend.auth.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 空间权限实体类
 */
@Data
public class SpaceUserPermission implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 权限键
     */
    private String key;

    /**
     * 权限名称
     */
    private String name;

    /**
     * 权限描述
     */
    private String description;
}
