package com.yupi.yupicturebackend.manager.auth.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 空间角色实体类
 */
@Data
public class SpaceUserRole implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 角色键
     */
    private String key;

    /**
     * 角色名称
     */
    private String name;

    /**
     * 角色权限列表
     */
    private List<String> permissions;

    /**
     * 角色描述
     */
    private String description;
}
