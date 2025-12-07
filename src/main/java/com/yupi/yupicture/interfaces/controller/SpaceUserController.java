package com.yupi.yupicture.interfaces.controller;


import cn.hutool.core.util.ObjUtil;
import com.yupi.yupicture.shared.auth.annotation.SaSpaceCheckPermission;
import com.yupi.yupicture.shared.auth.model.SpaceUserPermissionConstant;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.yupi.yupicture.application.service.SpaceUserApplicationService;
import com.yupi.yupicture.domain.space.entity.SpaceUser;
import com.yupi.yupicture.infrastructure.common.BaseResponse;
import com.yupi.yupicture.infrastructure.common.DeleteRequest;
import com.yupi.yupicture.infrastructure.common.ResultUtils;
import com.yupi.yupicture.infrastructure.exception.ErrorCode;
import com.yupi.yupicture.infrastructure.exception.ThrowUtils;
import com.yupi.yupicture.interfaces.dto.spaceuser.SpaceUserAddRequest;
import com.yupi.yupicture.interfaces.dto.spaceuser.SpaceUserEditRequest;
import com.yupi.yupicture.interfaces.dto.spaceuser.SpaceUserQueryRequest;
import com.yupi.yupicture.interfaces.vo.space.SpaceUserVO;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/spaceUser")
@Api(tags = "团队空间接口")
public class SpaceUserController {

    @Resource
    private SpaceUserApplicationService spaceUserApplicationService;

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
        Long spaceUserId = spaceUserApplicationService.addSpaceUser(spaceUserAddRequest);
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
        spaceUserApplicationService.deleteSpaceUser(deleteRequest);
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
        spaceUserApplicationService.editSpaceUser(spaceUserEditRequest);
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
        SpaceUser spaceUser = spaceUserApplicationService.getSpaceUser(spaceUserQueryRequest);
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
        List<SpaceUserVO> spaceUserVOList = spaceUserApplicationService.getListSpaceUser(spaceUserQueryRequest);
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
        List<SpaceUserVO> spaceUserVOList = spaceUserApplicationService.listMySpaceUser(request);
        return ResultUtils.success(spaceUserVOList);
    }
}
























