package com.yupi.yupicture.domain.space.service.impl;

import cn.hutool.core.util.ObjUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import com.yupi.yupicture.application.service.SpaceApplicationService;
import com.yupi.yupicture.application.service.UserApplicationService;
import com.yupi.yupicture.domain.space.entity.SpaceUser;
import com.yupi.yupicture.domain.space.repository.SpaceUserRepository;
import com.yupi.yupicture.domain.space.service.SpaceUserDomainService;
import com.yupi.yupicture.infrastructure.common.DeleteRequest;
import com.yupi.yupicture.infrastructure.exception.ErrorCode;
import com.yupi.yupicture.infrastructure.exception.ThrowUtils;
import com.yupi.yupicture.interfaces.dto.spaceuser.SpaceUserQueryRequest;

import javax.annotation.Resource;
import java.util.List;

import static com.yupi.yupicture.infrastructure.exception.ErrorCode.PARAMS_ERROR;

/**
 * @author Ayaki
 * @description 针对表【space_user(空间用户关联)】的数据库操作Service实现
 * @createDate 2025-12-02 15:18:03
 */
@Service
public class SpaceUserDomainServiceImpl implements SpaceUserDomainService {

    @Resource
    @Lazy
    private SpaceApplicationService spaceApplicationService;
    @Resource
    private UserApplicationService userApplicationService;
    @Resource
    private SpaceUserRepository spaceUserRepository;

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
        SpaceUser spaceUser = spaceUserRepository.getById(id);
        ThrowUtils.throwIf(ObjUtil.isEmpty(spaceUser), ErrorCode.NOT_FOUND_ERROR);
        boolean remove = spaceUserRepository.removeById(id);
        ThrowUtils.throwIf(!remove, ErrorCode.OPERATION_ERROR, "删除空间成员失败");
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
     * 保存空间成员
     *
     * @param spaceUser
     * @return
     */
    @Override
    public boolean saveSpaceUser(SpaceUser spaceUser) {
        return spaceUserRepository.save(spaceUser);
    }

    /**
     * 根据id查询空间成员
     *
     * @param id
     * @return
     */
    @Override
    public SpaceUser getSpaceUserById(Long id) {
        return spaceUserRepository.getById(id);
    }

    /**
     * 修改空间成员
     * @param spaceUser
     * @return
     */
    @Override
    public boolean updateById(SpaceUser spaceUser) {
        return spaceUserRepository.updateById(spaceUser);
    }

    /**
     * 获取空间成员
     * @param queryWrapper
     * @return
     */
    @Override
    public List<SpaceUser> listSpaceUser(QueryWrapper<SpaceUser> queryWrapper) {
        return spaceUserRepository.list(queryWrapper);
    }

    @Override
    public List<SpaceUser> getListSpaceUser(SpaceUserQueryRequest spaceUserQueryRequest) {
        // 查询条件
        QueryWrapper<SpaceUser> queryWrapper = this.getQueryWrapper(spaceUserQueryRequest);
        // 查询数据库
        return spaceUserRepository.list(queryWrapper);
    }

    @Override
    public SpaceUser getOne(QueryWrapper<SpaceUser> queryWrapper) {
        return spaceUserRepository.getOne(queryWrapper);
    }

    @Override
    public SpaceUser lambdaQuerySpaceUser(LambdaQueryWrapper<SpaceUser> lambdaQueryWrapper) {
        return spaceUserRepository.getOne(lambdaQueryWrapper);
    }
}




