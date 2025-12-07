package com.yupi.yupicture.application.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import com.yupi.yupicture.application.service.SpaceApplicationService;
import com.yupi.yupicture.application.service.SpaceUserApplicationService;
import com.yupi.yupicture.application.service.UserApplicationService;
import com.yupi.yupicture.domain.space.entity.Space;
import com.yupi.yupicture.domain.space.entity.SpaceUser;
import com.yupi.yupicture.domain.space.service.SpaceUserDomainService;
import com.yupi.yupicture.domain.space.valueobject.SpaceRoleEnum;
import com.yupi.yupicture.domain.user.entity.User;
import com.yupi.yupicture.infrastructure.common.DeleteRequest;
import com.yupi.yupicture.infrastructure.exception.ErrorCode;
import com.yupi.yupicture.infrastructure.exception.ThrowUtils;
import com.yupi.yupicture.interfaces.dto.spaceuser.SpaceUserAddRequest;
import com.yupi.yupicture.interfaces.dto.spaceuser.SpaceUserEditRequest;
import com.yupi.yupicture.interfaces.dto.spaceuser.SpaceUserQueryRequest;
import com.yupi.yupicture.interfaces.vo.space.SpaceUserVO;
import com.yupi.yupicture.interfaces.vo.space.SpaceVO;
import com.yupi.yupicture.interfaces.vo.user.UserVO;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.yupi.yupicture.infrastructure.exception.ErrorCode.PARAMS_ERROR;

/**
 * @author Ayaki
 * @description 针对表【space_user(空间用户关联)】的数据库操作Service实现
 * @createDate 2025-12-02 15:18:03
 */
@Service
public class SpaceUserApplicationServiceImpl implements SpaceUserApplicationService {

    @Resource
    @Lazy
    private SpaceApplicationService spaceApplicationService;
    @Resource
    private UserApplicationService userApplicationService;
    @Resource
    private SpaceUserDomainService spaceUserDomainService;


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
        boolean save = spaceUserDomainService.saveSpaceUser(spaceUser);
        ThrowUtils.throwIf(!save, ErrorCode.OPERATION_ERROR, "添加空间成员失败");
        // 返回id
        return spaceUser.getId();
    }

    /**
     * 删除空间成员
     *
     * @param deleteRequest
     */
    @Override
    public void deleteSpaceUser(DeleteRequest deleteRequest) {
        spaceUserDomainService.deleteSpaceUser(deleteRequest);
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
        SpaceUser oldSpaceUser = spaceUserDomainService.getSpaceUserById(spaceUser.getId());
        ThrowUtils.throwIf(ObjUtil.isEmpty(oldSpaceUser), ErrorCode.NOT_FOUND_ERROR);
        // 参数校验
        validSpaceUser(spaceUser, false);
        // 操作数据库
        boolean update = spaceUserDomainService.updateById(spaceUser);
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
        Space space = spaceApplicationService.getSpaceById(spaceUser.getSpaceId());
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
        return spaceUserDomainService.getQueryWrapper(spaceUserQueryRequest);
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
            Space space = spaceApplicationService.getSpaceById(spaceId);
            ThrowUtils.throwIf(ObjUtil.isEmpty(space), ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        }
        // 修改校验
        String spaceRole = spaceUser.getSpaceRole();
        SpaceRoleEnum spaceRoleEnum = SpaceRoleEnum.getEnumByValue(spaceRole);
        ThrowUtils.throwIf(ObjUtil.hasEmpty(spaceRole, spaceRoleEnum), PARAMS_ERROR, "空间角色不存在");
    }

    @Override
    public boolean saveSpaceUser(SpaceUser spaceUser) {
        return spaceUserDomainService.saveSpaceUser(spaceUser);
    }

    @Override
    public List<SpaceUserVO> listMySpaceUser(HttpServletRequest request) {
        // 校验登录用户
        User loginUser = userApplicationService.getLoginUser(request);
        ThrowUtils.throwIf(ObjUtil.isEmpty(loginUser), ErrorCode.NOT_LOGIN_ERROR);
        // 创建查询条件
        SpaceUserQueryRequest spaceUserQueryRequest = new SpaceUserQueryRequest();
        spaceUserQueryRequest.setUserId(loginUser.getId());
        QueryWrapper<SpaceUser> queryWrapper = this.getQueryWrapper(spaceUserQueryRequest);
        // 查询数据库
        List<SpaceUser> list = spaceUserDomainService.listSpaceUser(queryWrapper);
        // 封装VO列表
        List<SpaceUserVO> spaceUserVOList = this.getSpaceUserVOList(list);
//        ThrowUtils.throwIf(CollUtil.isEmpty(spaceUserVOList), ErrorCode.NOT_FOUND_ERROR);
        return spaceUserVOList;
    }

    @Override
    public List<SpaceUserVO> getListSpaceUser(SpaceUserQueryRequest spaceUserQueryRequest) {
        List<SpaceUser> spaceUserList = spaceUserDomainService.getListSpaceUser(spaceUserQueryRequest);
        List<SpaceUserVO> spaceUserVOList = this.getSpaceUserVOList(spaceUserList);
        ThrowUtils.throwIf(CollUtil.isEmpty(spaceUserVOList), ErrorCode.NOT_FOUND_ERROR);
        return spaceUserVOList;
    }

    @Override
    public SpaceUser getSpaceUser(SpaceUserQueryRequest spaceUserQueryRequest) {
        // 获取参数
        Long spaceId = spaceUserQueryRequest.getSpaceId();
        Long userId = spaceUserQueryRequest.getUserId();
        ThrowUtils.throwIf(ObjUtil.hasEmpty(spaceId, userId), ErrorCode.PARAMS_ERROR);
        // 查询条件
        QueryWrapper<SpaceUser> queryWrapper = this.getQueryWrapper(spaceUserQueryRequest);
        SpaceUser spaceUser = spaceUserDomainService.getOne(queryWrapper);
        ThrowUtils.throwIf(ObjUtil.isEmpty(spaceUser), ErrorCode.NOT_FOUND_ERROR);
        return spaceUser;
    }

    @Override
    public SpaceUser lambdaQuerySpaceUser(LambdaQueryWrapper<SpaceUser> lambdaQueryWrapper) {
        return spaceUserDomainService.lambdaQuerySpaceUser(lambdaQueryWrapper);
    }

    @Override
    public SpaceUser getSpaceUserById(Long spaceUserId) {
        return spaceUserDomainService.getSpaceUserById(spaceUserId);
    }
}




