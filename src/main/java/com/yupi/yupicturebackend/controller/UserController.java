package com.yupi.yupicturebackend.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.yupicturebackend.annotation.AuthCheck;
import com.yupi.yupicturebackend.common.BaseResponse;
import com.yupi.yupicturebackend.common.DeleteRequest;
import com.yupi.yupicturebackend.common.ResultUtils;
import com.yupi.yupicturebackend.exception.ThrowUtils;
import com.yupi.yupicturebackend.model.dto.user.*;
import com.yupi.yupicturebackend.model.entity.User;
import com.yupi.yupicturebackend.model.vo.LoginUserVO;
import com.yupi.yupicturebackend.model.vo.UserVO;
import com.yupi.yupicturebackend.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import java.util.List;

import static com.yupi.yupicturebackend.exception.ErrorCode.OPERATION_ERROR;
import static com.yupi.yupicturebackend.exception.ErrorCode.PARAMS_ERROR;

@RestController
@RequestMapping("/user")
@Api(tags = "用户接口")
public class UserController {

    @Resource
    private UserService userService;

    /**
     * 用户注册
     *
     * @param userRegisterRequest
     * @return
     */
    @ApiOperation("用户注册")
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(userRegisterRequest), PARAMS_ERROR);
        long result = userService.userRegister(userRegisterRequest);
        return ResultUtils.success(result);
    }

    /**
     * 用户登录
     *
     * @param userLoginRequest
     * @param request
     * @return
     */
    @ApiOperation("用户登录")
    @PostMapping("/login")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(userLoginRequest), PARAMS_ERROR);
        LoginUserVO userVO = userService.userLogin(userLoginRequest, request);
        return ResultUtils.success(userVO);
    }

    /**
     * 获取当前登录用户
     *
     * @return
     */
    @ApiOperation("获取当前登录用户")
    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
        // 用户脱敏
        User loginUser = userService.getLoginUser(request);
        LoginUserVO loginUserVO = userService.getLoginUserVO(loginUser);
        return ResultUtils.success(loginUserVO);
    }

    /**
     * 登录用户注销
     *
     * @return
     */
    @ApiOperation("登录用户注销")
    @PostMapping("/logout")
    public BaseResponse<Boolean> logoutUser(HttpServletRequest request) {
        boolean result = userService.userLogout(request);
        return ResultUtils.success(result);
    }

    /**
     * 管理员添加用户
     *
     * @param userAddRequest
     * @return
     */
    @ApiOperation("管理员添加用户")
    @PostMapping("/add")
    @AuthCheck(mustRole = "admin")
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(userAddRequest), PARAMS_ERROR);
        // 封装user
        User user = BeanUtil.copyProperties(userAddRequest, User.class);
        // 默认密码
        final String DEFAULT_PASSWORD = "12345678";
        // 密码加密
        user.setUserPassword(userService.getEncryptPassword(DEFAULT_PASSWORD));
        // 保存用户
        boolean saveResult = userService.save(user);
        // 校验
        ThrowUtils.throwIf(!saveResult, OPERATION_ERROR);
        return ResultUtils.success(user.getId());
    }

    /**
     * 逻辑删除用户
     *
     * @param deleteRequest
     * @return
     */
    @ApiOperation("逻辑删除用户")
    @PostMapping("/delete")
    @AuthCheck(mustRole = "admin")
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(deleteRequest) || deleteRequest.getId() <= 0, PARAMS_ERROR);
        // 获取id
        Long id = deleteRequest.getId();
        // 逻辑删除
        boolean removeResult = userService.removeById(id);
        return ResultUtils.success(removeResult);
    }

    /**
     * 更新用户
     *
     * @param userUpdateRequest
     * @return
     */
    @ApiOperation("更新用户")
    @PostMapping("/update")
    @AuthCheck(mustRole = "admin")
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(userUpdateRequest) || userUpdateRequest.getId() <= 0, PARAMS_ERROR);
        // 封装 user
        User user = BeanUtil.copyProperties(userUpdateRequest, User.class);
        // 更新
        boolean updateResult = userService.updateById(user);
        // 校验
        ThrowUtils.throwIf(!updateResult, OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 获取用户
     *
     * @param id
     * @return
     */
    @ApiOperation("获取用户")
    @GetMapping("/get")
    @AuthCheck(mustRole = "admin")
    public BaseResponse<User> getUserById(long id) {
        ThrowUtils.throwIf(id <= 0, PARAMS_ERROR);
        User user = userService.getById(id);
        return ResultUtils.success(user);
    }

    /**
     * 获取用户VO
     *
     * @param id
     * @return
     */
    @ApiOperation("获取用户VO")
    @GetMapping("/get/vo")
    @AuthCheck(mustRole = "admin")
    public BaseResponse<UserVO> getUserVOById(long id) {
        ThrowUtils.throwIf(id <= 0, PARAMS_ERROR);
        BaseResponse<User> response = getUserById(id);
        User user = response.getData();
        return ResultUtils.success(userService.getUserVO(user));
    }

    /**
     * 获取用户列表
     *
     * @param userQueryRequest
     * @return
     */
    @ApiOperation("获取用户列表")
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(userQueryRequest), PARAMS_ERROR);
        // 分页参数
        int current = userQueryRequest.getCurrent();
        int pageSize = userQueryRequest.getPageSize();
        // 分页查询
        Page<User> page = userService.page(new Page<>(current, pageSize), userService.getQueryWrapper(userQueryRequest));
        List<UserVO> userVOList = userService.getUserVOList(page.getRecords());
        // 封装VO
        Page<UserVO> userVOPage = new Page<>(current, pageSize, page.getTotal());
        userVOPage.setRecords(userVOList);
        return ResultUtils.success(userVOPage);
    }
}
























