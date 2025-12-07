package com.yupi.yupicture.application.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.yupicture.shared.auth.SpaceUserAuthManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import com.yupi.yupicture.application.service.SpaceApplicationService;
import com.yupi.yupicture.application.service.SpaceUserApplicationService;
import com.yupi.yupicture.application.service.UserApplicationService;
import com.yupi.yupicture.domain.space.entity.Space;
import com.yupi.yupicture.domain.space.entity.SpaceUser;
import com.yupi.yupicture.domain.space.service.SpaceDomainService;
import com.yupi.yupicture.domain.space.valueobject.SpaceLevelEnum;
import com.yupi.yupicture.domain.space.valueobject.SpaceRoleEnum;
import com.yupi.yupicture.domain.space.valueobject.SpaceTypeEnum;
import com.yupi.yupicture.domain.user.entity.User;
import com.yupi.yupicture.infrastructure.common.DeleteRequest;
import com.yupi.yupicture.infrastructure.exception.BusinessException;
import com.yupi.yupicture.infrastructure.exception.ErrorCode;
import com.yupi.yupicture.infrastructure.exception.ThrowUtils;
import com.yupi.yupicture.interfaces.dto.space.SpaceAddRequest;
import com.yupi.yupicture.interfaces.dto.space.SpaceEditRequest;
import com.yupi.yupicture.interfaces.dto.space.SpaceQueryRequest;
import com.yupi.yupicture.interfaces.dto.space.SpaceUpdateRequest;
import com.yupi.yupicture.interfaces.vo.space.SpaceVO;
import com.yupi.yupicture.interfaces.vo.user.UserVO;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Ayaki
 * @description 针对表【space(空间)】的数据库操作Service实现
 * @createDate 2025-11-28 18:24:24
 */
@Service
public class SpaceApplicationServiceImpl implements SpaceApplicationService {

    @Resource
    private UserApplicationService userApplicationService;
    @Resource
    private TransactionTemplate transactionTemplate;
    @Resource
    private SpaceUserApplicationService spaceUserApplicationService;
    @Resource
    private SpaceDomainService spaceDomainService;
    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;
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
        QueryWrapper<Space> queryWrapper = spaceDomainService.getQueryWrapper(spaceQueryRequest);
        ThrowUtils.throwIf(ObjUtil.isEmpty(spaceQueryRequest), ErrorCode.PARAMS_ERROR);
        return queryWrapper;
    }

    /**
     * 空间转VO
     *
     * @param space
     * @param request
     * @return
     */
    @Override
    public SpaceVO getSpaceVO(Space space, HttpServletRequest request) {
        ///  VO 中包含创建空间的用户信息
        // 校验
        ThrowUtils.throwIf(ObjUtil.isEmpty(space), ErrorCode.NOT_FOUND_ERROR);
        // 转SpaceVO
        SpaceVO spaceVO = SpaceVO.objToVo(space);
        // 获取用户id
        Long userId = space.getUserId();
        // 校验
        if (userId != null && userId > 0) {
            // 获取用户信息-getById()
            User user = userApplicationService.getUserById(userId);
            UserVO userVO = userApplicationService.getUserVO(user);
            spaceVO.setUserVO(userVO);
        }
        return spaceVO;
    }

    /**
     * 空间分页查询转 VO
     *
     * @param spacePage
     * @param request
     * @return
     */
    @Override
    public Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request) {
        // 获得page
        List<Space> records = spacePage.getRecords();
        // 封装 VO 的分页参数
        Page<SpaceVO> spaceVOPage = new Page<>(spacePage.getCurrent(), spacePage.getSize(), spacePage.getTotal());
        // 校验
        if (CollUtil.isEmpty(records)) {
            return spaceVOPage;
        }
        // 封装 VO
        List<SpaceVO> spaceVOList = records.stream().map(SpaceVO::objToVo).collect(Collectors.toList());
        ///  一个用户可以有多个空间，使用 Map<UserId,List<SpaceVO>
        // 获取 userId 集合
        Set<Long> userIdSet = records.stream().map(Space::getUserId).collect(Collectors.toSet());
        // 可以使用 userId 匹配 user 的 map 集合
        Map<Long, List<User>> userIdUserListMap = userApplicationService.listByIds(userIdSet).stream().collect(Collectors.groupingBy(User::getId));
        // 封装 spaceVOList 的 user信息
        spaceVOList.forEach(spaceVO -> {
            // 获得空间关联的 userId
            Long userId = spaceVO.getUserId();
            // 查 user，1个id对应一个user所以是get(0)
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            // 把 userVO 对象封装进 SpaceVO 中
            spaceVO.setUserVO(userApplicationService.getUserVO(user));
        });
        // 返回
        return spaceVOPage.setRecords(spaceVOList);
    }

    /**
     * 根据空间级别，自动填充初始空间容量和数量
     *
     * @param space
     */
    @Override
    public void fillSpaceBySpaceLevel(Space space) {
        spaceDomainService.fillSpaceBySpaceLevel(space);
    }

    /**
     * 创建空间
     *
     * @param spaceAddRequest
     * @param loginUser
     */
    @Override
    public Long addSpace(SpaceAddRequest spaceAddRequest, User loginUser) {
        /// 校验
        // 转po
        Space space = BeanUtil.copyProperties(spaceAddRequest, Space.class);
        /// 用户注册自动创建空间，请求参数先传默认值
        if (StrUtil.isBlank(spaceAddRequest.getSpaceName())) {
            space.setSpaceName("默认空间");
        }
        // 普通版
        if (ObjUtil.isEmpty(spaceAddRequest.getSpaceLevel())) {
            space.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
        }
        // 不指定空间类型默认为私有空间
        if (ObjUtil.isEmpty(spaceAddRequest.getSpaceType())) {
            space.setSpaceType(SpaceTypeEnum.PRIVATE.getValue());
        }
        // 自动填充参数
        this.fillSpaceBySpaceLevel(space);
        // 空间校验
        space.validSpace(true);
        // 补充用户数据
        Long userId = loginUser.getId();
        space.setUserId(userId);
        // 权限校验-限管理员能创建付费级别
        if (spaceAddRequest.getSpaceLevel() != SpaceLevelEnum.COMMON.getValue() && !loginUser.isAdmin()) {
            // 不是管理员还创建付费级别-直接抛异常
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权创建指定级别的空间");
        }
        /// 创建空间-操作数据库-事务+锁
        String Lock = String.valueOf(userId).intern();
        synchronized (Lock) { // 锁
            Long newSpaceId = transactionTemplate.execute(status -> {
                boolean exists = spaceDomainService.spaceUserLambdaQuery(space);
                ThrowUtils.throwIf(exists, ErrorCode.OPERATION_ERROR, "每个用户每类空间仅能创建1个");
                // 写入数据库
                boolean result = spaceDomainService.saveSpace(space);
                ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "创建空间失败");
                // 创建团队空间时，将创建者的信息作为管理员插入到空间成员表中
                if (Objects.equals(space.getSpaceType(), SpaceTypeEnum.TEAM.getValue())) {
                    SpaceUser spaceUser = new SpaceUser();
                    spaceUser.setSpaceId(space.getId());
                    spaceUser.setUserId(space.getUserId());
                    spaceUser.setSpaceRole(SpaceRoleEnum.ADMIN.getValue());
                    // 插入数据库
                    boolean save = spaceUserApplicationService.saveSpaceUser(spaceUser);
                    ThrowUtils.throwIf(!save, ErrorCode.OPERATION_ERROR, "添加团队空间成员失败");
                }
                // 如果是旗舰版团队空间，则为其单独创建分表——为了方便部署，暂不使用分表
                // dynamicShardingManager.createSpacePictureTable(space);
                return space.getId();
            });
            // 校验为空就返回默认值 1L
            return Optional.ofNullable(newSpaceId).orElse(1L);
        }
    }

    /**
     * 校验空间权限
     *
     * @param loginUser
     * @param space
     */
    @Override
    public void checkSpaceAuth(User loginUser, Space space) {
        spaceDomainService.checkSpaceAuth(loginUser, space);
    }

    /**
     * 根据空间id获取空间
     *
     * @param spaceId
     * @return
     */
    @Override
    public Space getSpaceById(Long spaceId) {
        Space space = spaceDomainService.getSpaceById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        return space;
    }

    /**
     * 获取空间列表
     *
     * @param queryWrapper
     * @return
     */
    @Override
    public List<Space> getSpaceList(QueryWrapper<Space> queryWrapper) {
        List<Space> spaceList = spaceDomainService.getSpaceList(queryWrapper);
        ThrowUtils.throwIf(spaceList == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        return spaceList;
    }

    @Override
    public boolean spaceLambdaUpdate(LambdaUpdateWrapper<Space> spaceLambdaUpdateWrapper) {
        return spaceDomainService.spaceLambdaUpdate(spaceLambdaUpdateWrapper);
    }

    @Override
    public List<Space> lambdaQuerySpace(LambdaQueryWrapper<Space> queryWrapper) {
        return spaceDomainService.lambdaQuerySpace(queryWrapper);
    }

    @Override
    public boolean removeSpaceById(Long id) {
        return spaceDomainService.removeSpaceById(id);
    }

    @Override
    public boolean deleteSpace(DeleteRequest deleteRequest, HttpServletRequest request) {
        // 解析参数
        Long id = deleteRequest.getId();
        User loginUser = userApplicationService.getLoginUser(request);
        // 空间是否存在
        Space oldSpace = this.getSpaceById(id);
        ThrowUtils.throwIf(ObjUtil.isEmpty(oldSpace), ErrorCode.NOT_FOUND_ERROR);
        // 仅本人与管理员可删除
        this.checkSpaceAuth(loginUser, oldSpace);
        // 逻辑删除
        boolean removeResult = this.removeSpaceById(id);
        ThrowUtils.throwIf(!removeResult, ErrorCode.OPERATION_ERROR);
        return removeResult;
    }

    @Override
    public boolean updateSpaceById(Space space) {
        return spaceDomainService.updateSpaceById(space);
    }

    @Override
    public Page<Space> listSpaceByPage(SpaceQueryRequest spaceQueryRequest) {
        QueryWrapper<Space> queryWrapper = this.getQueryWrapper(spaceQueryRequest);
        return spaceDomainService.listSpaceByPage(spaceQueryRequest, queryWrapper);
    }

    @Override
    public Page<SpaceVO> listSpaceVOByPage(SpaceQueryRequest spaceQueryRequest, HttpServletRequest request) {
        QueryWrapper<Space> queryWrapper = this.getQueryWrapper(spaceQueryRequest);
        Page<Space> spacePage = spaceDomainService.listSpaceVOByPage(spaceQueryRequest, queryWrapper);
        // 空间分页转 VO
        return this.getSpaceVOPage(spacePage, request);
    }

    @Override
    public boolean updateSpace(SpaceUpdateRequest spaceUpdateRequest, Space space, HttpServletRequest request) {
        // 自动补充数据
        this.fillSpaceBySpaceLevel(space);
        // 校验空间，传 false 表示更新校验
        space.validSpace(false);
        // 校验需要更新数据是否存在
        Long id = spaceUpdateRequest.getId();
        Space oldSpace = this.getSpaceById(id);
        if (ObjUtil.isEmpty(oldSpace)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "更新的空间不存在");
        }
        // 更新空间-操作数据库
        boolean result = this.updateSpaceById(space);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "更新空间失败");
        return result;
    }

    /**
     * 编辑空间
     * @param spaceEditRequest
     * @param space
     * @param request
     * @return
     */
    @Override
    public boolean editSpace(SpaceEditRequest spaceEditRequest, Space space, HttpServletRequest request) {
        // 自动填充参数
        this.fillSpaceBySpaceLevel(space);
        // 设置编辑时间
        space.setEditTime(new Date());
        // 校验参数-false 表示编辑校验
        space.validSpace(false);
        // 校验编辑数据是否存在
        Long id = space.getId();
        Space oldSpace = this.getSpaceById(id);
        ThrowUtils.throwIf(ObjUtil.isEmpty(oldSpace), ErrorCode.NOT_FOUND_ERROR, "编辑的空间不存在");
        // 仅本人或管理员可编辑
        User loginUser = userApplicationService.getLoginUser(request);
        this.checkSpaceAuth(loginUser, space);
        /// 编辑
        // 编辑-操作数据库
        boolean result = this.updateSpaceById(oldSpace);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "编辑空间失败");
        return result;
    }

    @Override
    public SpaceVO getSpaceVOById(long id, HttpServletRequest request) {
        // 获取空间
        Space space = this.getSpaceById(id);
        ThrowUtils.throwIf(ObjUtil.isEmpty(space), ErrorCode.NOT_FOUND_ERROR);
        // 获得spaceVO
        SpaceVO spaceVO = this.getSpaceVO(space, request);
        User loginUser = userApplicationService.getLoginUser(request);
        // 获得权限列表
        List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);
        // 存入权限列表
        spaceVO.setPermissionList(permissionList);
        // 存入UserVO
        spaceVO.setUserVO(userApplicationService.getUserVO(loginUser));
        return spaceVO;
    }
}




