package com.yupi.yupicture.shared.websocket.model;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;

@Getter
public enum PictureEditMessageTypeEnum {
    INFO("发送通知", "INFO"),
    ERROR("发送错误", "ERROR"),
    ENTER_EDIT("进入编辑状态", "ENTER_EDIT"),
    EDIT_ACTION("执行编辑操作", "EDIT_ACTION"),
    EXIT_EDIT("退出编辑状态", "EXIT_EDIT"),
    ;

    private final String text;

    private final String value;

    PictureEditMessageTypeEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获得 Enum
     *
     * @param value
     * @return
     */
    public static PictureEditMessageTypeEnum getEnumByValue(String value) {
        if (StrUtil.isEmpty(value)) {
            return null;
        }
        for (PictureEditMessageTypeEnum pictureEditMessageTypeEnum : PictureEditMessageTypeEnum.values()) {
            if (pictureEditMessageTypeEnum.value.equals(value)) {
                return pictureEditMessageTypeEnum;
            }
        }
        return null;
    }
}
