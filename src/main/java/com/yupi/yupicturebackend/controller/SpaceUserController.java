package com.yupi.yupicturebackend.controller;


import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yupi.yupicturebackend.manager.auth.annotation.SaSpaceCheckPermission;
import com.yupi.yupicturebackend.manager.auth.model.SpaceUserPermissionConstant;
import yupicture.application.service.UserApplicationService;
import yupicture.infrastructure.common.BaseResponse;
import yupicture.infrastructure.common.DeleteRequest;
import yupicture.infrastructure.common.ResultUtils;
import yupicture.infrastructure.exception.ErrorCode;
import yupicture.infrastructure.exception.ThrowUtils;
import com.yupi.yupicturebackend.model.dto.spaceuser.SpaceUserAddRequest;
import com.yupi.yupicturebackend.model.dto.spaceuser.SpaceUserEditRequest;
import com.yupi.yupicturebackend.model.dto.spaceuser.SpaceUserQueryRequest;
import com.yupi.yupicturebackend.model.entity.SpaceUser;
import yupicture.domain.user.entity.User;
import com.yupi.yupicturebackend.model.vo.space.SpaceUserVO;
import com.yupi.yupicturebackend.service.SpaceUserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/spaceUser")
@Api(tags = "团队空间接口")
public class SpaceUserController {

    @Resource
    private SpaceUserService spaceUserService;
    @Autowired
    private UserApplicationService userApplicationService;

    /**
     * 创建团队空间
     *
     * @param spaceUserAddRequest
     * @return
     */
    @PostMapping("/add")
    @ApiOperation("创建团队空间/加入团队空间")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<Long> addSpaceUser(@RequestBody SpaceUserAddRequest spaceUserAddRequest) {
        /// 校验
        ThrowUtils.throwIf(ObjUtil.isEmpty(spaceUserAddRequest), ErrorCode.PARAMS_ERROR);
        Long spaceUserId = spaceUserService.addSpaceUser(spaceUserAddRequest);
        return ResultUtils.success(spaceUserId);
    }

    /**
     * 删除团队空间
     *
     * @param deleteRequest
     * @return
     */
    @PostMapping("/delete")
    @ApiOperation("删除团队空间成员")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<Boolean> deleteSpaceUser(@RequestBody DeleteRequest deleteRequest) {
        /// 校验
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        spaceUserService.deleteSpaceUser(deleteRequest);
        return ResultUtils.success(true);
    }

    /**
     * 团队空间编辑
     *
     * @param spaceUserEditRequest
     * @return
     */
    @PostMapping("/edit")
    @ApiOperation("团队空间编辑成员")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<Boolean> editSpaceUser(
            @RequestBody SpaceUserEditRequest spaceUserEditRequest) {
        /// 校验
        ThrowUtils.throwIf(
                ObjUtil.isEmpty(spaceUserEditRequest) || spaceUserEditRequest.getId() <= 0,
                ErrorCode.PARAMS_ERROR);
        spaceUserService.editSpaceUser(spaceUserEditRequest);
        return ResultUtils.success(true);
    }

    /**
     * 获取成员空间信息
     *
     * @param spaceUserQueryRequest
     * @return
     */
    @PostMapping("/get")
    @ApiOperation("获取成员空间信息")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<SpaceUser> getSpaceUser(@RequestBody SpaceUserQueryRequest spaceUserQueryRequest) {
        /// 校验
        ThrowUtils.throwIf(ObjUtil.isEmpty(spaceUserQueryRequest), ErrorCode.PARAMS_ERROR);
        // 获取参数
        Long spaceId = spaceUserQueryRequest.getSpaceId();
        Long userId = spaceUserQueryRequest.getUserId();
        ThrowUtils.throwIf(ObjUtil.hasEmpty(spaceId, userId), ErrorCode.PARAMS_ERROR);
        // 查询条件
        QueryWrapper<SpaceUser> queryWrapper = spaceUserService.getQueryWrapper(spaceUserQueryRequest);
        SpaceUser spaceUser = spaceUserService.getOne(queryWrapper);
        ThrowUtils.throwIf(ObjUtil.isEmpty(spaceUser), ErrorCode.NOT_FOUND_ERROR);

        return ResultUtils.success(spaceUser);
    }

    /**
     * 查询团队空间成员列表
     *
     * @param spaceUserQueryRequest
     * @return
     */
    @PostMapping("/list")
    @ApiOperation("查询团队空间成员列表")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<List<SpaceUserVO>> listSpaceUser(@RequestBody SpaceUserQueryRequest spaceUserQueryRequest) {
        /// 校验
        ThrowUtils.throwIf(ObjUtil.isEmpty(spaceUserQueryRequest), ErrorCode.PARAMS_ERROR);
        // 查询条件
        QueryWrapper<SpaceUser> queryWrapper = spaceUserService.getQueryWrapper(spaceUserQueryRequest);
        // 查询数据库
        List<SpaceUser> list = spaceUserService.list(queryWrapper);
        // 转换为VO列表
        List<SpaceUserVO> spaceUserVOList = spaceUserService.getSpaceUserVOList(list);
        return ResultUtils.success(spaceUserVOList);
    }

    /**
     * 查询我加入的团队列表
     * @param request
     * @return
     */
    @PostMapping("/list/my")
    @ApiOperation("查询我加入的团队列表")
    public BaseResponse<List<SpaceUserVO>> listMySpaceUser(HttpServletRequest request) {
        // 校验登录用户
        User loginUser = userApplicationService.getLoginUser(request);
        ThrowUtils.throwIf(ObjUtil.isEmpty(loginUser), ErrorCode.NOT_LOGIN_ERROR);
        // 创建查询条件
        SpaceUserQueryRequest spaceUserQueryRequest = new SpaceUserQueryRequest();
        spaceUserQueryRequest.setUserId(loginUser.getId());
        QueryWrapper<SpaceUser> queryWrapper = spaceUserService.getQueryWrapper(spaceUserQueryRequest);
        // 查询数据库
        List<SpaceUser> list = spaceUserService.list(queryWrapper);
        // 封装VO列表
        List<SpaceUserVO> spaceUserVOList = spaceUserService.getSpaceUserVOList(list);
        return ResultUtils.success(spaceUserVOList);
    }
}
























