package com.yupi.yupicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.yupicturebackend.common.DeleteRequest;
import com.yupi.yupicturebackend.model.dto.space.SpaceAddRequest;
import com.yupi.yupicturebackend.model.dto.space.SpaceEditRequest;
import com.yupi.yupicturebackend.model.dto.space.SpaceQueryRequest;
import com.yupi.yupicturebackend.model.dto.spaceuser.SpaceUserAddRequest;
import com.yupi.yupicturebackend.model.dto.spaceuser.SpaceUserEditRequest;
import com.yupi.yupicturebackend.model.dto.spaceuser.SpaceUserQueryRequest;
import com.yupi.yupicturebackend.model.entity.Space;
import com.yupi.yupicturebackend.model.entity.SpaceUser;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.yupicturebackend.model.entity.User;
import com.yupi.yupicturebackend.model.vo.space.SpaceUserVO;
import com.yupi.yupicturebackend.model.vo.space.SpaceVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author Ayaki
 * @description 针对表【space_user(空间用户关联)】的数据库操作Service
 * @createDate 2025-12-02 15:18:03
 */
public interface SpaceUserService extends IService<SpaceUser> {

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
}
