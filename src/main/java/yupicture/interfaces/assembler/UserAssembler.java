package yupicture.interfaces.assembler;

import cn.hutool.core.bean.BeanUtil;
import yupicture.domain.user.entity.User;
import yupicture.interfaces.dto.user.UserAddRequest;
import yupicture.interfaces.dto.user.UserUpdateRequest;

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

