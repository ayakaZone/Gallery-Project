package com.yupi.yupicture.interfaces.assembler;

import cn.hutool.core.bean.BeanUtil;
import com.yupi.yupicture.domain.user.entity.User;
import com.yupi.yupicture.interfaces.dto.user.UserAddRequest;
import com.yupi.yupicture.interfaces.dto.user.UserUpdateRequest;

public class UserAssembler {

    public static User toUserEntity(UserAddRequest userAddRequest) {
        // 封装user
        return BeanUtil.copyProperties(userAddRequest, User.class);
    }

    public static User toUserEntity(UserUpdateRequest updateRequest) {
        // 封装user
        return BeanUtil.copyProperties(updateRequest, User.class);
    }
}

