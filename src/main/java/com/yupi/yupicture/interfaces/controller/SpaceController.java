package com.yupi.yupicture.interfaces.controller;


import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.yupicture.shared.auth.SpaceUserAuthManager;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.yupi.yupicture.application.service.SpaceApplicationService;
import com.yupi.yupicture.application.service.UserApplicationService;
import com.yupi.yupicture.domain.space.entity.Space;
import com.yupi.yupicture.domain.space.valueobject.SpaceLevelEnum;
import com.yupi.yupicture.domain.user.constant.UserConstant;
import com.yupi.yupicture.domain.user.entity.User;
import com.yupi.yupicture.infrastructure.annotation.AuthCheck;
import com.yupi.yupicture.infrastructure.common.BaseResponse;
import com.yupi.yupicture.infrastructure.common.DeleteRequest;
import com.yupi.yupicture.infrastructure.common.ResultUtils;
import com.yupi.yupicture.infrastructure.exception.ErrorCode;
import com.yupi.yupicture.infrastructure.exception.ThrowUtils;
import com.yupi.yupicture.interfaces.assembler.SpaceAssembler;
import com.yupi.yupicture.interfaces.dto.space.SpaceAddRequest;
import com.yupi.yupicture.interfaces.dto.space.SpaceEditRequest;
import com.yupi.yupicture.interfaces.dto.space.SpaceQueryRequest;
import com.yupi.yupicture.interfaces.dto.space.SpaceUpdateRequest;
import com.yupi.yupicture.interfaces.vo.space.SpaceLevelVO;
import com.yupi.yupicture.interfaces.vo.space.SpaceVO;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/space")
@Api(tags = "空间接口")
public class SpaceController {

    @Resource
    private UserApplicationService userApplicationService;

    @Resource
    private SpaceApplicationService spaceApplicationService;
    @Autowired
    private SpaceUserAuthManager spaceUserAuthManager;

    /**
     * 创建空间
     *
     * @param spaceAddRequest
     * @return
     */
    @PostMapping("/add")
    @ApiOperation("创建空间")
    public BaseResponse<Long> addSpace(@RequestBody SpaceAddRequest spaceAddRequest, HttpServletRequest request) {
        /// 校验
        ThrowUtils.throwIf(ObjUtil.isEmpty(spaceAddRequest), ErrorCode.PARAMS_ERROR);
        User loginUser = userApplicationService.getLoginUser(request);
        Long spaceId = spaceApplicationService.addSpace(spaceAddRequest, loginUser);
        return ResultUtils.success(spaceId);
    }

    @PostMapping("/delete")
    @ApiOperation("删除空间")
    public BaseResponse<Boolean> deleteSpace(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        /// 校验
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        boolean removeResult = spaceApplicationService.deleteSpace(deleteRequest, request);
        return ResultUtils.success(removeResult);
    }

    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @ApiOperation("管理员空间更新")
    public BaseResponse<Boolean> updateSpace(@RequestBody SpaceUpdateRequest spaceUpdateRequest, HttpServletRequest request) {
        /// 校验
        ThrowUtils.throwIf(ObjUtil.isEmpty(spaceUpdateRequest) || spaceUpdateRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        /// 更新空间对象校验
        // 转po
        Space space = SpaceAssembler.toSpaceEntity(spaceUpdateRequest);
        boolean updateResult = spaceApplicationService.updateSpace(spaceUpdateRequest,space, request);
        return ResultUtils.success(updateResult);
    }

    /**
     * 用户空间编辑
     *
     * @param spaceEditRequest
     * @return
     */
    @PostMapping("/edit")
    @ApiOperation("用户空间编辑")
    public BaseResponse<Boolean> editSpace(@RequestBody SpaceEditRequest spaceEditRequest, HttpServletRequest request) {
        /// 校验
        ThrowUtils.throwIf(ObjUtil.isEmpty(spaceEditRequest) || spaceEditRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        // 转po
        Space space = SpaceAssembler.toSpaceEntity(spaceEditRequest);
        boolean result = spaceApplicationService.editSpace(spaceEditRequest, space, request);
        return ResultUtils.success(result);
    }

    /**
     * 根据id获取空间
     *
     * @param id
     * @param request
     * @return
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @ApiOperation("管理员获取空间")
    public BaseResponse<Space> getSpaceById(long id, HttpServletRequest request) {
        /// 校验
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 获取空间
        Space space = spaceApplicationService.getSpaceById(id);
        ThrowUtils.throwIf(ObjUtil.isEmpty(space), ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(space);
    }

    /**
     * 用户获取空间
     *
     * @param id
     * @param request
     * @return
     */
    @GetMapping("/get/vo")
    @ApiOperation("用户获取空间")

    public BaseResponse<SpaceVO> getSpaceVOById(long id, HttpServletRequest request) {
        /// 校验
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        SpaceVO spaceVO = spaceApplicationService.getSpaceVOById(id, request);
        return ResultUtils.success(spaceVO);
    }

    /**
     * 管理员空间分页查询
     *
     * @param spaceQueryRequest
     * @return
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @ApiOperation("管理员空间分页查询")
    public BaseResponse<Page<Space>> listSpaceByPage(@RequestBody SpaceQueryRequest spaceQueryRequest) {
        ThrowUtils.throwIf(spaceQueryRequest == null, ErrorCode.PARAMS_ERROR);
        Page<Space> spacePage = spaceApplicationService.listSpaceByPage(spaceQueryRequest);
        return ResultUtils.success(spacePage);
    }

    /**
     * 用户空间分页查询
     *
     * @param spaceQueryRequest
     * @return
     */
    @PostMapping("/list/page/vo")
    @ApiOperation("用户空间分页查询")
    public BaseResponse<Page<SpaceVO>> listSpaceVOByPage(@RequestBody SpaceQueryRequest spaceQueryRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceQueryRequest == null, ErrorCode.PARAMS_ERROR);
        Page<SpaceVO> spaceVOPage = spaceApplicationService.listSpaceVOByPage(spaceQueryRequest, request);
        return ResultUtils.success(spaceVOPage);
    }

    @PostMapping("/list/level")
    @ApiOperation("用户空间列表")
    public BaseResponse<List<SpaceLevelVO>> listSpaceLevel() {
        // 获取所有用户的空间状态
        List<SpaceLevelVO> spaceLevelVOList = Arrays.stream(SpaceLevelEnum.values()).map(spaceLevelEnum -> new SpaceLevelVO(spaceLevelEnum.getText(), spaceLevelEnum.getValue(), spaceLevelEnum.getMaxCount(), spaceLevelEnum.getMaxSize())).collect(Collectors.toList());
        return ResultUtils.success(spaceLevelVOList);
    }

}
























