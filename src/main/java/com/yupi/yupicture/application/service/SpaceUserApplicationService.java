package com.yupi.yupicture.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yupi.yupicture.domain.space.entity.SpaceUser;
import com.yupi.yupicture.infrastructure.common.DeleteRequest;
import com.yupi.yupicture.interfaces.dto.spaceuser.SpaceUserAddRequest;
import com.yupi.yupicture.interfaces.dto.spaceuser.SpaceUserEditRequest;
import com.yupi.yupicture.interfaces.dto.spaceuser.SpaceUserQueryRequest;
import com.yupi.yupicture.interfaces.vo.space.SpaceUserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author Ayaki
 * @description 针对表【space_user(空间用户关联)】的数据库操作Service
 * @createDate 2025-12-02 15:18:03
 */
public interface SpaceUserApplicationService{

    /**
     * 创建空间成员
     *
     * @param spaceUserAddRequest
     * @return
     */
    Long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest);

    /**
     * 删除空间成员
     *
     * @param deleteRequest
     */
    void deleteSpaceUser(DeleteRequest deleteRequest);

    /**
     * 修改空间成员
     *
     * @param spaceUserEditRequest
     */
    void editSpaceUser(SpaceUserEditRequest spaceUserEditRequest);

    /**
     * 获得单个空间成员信息
     *
     * @param spaceUser
     * @return
     */
    SpaceUserVO getSpaceUserVO(SpaceUser spaceUser);

    /**
     * 获得空间成员信息列表
     *
     * @param spaceUserList
     * @return
     */
    List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList);

    /**
     * 封装查询条件
     *
     * @param spaceUserQueryRequest
     * @return
     */
    QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest);

    /**
     * 校验空间成员信息有效性
     *
     * @param spaceUser
     * @param add
     */
    void validSpaceUser(SpaceUser spaceUser, boolean add);

    /**
     * 保存空间成员信息
     * @param spaceUser
     * @return
     */
    boolean saveSpaceUser(SpaceUser spaceUser);

    /**
     * 获取我加入的空间列表
     * @param request
     * @return
     */
    List<SpaceUserVO> listMySpaceUser(HttpServletRequest request);

    /**
     * 获取空间成员列表
     * @param spaceUserQueryRequest
     * @return
     */
    List<SpaceUserVO> getListSpaceUser(SpaceUserQueryRequest spaceUserQueryRequest);

    /**
     * 获取空间成员信息
     * @param spaceUserQueryRequest
     * @return
     */
    SpaceUser getSpaceUser(SpaceUserQueryRequest spaceUserQueryRequest);

    /**
     * 获取空间成员信息
     * @param lambdaQueryWrapper
     * @return
     */
    SpaceUser lambdaQuerySpaceUser(LambdaQueryWrapper<SpaceUser> lambdaQueryWrapper);

    /**
     * 获取空间成员信息
     * @param spaceUserId
     * @return
     */
    SpaceUser getSpaceUserById(Long spaceUserId);
}
