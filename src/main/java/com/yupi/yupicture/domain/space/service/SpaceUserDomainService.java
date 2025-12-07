package com.yupi.yupicture.domain.space.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yupi.yupicture.domain.space.entity.SpaceUser;
import com.yupi.yupicture.infrastructure.common.DeleteRequest;
import com.yupi.yupicture.interfaces.dto.spaceuser.SpaceUserQueryRequest;

import java.util.List;

/**
 * @author Ayaki
 * @description 针对表【space_user(空间用户关联)】的数据库操作Service
 * @createDate 2025-12-02 15:18:03
 */
public interface SpaceUserDomainService{

    /**
     * 删除空间成员
     *
     * @param deleteRequest
     */
    void deleteSpaceUser(DeleteRequest deleteRequest);

    /**
     * 封装查询条件
     *
     * @param spaceUserQueryRequest
     * @return
     */
    QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest);

    /**
     * 保存空间成员信息
     * @param spaceUser
     * @return
     */
    boolean saveSpaceUser(SpaceUser spaceUser);

    /**
     * 获取空间成员信息
     * @param id
     * @return
     */
    SpaceUser getSpaceUserById(Long id);


    /**
     * 修改空间成员信息
     * @param spaceUser
     * @return
     */
    boolean updateById(SpaceUser spaceUser);

    /**
     * 获取空间成员
     * @param queryWrapper
     * @return
     */
    List<SpaceUser> listSpaceUser(QueryWrapper<SpaceUser> queryWrapper);

    /**
     * 获取空间成员列表
     * @param spaceUserQueryRequest
     * @return
     */
    List<SpaceUser> getListSpaceUser(SpaceUserQueryRequest spaceUserQueryRequest);

    /**
     * 获取空间成员
     * @param queryWrapper
     * @return
     */
    SpaceUser getOne(QueryWrapper<SpaceUser> queryWrapper);

    /**
     * 获取空间成员
     * @param lambdaQueryWrapper
     * @return
     */
    SpaceUser lambdaQuerySpaceUser(LambdaQueryWrapper<SpaceUser> lambdaQueryWrapper);
}
