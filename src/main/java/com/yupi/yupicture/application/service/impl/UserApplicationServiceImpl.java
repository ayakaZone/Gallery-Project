package com.yupi.yupicture.application.service.impl;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.yupicture.domain.user.valueobject.UserRoleEnum;
import com.yupi.yupicture.infrastructure.exception.ErrorCode;
import com.yupi.yupicture.interfaces.dto.user.UserExchangeVIPCodeRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.yupi.yupicture.application.service.UserApplicationService;
import com.yupi.yupicture.domain.user.entity.User;
import com.yupi.yupicture.domain.user.service.UserDomainService;
import com.yupi.yupicture.infrastructure.common.DeleteRequest;
import com.yupi.yupicture.infrastructure.exception.ThrowUtils;
import com.yupi.yupicture.interfaces.dto.user.UserLoginRequest;
import com.yupi.yupicture.interfaces.dto.user.UserQueryRequest;
import com.yupi.yupicture.interfaces.dto.user.UserRegisterRequest;
import com.yupi.yupicture.interfaces.vo.user.LoginUserVO;
import com.yupi.yupicture.interfaces.vo.user.UserVO;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.yupi.yupicture.infrastructure.exception.ErrorCode.*;

/**
 * 用户服务实现
 */
@Slf4j
@Service
public class UserApplicationServiceImpl implements UserApplicationService {

    @Resource
    private UserDomainService userDomainService;

    /**
     * 用户注册
     *
     * @param userRegisterRequest
     * @return
     */
    @Override
    public long userRegister(UserRegisterRequest userRegisterRequest) {
        // 校验用户注册信息
        User.validUserRegister(userRegisterRequest);
        // 完成用户注册
        long userId = userDomainService.userRegister(userRegisterRequest);
        ThrowUtils.throwIf(userId <= 0, PARAMS_ERROR);
        return userId;
    }

    /**
     * 用户登录
     *
     * @param userLoginRequest
     * @param request
     * @return
     */
    @Override
    public LoginUserVO userLogin(UserLoginRequest userLoginRequest, HttpServletRequest request) {
        // 校验用户登录信息
        User.validUserLogin(userLoginRequest);
        // 完成用户登录
        LoginUserVO loginUserVO = userDomainService.userLogin(userLoginRequest, request);
        ThrowUtils.throwIf(loginUserVO == null, PARAMS_ERROR);
        return loginUserVO;
    }

    /**
     * 获取加密处理的密码
     */
    @Override
    public String getEncryptPassword(String userPassword) {
        // 获取加密处理的密码
        String password = userDomainService.getEncryptPassword(userPassword);
        ThrowUtils.throwIf(password == null, PARAMS_ERROR);
        return password;
    }

    /**
     * 获得当前登录用户
     *
     * @param request
     * @return
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 获取当前登录用户
        User loginUser = userDomainService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, NOT_LOGIN_ERROR);
        return loginUser;
    }

    /**
     * 封装 LoginUserVO
     *
     * @param user
     * @return
     */
    @Override
    public LoginUserVO getLoginUserVO(User user) {
        // 封装 LoginUserVO
        LoginUserVO loginUserVO = userDomainService.getLoginUserVO(user);
        ThrowUtils.throwIf(loginUserVO == null, NOT_FOUND_ERROR);
        return loginUserVO;
    }

    /**
     * 获得 UserVO
     *
     * @param user
     * @return
     */
    @Override
    public UserVO getUserVO(User user) {
        // 获得 UserVO
        UserVO userVO = userDomainService.getUserVO(user);
        ThrowUtils.throwIf(userVO == null, NOT_FOUND_ERROR);
        return userVO;
    }

    /**
     * 封装 List<UserVO>
     *
     * @param userList
     * @return
     */
    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        // 封装 UserVO
        List<UserVO> userVOList = userDomainService.getUserVOList(userList);
        ThrowUtils.throwIf(userVOList == null, PARAMS_ERROR);
        return userVOList;
    }

    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    @Override
    public boolean userLogout(HttpServletRequest request) {
        // 用户注销
        boolean result = userDomainService.userLogout(request);
        ThrowUtils.throwIf(!result, NOT_LOGIN_ERROR);
        return result;
    }

    /**
     * 用户分页查询条件
     *
     * @param userQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        // 用户分页查询条件
        QueryWrapper<User> queryWrapper = userDomainService.getQueryWrapper(userQueryRequest);
        ThrowUtils.throwIf(queryWrapper == null, PARAMS_ERROR);
        return queryWrapper;
    }

    /**
     * 获取用户
     *
     * @param id
     * @return
     */
    @Override
    public User getUserById(long id) {
        // 获取用户
        User user = userDomainService.getById(id);
        ThrowUtils.throwIf(user == null, NOT_FOUND_ERROR);
        return user;
    }

    /**
     * 获取用户VO
     *
     * @param id
     * @return
     */
    @Override
    public UserVO getUserVOById(long id) {
        // 获取用户VO
        UserVO userVO = userDomainService.getUserVO(this.getUserById(id));
        ThrowUtils.throwIf(userVO == null, NOT_FOUND_ERROR);
        return userVO;
    }

    /**
     * 保存用户
     *
     * @param user
     * @return
     */
    @Override
    public Long saveUser(User user) {
        // 保存用户
        Long userId = userDomainService.saveUser(user);
        ThrowUtils.throwIf(userId <= 0, OPERATION_ERROR);
        return userId;
    }

    /**
     * 删除用户
     *
     * @param deleteRequest
     * @return
     */
    @Override
    public boolean deleteUser(DeleteRequest deleteRequest) {
        // 删除用户
        Long userId = deleteRequest.getId();
        ThrowUtils.throwIf(userId <= 0, PARAMS_ERROR);
        boolean result = userDomainService.removeById(userId);
        ThrowUtils.throwIf(!result, OPERATION_ERROR);
        return result;
    }

    /**
     * 修改用户
     *
     * @param user
     * @return
     */
    @Override
    public boolean updateUser(User user) {
        // 修改用户
        boolean result = userDomainService.updateById(user);
        ThrowUtils.throwIf(!result, OPERATION_ERROR);
        return result;
    }

    /**
     * 获取用户分页列表
     *
     * @param userQueryRequest
     * @return
     */
    @Override
    public Page<UserVO> listUserVOByPage(UserQueryRequest userQueryRequest) {
        Page<UserVO> userVOPage = userDomainService.listUserVOByPage(userQueryRequest);
        ThrowUtils.throwIf(userVOPage == null, NOT_FOUND_ERROR);
        return userVOPage;
    }

    /**
     * 获取用户列表
     *
     * @param userIdSet
     * @return
     */
    @Override
    public List<User> listByIds(Set<Long> userIdSet) {
        List<User> users = userDomainService.listByIds(userIdSet);
        ThrowUtils.throwIf(users == null, NOT_FOUND_ERROR);
        return users;
    }

    /**
     * 用户兑换会员
     * @param request
     * @param loginUser
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean exchangeVipCode(UserExchangeVIPCodeRequest request, User loginUser) {
        // 1. 参数校验
        ThrowUtils.throwIf(request == null || request.getVipCode() == null,
                ErrorCode.PARAMS_ERROR, "请求参数为空");
        String vipCode = request.getVipCode().trim();
        ThrowUtils.throwIf(vipCode.isEmpty(), ErrorCode.PARAMS_ERROR, "兑换码不能为空");

        // 2. 校验用户是否已经是会员
        ThrowUtils.throwIf(  userDomainService.isVipUser(loginUser.getId()),
                ErrorCode.OPERATION_ERROR, "您已经是会员，无需重复兑换");

        // 3. 验证并锁定兑换码
        boolean codeValid = userDomainService.validateAndUseCode(vipCode);
        ThrowUtils.throwIf(!codeValid, ErrorCode.OPERATION_ERROR, "兑换码无效或已被使用");

        // 4. 生成会员信息
        String vipNumber = "VIP_" + IdUtil.getSnowflakeNextId();
        DateTime expireTime = DateUtil.offsetYear(new Date(), 1);// 一年后到期

        // 5. 更新用户信息
        User updateUser = new User();
        updateUser.setId(loginUser.getId());
        updateUser.setUserRole(UserRoleEnum.VIP_USER.getValue()); // 或者根据你的角色定义调整
        updateUser.setVipNumber(vipNumber);
        updateUser.setVipExpireTime(expireTime);
        updateUser.setVipCode(vipCode);

        boolean updated = userDomainService.updateById(updateUser);
        ThrowUtils.throwIf(!updated, ErrorCode.SYSTEM_ERROR, "会员开通失败");

        return true;
    }

}




