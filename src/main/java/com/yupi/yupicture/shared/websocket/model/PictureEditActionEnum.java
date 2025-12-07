package com.yupi.yupicture.shared.websocket.model;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;

@Getter
public enum PictureEditActionEnum {
    ZOOM_IN("放大操作", "ZOOM_IN"),
    ZOOM_OUT("缩小操作", "ZOOM_OUT"),
    ROTATE_LEFT("左旋操作", "ROTATE_LEFT"),
    ROTATE_RiGHT("右旋操作", "ROTATE_RIGHT");

    private final String text;

    private final String value;

    PictureEditActionEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获得 Enum
     *
     * @param value
     * @return
     */
    public static PictureEditActionEnum getEnumByValue(String value) {
        if (StrUtil.isEmpty(value)) {
            return null;
        }
        for (PictureEditActionEnum pictureEditActionEnum : PictureEditActionEnum.values()) {
            if (pictureEditActionEnum.value.equals(value)) {
                return pictureEditActionEnum;
            }
        }
        return null;
    }
}
