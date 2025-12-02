package com.yupi.yupicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.yupicturebackend.model.dto.space.SpaceAddRequest;
import com.yupi.yupicturebackend.model.dto.space.SpaceQueryRequest;
import com.yupi.yupicturebackend.model.entity.Space;
import com.yupi.yupicturebackend.model.entity.User;
import com.yupi.yupicturebackend.model.vo.space.SpaceVO;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Ayaki
 * @description 针对表【space(空间)】的数据库操作Service
 * @createDate 2025-11-28 18:24:24
 */
public interface SpaceService extends IService<Space> {
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
     * 空间校验
     *
     * @param space
     */
    void validSpace(Space space, boolean add);

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
}
