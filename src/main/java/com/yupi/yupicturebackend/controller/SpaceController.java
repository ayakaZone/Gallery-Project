package com.yupi.yupicturebackend.controller;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.yupicturebackend.annotation.AuthCheck;
import com.yupi.yupicturebackend.auth.SpaceUserAuthManager;
import com.yupi.yupicturebackend.common.BaseResponse;
import com.yupi.yupicturebackend.common.DeleteRequest;
import com.yupi.yupicturebackend.common.ResultUtils;
import com.yupi.yupicturebackend.constant.UserConstant;
import com.yupi.yupicturebackend.exception.BusinessException;
import com.yupi.yupicturebackend.exception.ErrorCode;
import com.yupi.yupicturebackend.exception.ThrowUtils;
import com.yupi.yupicturebackend.model.dto.space.SpaceAddRequest;
import com.yupi.yupicturebackend.model.dto.space.SpaceEditRequest;
import com.yupi.yupicturebackend.model.dto.space.SpaceQueryRequest;
import com.yupi.yupicturebackend.model.dto.space.SpaceUpdateRequest;
import com.yupi.yupicturebackend.model.entity.Space;
import com.yupi.yupicturebackend.model.entity.User;
import com.yupi.yupicturebackend.model.enums.SpaceLevelEnum;
import com.yupi.yupicturebackend.model.vo.space.SpaceLevelVO;
import com.yupi.yupicturebackend.model.vo.space.SpaceVO;
import com.yupi.yupicturebackend.service.SpaceService;
import com.yupi.yupicturebackend.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/space")
@Api(tags = "空间接口")
public class SpaceController {

    @Resource
    private UserService userService;

    @Resource
    private SpaceService spaceService;
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
        User loginUser = userService.getLoginUser(request);
        Long spaceId = spaceService.addSpace(spaceAddRequest, loginUser);
        return ResultUtils.success(spaceId);
    }

    @PostMapping("/delete")
    @ApiOperation("删除空间")
    public BaseResponse<Boolean> deleteSpace(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        /// 校验
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        // 解析参数
        Long id = deleteRequest.getId();
        User loginUser = userService.getLoginUser(request);
        // 空间是否存在
        Space oldSpace = spaceService.getById(id);
        ThrowUtils.throwIf(ObjUtil.isEmpty(oldSpace), ErrorCode.NOT_FOUND_ERROR);
        // 仅本人与管理员可删除
        spaceService.checkSpaceAuth(loginUser, oldSpace);
        // 逻辑删除
        boolean removeResult = spaceService.removeById(id);
        ThrowUtils.throwIf(!removeResult, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @ApiOperation("管理员空间更新")
    public BaseResponse<Boolean> updateSpace(
            @RequestBody SpaceUpdateRequest spaceUpdateRequest, HttpServletRequest request) {
        /// 校验
        ThrowUtils.throwIf(
                ObjUtil.isEmpty(spaceUpdateRequest) || spaceUpdateRequest.getId() <= 0,
                ErrorCode.PARAMS_ERROR);
        /// 更新空间对象校验
        // 转po
        Space space = BeanUtil.copyProperties(spaceUpdateRequest, Space.class);
        // 自动补充数据
        spaceService.fillSpaceBySpaceLevel(space);
        // 校验空间，传 false 表示更新校验
        spaceService.validSpace(space, false);
        // 校验需要更新数据是否存在
        Long id = spaceUpdateRequest.getId();
        Space oldSpace = spaceService.getById(id);
        if (ObjUtil.isEmpty(oldSpace)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "更新的空间不存在");
        }
        // 更新空间-操作数据库
        boolean result = spaceService.updateById(space);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "更新空间失败");
        return ResultUtils.success(true);
    }

    /**
     * 用户空间编辑
     *
     * @param spaceEditRequest
     * @return
     */
    @PostMapping("/edit")
    @ApiOperation("用户空间编辑")
    public BaseResponse<Boolean> editSpace(
            @RequestBody SpaceEditRequest spaceEditRequest, HttpServletRequest request) {
        /// 校验
        ThrowUtils.throwIf(
                ObjUtil.isEmpty(spaceEditRequest) || spaceEditRequest.getId() <= 0,
                ErrorCode.PARAMS_ERROR);
        // 转po
        Space space = BeanUtil.copyProperties(spaceEditRequest, Space.class);
        // 自动填充参数
        spaceService.fillSpaceBySpaceLevel(space);
        // 设置编辑时间
        space.setEditTime(new Date());
        // 校验参数-false 表示编辑校验
        spaceService.validSpace(space, false);
        // 校验编辑数据是否存在
        Long id = space.getId();
        Space oldSpace = spaceService.getById(id);
        ThrowUtils.throwIf(ObjUtil.isEmpty(oldSpace), ErrorCode.NOT_FOUND_ERROR, "编辑的空间不存在");
        // 仅本人或管理员可编辑
        User loginUser = userService.getLoginUser(request);
        spaceService.checkSpaceAuth(loginUser, space);
        /// 编辑
        // 编辑-操作数据库
        boolean result = spaceService.updateById(oldSpace);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "编辑空间失败");
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
        Space space = spaceService.getById(id);
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
        // 获取空间
        Space space = spaceService.getById(id);
        ThrowUtils.throwIf(ObjUtil.isEmpty(space), ErrorCode.NOT_FOUND_ERROR);
        // 获得spaceVO
        SpaceVO spaceVO = spaceService.getSpaceVO(space, request);
        User loginUser = userService.getLoginUser(request);
        // 获得权限列表
        List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);
        // 存入权限列表
        spaceVO.setPermissionList(permissionList);
        // 存入UserVO
        spaceVO.setUserVO(userService.getUserVO(loginUser));
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
        // 页面条件
        int current = spaceQueryRequest.getCurrent();
        int pageSize = spaceQueryRequest.getPageSize();
        // 分页查询
        Page<Space> spacePage = spaceService.page(
                new Page<>(current, pageSize),
                spaceService.getQueryWrapper(spaceQueryRequest));
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
    public BaseResponse<Page<SpaceVO>> listSpaceVOByPage(
            @RequestBody SpaceQueryRequest spaceQueryRequest, HttpServletRequest request) {
        // 页面条件
        int current = spaceQueryRequest.getCurrent();
        int pageSize = spaceQueryRequest.getPageSize();
        // 分页查询
        Page<Space> spacePage = spaceService.page(
                new Page<>(current, pageSize),
                spaceService.getQueryWrapper(spaceQueryRequest));
        // 空间分页转 VO
        Page<SpaceVO> spaceVOPage = spaceService.getSpaceVOPage(spacePage, request);
        return ResultUtils.success(spaceVOPage);
    }

    @PostMapping("/list/level")
    @ApiOperation("用户空间列表")
    public BaseResponse<List<SpaceLevelVO>> listSpaceLevel() {
        // 获取所有用户的空间状态
        List<SpaceLevelVO> spaceLevelVOList = Arrays.stream(SpaceLevelEnum.values())
                .map(spaceLevelEnum -> new SpaceLevelVO(
                        spaceLevelEnum.getText(),
                        spaceLevelEnum.getValue(),
                        spaceLevelEnum.getMaxCount(),
                        spaceLevelEnum.getMaxSize())
                ).collect(Collectors.toList());
        return ResultUtils.success(spaceLevelVOList);
    }

}
























