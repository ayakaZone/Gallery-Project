package com.yupi.yupicturebackend.service;

import com.yupi.yupicturebackend.model.dto.user.UserRegisterRequest;
import com.yupi.yupicturebackend.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author Ayaki
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2025-11-21 22:29:53
*/
public interface UserService extends IService<User> {
    long userRegister(UserRegisterRequest userRegisterRequest);
}
