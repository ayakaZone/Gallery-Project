package com.yupi.yupicture.domain.space.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.yupicture.domain.space.entity.Space;
import com.yupi.yupicture.domain.user.entity.User;
import com.yupi.yupicture.interfaces.dto.space.SpaceQueryRequest;

import java.util.List;

/**
 * @author Ayaki
 * @description 针对表【space(空间)】的数据库操作Service
 * @createDate 2025-11-28 18:24:24
 */
public interface SpaceDomainService {
    /**
     * 封装查询条件
     *
     * @param spaceQueryRequest
     * @return
     */
    QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);

    /**
     * 根据空间级别，自动填充初始空间容量和数量
     *
     * @param space
     */
    void fillSpaceBySpaceLevel(Space space);

    /**
     * 校验空间权限
     *
     * @param loginUser
     * @param space
     */
    void checkSpaceAuth(User loginUser, Space space);

    /**
     * 根据id获取空间
     *
     * @param spaceId
     * @return
     */
    Space getSpaceById(Long spaceId);

    /**
     * 获取空间列表
     *
     * @param queryWrapper
     */
    List<Space> getSpaceList(QueryWrapper<Space> queryWrapper);

    /**
     * 保存空间
     *
     * @param space
     * @return
     */
    boolean saveSpace(Space space);

    /**
     * 获取
     *
     * @return
     */
    boolean spaceUserLambdaQuery(Space space);

    /**
     * 更新空间
     *
     * @param spaceLambdaUpdateWrapper
     * @return
     */
    boolean spaceLambdaUpdate(LambdaUpdateWrapper<Space> spaceLambdaUpdateWrapper);

    /**
     * 查询空间
     *
     * @param queryWrapper
     * @return
     */
    List<Space> lambdaQuerySpace(LambdaQueryWrapper<Space> queryWrapper);

    /**
     * 删除空间
     * @param id
     * @return
     */
    boolean removeSpaceById(Long id);

    /**
     * 更新空间
     * @param space
     * @return
     */
    boolean updateSpaceById(Space space);

    /**
     * 分页查询空间
     *
     * @param queryRequest
     * @param spaceQueryRequest
     * @return
     */
    Page<Space> listSpaceByPage(SpaceQueryRequest queryRequest, QueryWrapper<Space> spaceQueryRequest);

    /**
     * 分页查询空间VO
     * @param spaceQueryRequest
     * @param queryWrapper
     * @return
     */
    Page<Space> listSpaceVOByPage(SpaceQueryRequest spaceQueryRequest, QueryWrapper<Space> queryWrapper);
}
