package com.yupi.yupicturebackend.model.enums;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 空间类型枚举
 *
 * @author Ayaki
 * @date 2025/11/23 11:01:09
 * @description 空间类型枚举
 */
@Getter
public enum SpaceRoleEnum {
    VIEWER("访问者", "viewer"),
    EDITOR("协作者", "editor"),
    ADMIN("管理员", "admin");

    private final String text;

    private final String value;

    SpaceRoleEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获得 Enum
     *
     * @param value
     * @return
     */
    public static SpaceRoleEnum getEnumByValue(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        for (SpaceRoleEnum spaceRoleEnum : SpaceRoleEnum.values()) {
            if (spaceRoleEnum.value.equals(value)) {
                return spaceRoleEnum;
            }
        }
        return null;
    }

    /**
     * 获取所有的枚举文本说明列表
     *
     * @return
     */
    public static List<String> getAllTexts() {
        return Arrays.stream(SpaceRoleEnum.values())
                .map(SpaceRoleEnum::getText)
                .collect(Collectors.toList());
    }

    /**
     * 获取所有的枚举的值列表
     *
     * @return
     */
    public static List<String> getAllValues() {
        return Arrays.stream(SpaceRoleEnum.values())
                .map(SpaceRoleEnum::getValue)
                .collect(Collectors.toList());
    }
}
