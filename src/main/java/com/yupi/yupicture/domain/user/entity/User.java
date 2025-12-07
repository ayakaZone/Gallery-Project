package com.yupi.yupicture.domain.user.entity;

import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import com.yupi.yupicture.domain.user.valueobject.UserRoleEnum;
import com.yupi.yupicture.infrastructure.exception.BusinessException;
import com.yupi.yupicture.interfaces.dto.user.UserLoginRequest;
import com.yupi.yupicture.interfaces.dto.user.UserRegisterRequest;

import java.io.Serializable;
import java.util.Date;

import static com.yupi.yupicture.infrastructure.exception.ErrorCode.PARAMS_ERROR;

/**
 * 用户
 * @TableName user
 */
@TableName(value ="user")
@Data
public class User implements Serializable {

    /**
     * id 雪花算法生成 ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 密码
     */
    private String userPassword;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户简介
     */
    private String userProfile;

    /**
     * 用户角色：user/admin
     */
    private String userRole;

    /**
     * 编辑时间
     */
    private Date editTime;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 会员编号（唯一）
     */
    private String vipNumber;

    /**
     * 会员到期时间
     */
    private Date vipExpireTime;

    /**
     * 兑换会员的兑换码
     */
    private String vipCode;

    /**
     * 是否删除 逻辑删除
     */
    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

    /**
     * 校验用户注册信息
     * @param userRegisterRequest
     */
    public static void validUserRegister(UserRegisterRequest userRegisterRequest){
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
    }

    /**
     * 校验用户登录信息
     * @param userLoginRequest
     */
    public static void validUserLogin(UserLoginRequest userLoginRequest){
        // 参数校验
        if (ObjUtil.isEmpty(userLoginRequest)) {
            throw new BusinessException(PARAMS_ERROR, "参数为空");
        }
        if (userLoginRequest.getUserAccount().length() < 4) {
            throw new BusinessException(PARAMS_ERROR, "账号不能小于4位");
        }
        if (userLoginRequest.getUserPassword().length() < 8) {
            throw new BusinessException(PARAMS_ERROR, "密码不能小于8位");
        }
    }

    /**
     * 判断是否是管理员
     * @return
     */
    public boolean isAdmin() {
        return this.getUserRole().equals(UserRoleEnum.ADMIN.getValue());
    }
}