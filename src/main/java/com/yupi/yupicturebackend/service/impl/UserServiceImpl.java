package com.yupi.yupicturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.yupicturebackend.exception.BusinessException;
import com.yupi.yupicturebackend.model.dto.user.UserLoginRequest;
import com.yupi.yupicturebackend.model.dto.user.UserQueryRequest;
import com.yupi.yupicturebackend.model.dto.user.UserRegisterRequest;
import com.yupi.yupicturebackend.model.entity.User;
import com.yupi.yupicturebackend.model.enums.UserRoleEnum;
import com.yupi.yupicturebackend.model.vo.LoginUserVO;
import com.yupi.yupicturebackend.model.vo.UserVO;
import com.yupi.yupicturebackend.service.UserService;
import com.yupi.yupicturebackend.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.yupi.yupicturebackend.Constant.UserConstant.USER_LOGIN_STATE;
import static com.yupi.yupicturebackend.exception.ErrorCode.*;

/**
* @author Ayaki
* @description 针对表【user(用户)】的数据库操作Service实现
* @createDate 2025-11-21 22:29:53
*/
@Slf4j
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
        user.setUserName("默认用户_" + UUID.randomUUID()); // 默认昵称
        user.setUserRole(UserRoleEnum.USER.getValue()); // 角色

        // 注册用户
        boolean saveResult = this.save(user);
        if (!saveResult) {
            throw new BusinessException(SYSTEM_ERROR, "注册失败，数据库异常");
        }
        return user.getId();
    }

    /**
     * 用户登录
     * @param userLoginRequest
     * @param request
     * @return
     */
    @Override
    public LoginUserVO userLogin(UserLoginRequest userLoginRequest, HttpServletRequest request) {
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
        // 密码加密
        String userPassword = userLoginRequest.getUserPassword();
        String encryptPassword = getEncryptPassword(userPassword);
        userLoginRequest.setUserPassword(encryptPassword);
        // 查询数据库
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.eq("userAccount", userLoginRequest.getUserAccount())
                .eq("userPassword", userLoginRequest.getUserPassword());
        User user = this.baseMapper.selectOne(userQueryWrapper);
        if (user == null) {
            log.info("user login failed, userAccount cannot match userPassword");
            throw new BusinessException(PARAMS_ERROR, "用户不存在或密码错误");
        }
        // Session 的用户登陆状态
        request.getSession().setAttribute(USER_LOGIN_STATE, user);
        // 用户脱敏
        return this.getLoginUserVO(user);
    }

    /**
     * 封装 LoginUserVO
     * @param user
     * @return
     */
    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        return BeanUtil.copyProperties(user, LoginUserVO.class);
    }

    /**
     * 封装 UserVO
     *
     * @param user
     * @return
     */
    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        return BeanUtil.copyProperties(user, UserVO.class);
    }

    /**
     * 封装 List<UserVO>
     *
     * @param userList
     * @return
     */
    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        List<UserVO> userVOList = userList.stream()
                .map(this::getUserVO)
                .collect(Collectors.toList());
        return userVOList;
    }

    /**
     * 获得当前登录用户
     * @param request
     * @return
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 从 Session 中获取当前登录用户
        HttpSession session = request.getSession();
        Object userObj = session.getAttribute(USER_LOGIN_STATE);
        User user = (User) userObj;
        // 校验
        if (ObjUtil.isEmpty(user) || ObjUtil.isEmpty(user.getId())) {
            throw new BusinessException(NOT_LOGIN_ERROR);
        }
        // 查询数据库（不一致性）
        Long userId = user.getId();
        user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(NOT_LOGIN_ERROR);
        }
        return user;
    }

    /**
     * 用户注销
     * @param request
     * @return
     */
    @Override
    public boolean userLogout(HttpServletRequest request) {
        // 校验
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        if (ObjUtil.isEmpty(userObj)) {
            throw new BusinessException(OPERATION_ERROR, "未登录");
        }
        // 移除 Session 登陆状态
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return true;
    }

    /**
     * 用户分页查询条件
     *
     * @param userQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        // 校验
        if (ObjUtil.isEmpty(userQueryRequest)) {
            throw new BusinessException(PARAMS_ERROR, "查询参数为空");
        }
        // 查询条件对象
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        // 用户字段
        Long id = userQueryRequest.getId();
        String userAccount = userQueryRequest.getUserAccount();
        String userName = userQueryRequest.getUserName();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        // 查询条件
        return wrapper.eq(ObjUtil.isNotEmpty(id), "id", id)
                .eq(StrUtil.isNotEmpty(userRole), "userRole", userRole)
                .like(StrUtil.isNotEmpty(userAccount), "userAccount", userAccount)
                .like(StrUtil.isNotEmpty(userName), "userName", userName)
                .like(StrUtil.isNotEmpty(userProfile), "userProfile", userProfile)
                .orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
    }

    /**
     * 获取加密处理的密码
     */
    @Override
    public String getEncryptPassword(String userPassword){
        // 加固密钥
        final String KEY = "yupi";
        // md5 加密算法
        return DigestUtils.md5DigestAsHex((KEY + userPassword).getBytes());
    }
}




