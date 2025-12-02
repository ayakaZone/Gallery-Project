package com.yupi.yupicturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.yupicturebackend.exception.BusinessException;
import com.yupi.yupicturebackend.exception.ErrorCode;
import com.yupi.yupicturebackend.exception.ThrowUtils;
import com.yupi.yupicturebackend.mapper.SpaceMapper;
import com.yupi.yupicturebackend.model.dto.space.SpaceAddRequest;
import com.yupi.yupicturebackend.model.dto.space.SpaceQueryRequest;
import com.yupi.yupicturebackend.model.entity.Space;
import com.yupi.yupicturebackend.model.entity.SpaceUser;
import com.yupi.yupicturebackend.model.entity.User;
import com.yupi.yupicturebackend.model.enums.SpaceLevelEnum;
import com.yupi.yupicturebackend.model.enums.SpaceRoleEnum;
import com.yupi.yupicturebackend.model.enums.SpaceTypeEnum;
import com.yupi.yupicturebackend.model.vo.space.SpaceVO;
import com.yupi.yupicturebackend.model.vo.UserVO;
import com.yupi.yupicturebackend.service.SpaceService;
import com.yupi.yupicturebackend.service.SpaceUserService;
import com.yupi.yupicturebackend.service.UserService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

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
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space> implements SpaceService {

    @Resource
    private UserService userService;
    @Resource
    private TransactionTemplate transactionTemplate;
    @Resource
    private SpaceUserService spaceUserService;

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
        QueryWrapper<Space> queryWrapper = QueryWrapper
                .eq(ObjUtil.isNotEmpty(id), "id", id)
                .eq(ObjUtil.isNotEmpty(userId), "userId", userId)
                .eq(ObjUtil.isNotEmpty(spaceLevel), "spaceLevel", spaceLevel)
                .eq(ObjUtil.isNotEmpty(spaceType), "spaceType", spaceType)
                .like(StrUtil.isNotEmpty(spaceName), "spaceName", spaceName);
        // 排序条件
        queryWrapper.orderBy(StrUtil.isNotBlank(sortField), sortOrder.equals("ascend"), sortField);
        // 返回
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
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
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
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream().collect(Collectors.groupingBy(User::getId));
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
            spaceVO.setUserVO(userService.getUserVO(user));
        });
        // 返回
        return spaceVOPage.setRecords(spaceVOList);
    }

    /**
     * 空间参数校验
     *
     * @param space
     * @param add
     */
    @Override
    public void validSpace(Space space, boolean add) {
        /// 校验
        ThrowUtils.throwIf(ObjUtil.isEmpty(space), ErrorCode.PARAMS_ERROR);
        // 获取参数
        String spaceName = space.getSpaceName();
        Integer spaceLevel = space.getSpaceLevel();
        Integer spaceType = space.getSpaceType();
        // 获取枚举说明
        SpaceLevelEnum spaceLevelEnumByValue = SpaceLevelEnum.getEnumByValue(spaceLevel);
        SpaceTypeEnum spaceTypeEnumValue = SpaceTypeEnum.getEnumByValue(spaceType);
        // 增加空间还是更新空间
        if (add) {
            // 增加：名称不为空，级别不为空
            if (StrUtil.isBlank(spaceName)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称不能为空");
            }
            if (ObjUtil.isEmpty(spaceLevel)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间级别不能为空");
            }
            // 空间类型
            if (ObjUtil.isEmpty(spaceType)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间类型不能为空");
            }
        }
        // 更新：名称不为空，级别不为空，名称小于30字符
        if (ObjUtil.isNotEmpty(spaceLevel) && ObjUtil.isEmpty(spaceLevelEnumByValue)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间级别不存在");
        }
        if (StrUtil.isBlank(spaceName) && spaceName.length() > 30) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称不能超过30个字符");
        }
        if (ObjUtil.isNotEmpty(spaceType) && ObjUtil.isEmpty(spaceTypeEnumValue)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间级别不存在");
        }

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
        this.validSpace(space, true);
        // 补充用户数据
        Long userId = loginUser.getId();
        space.setUserId(userId);
        // 权限校验-限管理员能创建付费级别
        if (spaceAddRequest.getSpaceLevel() != SpaceLevelEnum.COMMON.getValue() && !userService.isAdmin(loginUser)) {
            // 不是管理员还创建付费级别-直接抛异常
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权创建指定级别的空间");
        }
        /// 创建空间-操作数据库-事务+锁
        String Lock = String.valueOf(userId).intern();
        synchronized (Lock) { // 锁
            Long newSpaceId = transactionTemplate.execute(status -> {
                boolean exists = lambdaQuery()
                        .eq(Space::getUserId, space.getUserId())
                        .eq(Space::getSpaceType, space.getSpaceType())
                        .exists();
                ThrowUtils.throwIf(exists, ErrorCode.OPERATION_ERROR, "每个用户每类空间仅能创建1个");
                // 写入数据库
                boolean result = save(space);
                ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "创建空间失败");
                // 创建团队空间时，将创建者的信息作为管理员插入到空间成员表中
                if (Objects.equals(space.getSpaceType(), SpaceTypeEnum.TEAM.getValue())) {
                    SpaceUser spaceUser = new SpaceUser();
                    spaceUser.setSpaceId(space.getId());
                    spaceUser.setUserId(space.getUserId());
                    spaceUser.setSpaceRole(SpaceRoleEnum.ADMIN.getValue());
                    // 插入数据库
                    boolean save = spaceUserService.save(spaceUser);
                    ThrowUtils.throwIf(!save, ErrorCode.OPERATION_ERROR, "添加团队空间成员失败");
                }
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
        if (!loginUser.getId().equals(space.getUserId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权编辑空间");
        }
    }
}




