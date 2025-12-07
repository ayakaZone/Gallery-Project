package com.yupi.yupicture.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.yupicture.domain.space.entity.Space;
import com.yupi.yupicture.domain.user.entity.User;
import com.yupi.yupicture.infrastructure.common.DeleteRequest;
import com.yupi.yupicture.interfaces.dto.space.SpaceAddRequest;
import com.yupi.yupicture.interfaces.dto.space.SpaceEditRequest;
import com.yupi.yupicture.interfaces.dto.space.SpaceQueryRequest;
import com.yupi.yupicture.interfaces.dto.space.SpaceUpdateRequest;
import com.yupi.yupicture.interfaces.vo.space.SpaceVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author Ayaki
 * @description 针对表【space(空间)】的数据库操作Service
 * @createDate 2025-11-28 18:24:24
 */
public interface SpaceApplicationService {
    /**
     * 封装查询条件
     *
     * @param spaceQueryRequest
     * @return
     */
    QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);

    /**
     * Space PO 转 VO
     *
     * @param space
     * @param request
     * @return
     */
    SpaceVO getSpaceVO(Space space, HttpServletRequest request);

    /**
     * Space分页PO转VO
     *
     * @param spacePage
     * @param request
     * @return
     */
    Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request);

    /**
     * 根据空间级别，自动填充初始空间容量和数量
     *
     * @param space
     */
    void fillSpaceBySpaceLevel(Space space);

    /**
     * 创建空间
     *
     * @param spaceAddRequest
     * @param loginUser
     */
    Long addSpace(SpaceAddRequest spaceAddRequest, User loginUser);

    /**
     * 校验空间权限
     *
     * @param loginUser
     * @param space
     */
    void checkSpaceAuth(User loginUser, Space space);

    /**
     * 根据id获取空间
     * @param spaceId
     * @return
     */
    Space getSpaceById(Long spaceId);

    /**
     * 获取空间列表
     * @param queryWrapper
     * @return
     */
    List<Space> getSpaceList(QueryWrapper<Space> queryWrapper);

    /**
     * 更新空间
     * @param spaceLambdaUpdateWrapper
     * @return
     */
    boolean spaceLambdaUpdate(LambdaUpdateWrapper<Space> spaceLambdaUpdateWrapper);

    /**
     * 查询空间
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
     * 删除空间
     * @param deleteRequest
     * @param request
     * @return
     */
    boolean deleteSpace(DeleteRequest deleteRequest, HttpServletRequest request);

    /**
     * 更新空间
     * @param space
     * @return
     */
    boolean updateSpaceById(Space space);

    /**
     * 分页查询空间
     * @param spaceQueryRequest
     * @return
     */
    Page<Space> listSpaceByPage(SpaceQueryRequest spaceQueryRequest);

    /**
     * 分页查询空间
     *
     * @param spaceQueryRequest
     * @param request
     * @return
     */
    Page<SpaceVO> listSpaceVOByPage(SpaceQueryRequest spaceQueryRequest, HttpServletRequest request);

    /**
     * 更新空间
     *
     * @param spaceUpdateRequest
     * @param space
     * @param request
     * @return
     */
    boolean updateSpace(SpaceUpdateRequest spaceUpdateRequest, Space space, HttpServletRequest request);

    /**
     * 编辑空间
     * @param spaceEditRequest
     * @param space
     * @param request
     * @return
     */
    boolean editSpace(SpaceEditRequest spaceEditRequest, Space space, HttpServletRequest request);

    /**
     * 获取空间
     * @param id
     * @param request
     * @return
     */
    SpaceVO getSpaceVOById(long id, HttpServletRequest request);
}
