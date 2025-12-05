package com.yupi.yupicturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import yupicture.application.service.UserApplicationService;
import yupicture.infrastructure.common.DeleteRequest;
import yupicture.infrastructure.exception.ErrorCode;
import yupicture.infrastructure.exception.ThrowUtils;
import com.yupi.yupicturebackend.model.dto.spaceuser.SpaceUserAddRequest;
import com.yupi.yupicturebackend.model.dto.spaceuser.SpaceUserEditRequest;
import com.yupi.yupicturebackend.model.dto.spaceuser.SpaceUserQueryRequest;
import com.yupi.yupicturebackend.model.entity.Space;
import com.yupi.yupicturebackend.model.entity.SpaceUser;
import yupicture.domain.user.entity.User;
import com.yupi.yupicturebackend.model.enums.SpaceRoleEnum;
import yupicture.interfaces.vo.user.UserVO;
import com.yupi.yupicturebackend.model.vo.space.SpaceUserVO;
import com.yupi.yupicturebackend.model.vo.space.SpaceVO;
import com.yupi.yupicturebackend.service.SpaceService;
import com.yupi.yupicturebackend.service.SpaceUserService;
import yupicture.infrastructure.mapper.SpaceUserMapper;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static yupicture.infrastructure.exception.ErrorCode.PARAMS_ERROR;

/**
 * @author Ayaki
 * @description 针对表【space_user(空间用户关联)】的数据库操作Service实现
 * @createDate 2025-12-02 15:18:03
 */
@Service
public class SpaceUserServiceImpl extends ServiceImpl<SpaceUserMapper, SpaceUser> implements SpaceUserService {

    @Resource
    @Lazy
    private SpaceService spaceService;
    @Resource
    private UserApplicationService userApplicationService;


    /**
     * 创建空间成员
     *
     * @param spaceUserAddRequest
     * @return
     */
    @Override
    public Long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest) {
        // 类型转换
        SpaceUser spaceUser = BeanUtil.copyProperties(spaceUserAddRequest, SpaceUser.class);
        spaceUser.setSpaceRole(SpaceRoleEnum.VIEWER.getValue());
        // 参数校验
        validSpaceUser(spaceUser, true);
        // 数据库操作
        boolean save = this.save(spaceUser);
        ThrowUtils.throwIf(!save, ErrorCode.OPERATION_ERROR, "添加空间成员失败");
        //
        return spaceUser.getId();
    }

    /**
     * 删除空间成员
     *
     * @param deleteRequest
     */
    @Override
    public void deleteSpaceUser(DeleteRequest deleteRequest) {
        // 参数校验
        Long id = deleteRequest.getId();
        ThrowUtils.throwIf(ObjUtil.isEmpty(id), PARAMS_ERROR);
        // 空间成员是否存在
        SpaceUser spaceUser = this.getById(id);
        ThrowUtils.throwIf(ObjUtil.isEmpty(spaceUser), ErrorCode.NOT_FOUND_ERROR);
        boolean remove = this.removeById(id);
        ThrowUtils.throwIf(!remove, ErrorCode.OPERATION_ERROR, "删除空间成员失败");
    }

    /**
     * 修改空间成员角色
     *
     * @param spaceUserEditRequest
     */
    @Override
    public void editSpaceUser(SpaceUserEditRequest spaceUserEditRequest) {
        // 转换类型
        SpaceUser spaceUser = BeanUtil.copyProperties(spaceUserEditRequest, SpaceUser.class);
        // id是否为空
        Long id = spaceUser.getId();
        ThrowUtils.throwIf(ObjUtil.isEmpty(id), PARAMS_ERROR);
        // 空间成员是否存在
        SpaceUser oldSpaceUser = this.getById(spaceUser.getId());
        ThrowUtils.throwIf(ObjUtil.isEmpty(oldSpaceUser), ErrorCode.NOT_FOUND_ERROR);
        // 参数校验
        validSpaceUser(spaceUser, false);
        // 操作数据库
        boolean update = this.updateById(spaceUser);
        ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "修改空间成员角色失败");
    }

    /**
     * 封装用户成员信息VO
     *
     * @param spaceUser
     * @return
     */
    @Override
    public SpaceUserVO getSpaceUserVO(SpaceUser spaceUser) {
        // 参数校验
        ThrowUtils.throwIf(ObjUtil.isEmpty(spaceUser), PARAMS_ERROR);
        validSpaceUser(spaceUser, true);
        // 获取用户信息和空间想象
        User user = userApplicationService.getUserById(spaceUser.getUserId());
        Space space = spaceService.getById(spaceUser.getSpaceId());
        // 转换类型
        UserVO userVO = BeanUtil.copyProperties(user, UserVO.class);
        SpaceVO spaceVO = BeanUtil.copyProperties(space, SpaceVO.class);
        spaceVO.setUserVO(userVO);
        SpaceUserVO spaceUserVO = BeanUtil.copyProperties(spaceUser, SpaceUserVO.class);
        // 填充参数
        spaceUserVO.setUserVO(userVO);
        spaceUserVO.setSpace(spaceVO);
        return spaceUserVO;
    }

    /**
     * 获取用户成员列表
     *
     * @param spaceUserList
     * @return
     */
    @Override
    public List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList) {
        // 参数校验
        if (CollUtil.isEmpty(spaceUserList)) {
            return Collections.emptyList();
        }
        return spaceUserList.stream().map(this::getSpaceUserVO).collect(Collectors.toList());
    }

    /**
     * 获取查询条件
     *
     * @param spaceUserQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest) {
        // 没有查询条件就直接返回
        QueryWrapper<SpaceUser> spaceUserQueryWrapper = new QueryWrapper<>();
        if (spaceUserQueryRequest == null) {
            return spaceUserQueryWrapper;
        }
        // 获得参数
        Long id = spaceUserQueryRequest.getId();
        Long spaceId = spaceUserQueryRequest.getSpaceId();
        Long userId = spaceUserQueryRequest.getUserId();
        Integer spaceRole = spaceUserQueryRequest.getSpaceRole();
        // 封装查询条件并返回
        return spaceUserQueryWrapper
                .eq(ObjUtil.isNotEmpty(id), "id", id)
                .eq(ObjUtil.isNotEmpty(spaceId), "spaceId", spaceId)
                .eq(ObjUtil.isNotEmpty(userId), "userId", userId)
                .eq(ObjUtil.isNotEmpty(spaceRole), "spaceRole", spaceRole);
    }


    /**
     * 校验成员信息有效性(创建or编辑)
     *
     * @param spaceUser
     * @param add
     */
    @Override
    public void validSpaceUser(SpaceUser spaceUser, boolean add) {
        // 创建校验
        if (add) {
            // 用户是否存在
            Long userId = spaceUser.getUserId();
            ThrowUtils.throwIf(ObjUtil.isEmpty(userId) || userId <= 0, PARAMS_ERROR, "用户id不能为空");
            User user = userApplicationService.getUserById(userId);
            ThrowUtils.throwIf(ObjUtil.isEmpty(user), ErrorCode.NOT_FOUND_ERROR, "用户不存在");
            // 空间是否存在
            Long spaceId = spaceUser.getSpaceId();
            ThrowUtils.throwIf(ObjUtil.isEmpty(spaceId) || spaceId <= 0, PARAMS_ERROR, "空间id不能为空");
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(ObjUtil.isEmpty(space), ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        }
        // 修改校验
        String spaceRole = spaceUser.getSpaceRole();
        SpaceRoleEnum spaceRoleEnum = SpaceRoleEnum.getEnumByValue(spaceRole);
        ThrowUtils.throwIf(ObjUtil.hasEmpty(spaceRole, spaceRoleEnum), PARAMS_ERROR, "空间角色不存在");
    }
}




