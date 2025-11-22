package com.yupi.yupicturebackend.service.impl;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.yupicturebackend.exception.BusinessException;
import com.yupi.yupicturebackend.exception.ErrorCode;
import com.yupi.yupicturebackend.model.dto.UserRegisterRequest;
import com.yupi.yupicturebackend.model.entity.User;
import com.yupi.yupicturebackend.model.enums.UserRoleEnum;
import com.yupi.yupicturebackend.service.UserService;
import com.yupi.yupicturebackend.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import static com.yupi.yupicturebackend.exception.ErrorCode.PARAMS_ERROR;
import static com.yupi.yupicturebackend.exception.ErrorCode.SYSTEM_ERROR;

/**
* @author Ayaki
* @description 针对表【user(用户)】的数据库操作Service实现
* @createDate 2025-11-21 22:29:53
*/
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{

    /**
     * 用户注册
     * @param userRegisterRequest
     * @return
     */
    @Override
    public long userRegister(UserRegisterRequest userRegisterRequest) {
        // 校验参数
        if (ObjUtil.isEmpty(userRegisterRequest)) {
            throw new BusinessException(PARAMS_ERROR, "参数为空");
        }
        if (userRegisterRequest.getUserAccount().length() < 4) {
            throw new BusinessException(PARAMS_ERROR, "账号不能小于4位");
        }
        if (userRegisterRequest.getUserPassword().length() < 8 || userRegisterRequest.getCheckPassword().length() < 8) {
            throw new BusinessException(PARAMS_ERROR, "密码不能小于8位");
        }
        if (!userRegisterRequest.getUserPassword().equals(userRegisterRequest.getCheckPassword())) {
            throw new BusinessException(PARAMS_ERROR, "两次输入密码不一致");
        }
        // 用户账号是否重复
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userRegisterRequest.getUserAccount());
        Long count = this.baseMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException(PARAMS_ERROR, "账号已重复");
        }
        // 密码加密
        String encryptPassword = getEncryptPassword(userRegisterRequest.getUserPassword());
        // 封装用户
        User user = new User();
        user.setUserAccount(userRegisterRequest.getUserAccount()); // 账号
        user.setUserPassword(encryptPassword); // 加密后的密码
        user.setUserName("默认用户_" + UUID.randomUUID());
        user.setUserRole(UserRoleEnum.USER.getValue()); // 角色

        // 注册用户
        boolean saveResult = this.save(user);
        if (!saveResult) {
            throw new BusinessException(SYSTEM_ERROR, "注册失败，数据库异常");
        }
        return user.getId();
    }

    /**
     * 获取加密处理的密码
     */
    public String getEncryptPassword(String userPassword){
        // 加固密钥
        final String KEY = "yupi";
        // md5 加密算法
        return DigestUtils.md5DigestAsHex((KEY + userPassword).getBytes());
    }
}




