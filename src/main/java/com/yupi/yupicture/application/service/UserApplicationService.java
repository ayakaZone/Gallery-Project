package com.yupi.yupicture.application.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.yupicture.domain.user.entity.User;
import com.yupi.yupicture.infrastructure.common.DeleteRequest;
import com.yupi.yupicture.interfaces.dto.user.UserExchangeVIPCodeRequest;
import com.yupi.yupicture.interfaces.dto.user.UserLoginRequest;
import com.yupi.yupicture.interfaces.dto.user.UserQueryRequest;
import com.yupi.yupicture.interfaces.dto.user.UserRegisterRequest;
import com.yupi.yupicture.interfaces.vo.user.LoginUserVO;
import com.yupi.yupicture.interfaces.vo.user.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;

/**
* @author Ayaki
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2025-11-21 22:29:53
*/
public interface UserApplicationService{

    /**
     * 用户注册
     * @param userRegisterRequest
     * @return
     */
    long userRegister(UserRegisterRequest userRegisterRequest);

    /**
     * 用户登录
     * @param userLoginRequest
     * @param request
     * @return
     */
    LoginUserVO userLogin(UserLoginRequest userLoginRequest, HttpServletRequest request);

    /**
     * 封装 LoginUserVO
     *
     * @param user
     * @return
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 获取当前登录用户
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 获得 UserVO
     * @param user
     * @return
     */
    UserVO getUserVO(User user);

    /**
     * 封装 List<UserVO>
     * @param userList
     * @return
     */
    List<UserVO> getUserVOList(List<User> userList);

    /**
     * 用户注销
     * @param request
     * @return
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 用户分页查询条件
     *
     * @param userQueryRequest
     * @return
     */
    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);

    /**
     * 获取加密密码
     * @param userPassword
     * @return
     */
    String getEncryptPassword(String userPassword);

    /**
     * 获取用户
     * @param id
     * @return
     */
    User getUserById(long id);

    /**
     * 获取用户VO
     * @param id
     * @return
     */
    UserVO getUserVOById(long id);

    /**
     * 逻辑删除用户
     * @param deleteRequest
     * @return
     */
    boolean deleteUser(DeleteRequest deleteRequest);

    /**
     * 更新用户
     * @param user
     * @return
     */
    boolean updateUser(User user);

    /**
     * 获取用户VO列表
     * @param userQueryRequest
     * @return
     */
    Page<UserVO> listUserVOByPage(UserQueryRequest userQueryRequest);

    /**
     * 批量获取用户
     * @param userIdSet
     * @return
     */
    List<User> listByIds(Set<Long> userIdSet);

    /**
     * 保存用户
     * @param user
     * @return
     */
    Long saveUser(User user);

    /**
     * 兑换会员码
     * @param exchangeVIPCodeRequest
     * @return
     */
    boolean exchangeVipCode(UserExchangeVIPCodeRequest exchangeVIPCodeRequest, User user);

}
