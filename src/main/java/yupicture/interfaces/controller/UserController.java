package yupicture.interfaces.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;
import yupicture.application.service.UserApplicationService;
import yupicture.domain.user.entity.User;
import yupicture.infrastructure.annotation.AuthCheck;
import yupicture.infrastructure.common.BaseResponse;
import yupicture.infrastructure.common.DeleteRequest;
import yupicture.infrastructure.common.ResultUtils;
import yupicture.infrastructure.exception.ThrowUtils;
import yupicture.interfaces.assembler.UserAssembler;
import yupicture.interfaces.dto.user.*;
import yupicture.interfaces.vo.user.LoginUserVO;
import yupicture.interfaces.vo.user.UserVO;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import static yupicture.infrastructure.exception.ErrorCode.PARAMS_ERROR;

@RestController
@RequestMapping("/user")
@Api(tags = "用户接口")
public class UserController {

    @Resource
    private UserApplicationService userApplicationService;

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
        long userId = userApplicationService.userRegister(userRegisterRequest);
        return ResultUtils.success(userId);
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
        LoginUserVO userVO = userApplicationService.userLogin(userLoginRequest, request);
        return ResultUtils.success(userVO);
    }

    /**
     * 登录用户注销
     *
     * @return
     */
    @ApiOperation("登录用户注销")
    @PostMapping("/logout")
    public BaseResponse<Boolean> logoutUser(HttpServletRequest request) {
        boolean result = userApplicationService.userLogout(request);
        return ResultUtils.success(result);
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
        User loginUser = userApplicationService.getLoginUser(request);
        LoginUserVO loginUserVO = userApplicationService.getLoginUserVO(loginUser);
        return ResultUtils.success(loginUserVO);
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
        User user = UserAssembler.toUserEntity(userAddRequest);
        // 保存用户
        Long userId = userApplicationService.saveUser(user);
        return ResultUtils.success(userId);
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
        User user = userApplicationService.getUserById(id);
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
        return ResultUtils.success(userApplicationService.getUserVOById(id));
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
        boolean removeResult = userApplicationService.deleteUser(deleteRequest);
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
        User user = UserAssembler.toUserEntity(userUpdateRequest);
        // 更新
        boolean updateResult = userApplicationService.updateUser(user);
        return ResultUtils.success(updateResult);
    }

    /**
     * 获取用户VO列表
     *
     * @param userQueryRequest
     * @return
     */
    @ApiOperation("获取用户列表")
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest) {
        ThrowUtils.throwIf(ObjUtil.isEmpty(userQueryRequest), PARAMS_ERROR);
        Page<UserVO> userVOPage = userApplicationService.listUserVOByPage(userQueryRequest);
        return ResultUtils.success(userVOPage);
    }
}
























