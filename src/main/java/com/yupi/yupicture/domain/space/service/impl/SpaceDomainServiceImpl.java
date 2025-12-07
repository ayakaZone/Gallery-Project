package com.yupi.yupicture.domain.space.service.impl;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import com.yupi.yupicture.domain.space.entity.Space;
import com.yupi.yupicture.domain.space.repository.SpaceRepository;
import com.yupi.yupicture.domain.space.service.SpaceDomainService;
import com.yupi.yupicture.domain.space.valueobject.SpaceLevelEnum;
import com.yupi.yupicture.domain.user.entity.User;
import com.yupi.yupicture.infrastructure.exception.BusinessException;
import com.yupi.yupicture.infrastructure.exception.ErrorCode;
import com.yupi.yupicture.infrastructure.exception.ThrowUtils;
import com.yupi.yupicture.interfaces.dto.space.SpaceQueryRequest;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author Ayaki
 * @description 针对表【space(空间)】的数据库操作Service实现
 * @createDate 2025-11-28 18:24:24
 */
@Service
public class SpaceDomainServiceImpl implements SpaceDomainService {

    @Resource
    private SpaceRepository spaceRepository;

    // 为了方便部署、不使用分表
//    @Resource
//    @Lazy
//    private DynamicShardingManager dynamicShardingManager;

    /**
     * 空间查询条件
     *
     * @param spaceQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest) {
        // 校验
        ThrowUtils.throwIf(ObjUtil.isEmpty(spaceQueryRequest), ErrorCode.PARAMS_ERROR);
        // 查询参数
        Long id = spaceQueryRequest.getId();
        String spaceName = spaceQueryRequest.getSpaceName();
        Long userId = spaceQueryRequest.getUserId();
        Integer spaceLevel = spaceQueryRequest.getSpaceLevel();
        String sortField = spaceQueryRequest.getSortField();
        String sortOrder = spaceQueryRequest.getSortOrder();
        Integer spaceType = spaceQueryRequest.getSpaceType();
        /// 查询条件
        QueryWrapper<Space> QueryWrapper = new QueryWrapper<>();
        // 普通查询条件
        QueryWrapper<Space> queryWrapper = QueryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id).eq(ObjUtil.isNotEmpty(userId), "userId", userId).eq(ObjUtil.isNotEmpty(spaceLevel), "spaceLevel", spaceLevel).eq(ObjUtil.isNotEmpty(spaceType), "spaceType", spaceType).like(StrUtil.isNotEmpty(spaceName), "spaceName", spaceName);
        // 排序条件
        queryWrapper.orderBy(StrUtil.isNotBlank(sortField), sortOrder.equals("ascend"), sortField);
        // 返回
        return queryWrapper;
    }

    /**
     * 根据空间级别，自动填充初始空间容量和数量
     *
     * @param space
     */
    @Override
    public void fillSpaceBySpaceLevel(Space space) {
        // 从空间级别枚举类中获得初始参数
        SpaceLevelEnum enumByValue = SpaceLevelEnum.getEnumByValue(space.getSpaceLevel());
        // 校验管理员是否指定了空间容量
        if (ObjUtil.isNotEmpty(enumByValue)) {
            long maxCount = enumByValue.getMaxCount();
            long maxSize = enumByValue.getMaxSize();
            // 管理员没指定就设置初始值
            if (ObjUtil.isEmpty(space.getMaxCount())) {
                space.setMaxCount(maxCount);
            }
            if (ObjUtil.isEmpty(space.getMaxSize())) {
                space.setMaxSize(maxSize);
            }
        }
        // 指定了就使用管理员设置的空间容量
    }


    /**
     * 校验空间权限
     *
     * @param loginUser
     * @param space
     */
    @Override
    public void checkSpaceAuth(User loginUser, Space space) {
        if (!loginUser.getId().equals(space.getUserId()) && !loginUser.isAdmin()) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权编辑空间");
        }
    }

    /**
     * 获取空间
     *
     * @param spaceId
     * @return
     */
    @Override
    public Space getSpaceById(Long spaceId) {
        return spaceRepository.getById(spaceId);
    }

    /**
     * 获取空间列表
     *
     * @param queryWrapper
     * @return
     */
    @Override
    public List<Space> getSpaceList(QueryWrapper<Space> queryWrapper) {
        return spaceRepository.list(queryWrapper);
    }

    /**
     * 保存空间
     *
     * @param space
     * @return
     */
    @Override
    public boolean saveSpace(Space space) {
        return spaceRepository.save(space);
    }

    /**
     * 查询空间成员
     *
     * @param space
     * @return
     */
    @Override
    public boolean spaceUserLambdaQuery(Space space) {
        return spaceRepository.lambdaQuery().eq(Space::getUserId, space.getUserId()).eq(Space::getSpaceType, space.getSpaceType()).exists();
    }

    /**
     * 更新空间
     *
     * @param spaceLambdaUpdateWrapper
     * @return
     */
    @Override
    public boolean spaceLambdaUpdate(LambdaUpdateWrapper<Space> spaceLambdaUpdateWrapper) {
        return spaceRepository.update(spaceLambdaUpdateWrapper);
    }

    /**
     * 查询空间
     *
     * @param queryWrapper
     * @return
     */
    @Override
    public List<Space> lambdaQuerySpace(LambdaQueryWrapper<Space> queryWrapper) {
        return spaceRepository.list(queryWrapper);
    }

    @Override
    public boolean removeSpaceById(Long id) {
        return spaceRepository.removeById(id);
    }

    @Override
    public boolean updateSpaceById(Space space) {
        return spaceRepository.updateById(space);
    }

    @Override
    public Page<Space> listSpaceByPage(SpaceQueryRequest queryRequest, QueryWrapper<Space> spaceQueryRequest) {
        // 页面条件
        int current = queryRequest.getCurrent();
        int pageSize = queryRequest.getPageSize();
        // 分页查询
        return spaceRepository.page(new Page<>(current, pageSize), spaceQueryRequest);
    }

    @Override
    public Page<Space> listSpaceVOByPage(SpaceQueryRequest spaceQueryRequest, QueryWrapper<Space> queryWrapper) {
        // 页面条件
        int current = spaceQueryRequest.getCurrent();
        int pageSize = spaceQueryRequest.getPageSize();
        // 分页查询
        return spaceRepository.page(new Page<>(current, pageSize),queryWrapper);
    }
}




