package com.yupi.yupicturebackend.auth;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.yupi.yupicturebackend.auth.model.SpaceUserAuthConfig;
import com.yupi.yupicturebackend.auth.model.SpaceUserPermissionConstant;
import com.yupi.yupicturebackend.auth.model.SpaceUserRole;
import com.yupi.yupicturebackend.model.entity.Space;
import com.yupi.yupicturebackend.model.entity.SpaceUser;
import com.yupi.yupicturebackend.model.entity.User;
import com.yupi.yupicturebackend.model.enums.SpaceRoleEnum;
import com.yupi.yupicturebackend.model.enums.SpaceTypeEnum;
import com.yupi.yupicturebackend.service.SpaceUserService;
import com.yupi.yupicturebackend.service.UserService;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class SpaceUserAuthManager {

    public static final SpaceUserAuthConfig SPACE_USER_AUTH_CONFIG;

    /**
     * 加载角色权限配置类
     */
    static {
        String json = ResourceUtil.readUtf8Str("riz/spaceUserAuthConfig.json");
        SPACE_USER_AUTH_CONFIG = JSONUtil.toBean(json, SpaceUserAuthConfig.class);
    }

    private final UserService userService;
    private final SpaceUserService spaceUserService;

    public SpaceUserAuthManager(UserService userService, SpaceUserService spaceUserService) {
        this.userService = userService;
        this.spaceUserService = spaceUserService;
    }

    /**
     * 根据当前用户的角色分配配置类中对应的权限
     *
     * @param spaceUserRole
     */
    public List<String> getPermissionsByRole(String spaceUserRole) {
        // 校验
        if (StrUtil.isBlank(spaceUserRole)) {
            return Collections.emptyList();
        }
        // 匹配用户的角色返回对应权限
        SpaceUserRole role = SPACE_USER_AUTH_CONFIG.getRoles().stream().filter(r -> r.getKey().equals(spaceUserRole)).findFirst()// 取出流中的第一个元素，避免空指针异常
                .orElse(null);
        // 无角色就是没权限
        if (role == null) {
            return Collections.emptyList();
        }
        return role.getPermissions();
    }

    /**
     * 根据space获取对应权限列表
     *
     * @param space
     * @param loginUser
     * @return
     */
    public List<String> getPermissionList(Space space, User loginUser) {
        // 是否登录
        if (loginUser == null) {
            return Collections.emptyList();
        }
        // 管理员权限
        List<String> ADMIN_PERMISSIONS = this.getPermissionsByRole(SpaceRoleEnum.ADMIN.getValue());
        // 公共图库
        if (ObjUtil.isEmpty(space)) {
            // 是否是管理员
            if (userService.isAdmin(loginUser)) {
                return ADMIN_PERMISSIONS;
            }
            // 只读
            return Collections.singletonList(SpaceUserPermissionConstant.PICTURE_VIEW);
        }
        // 校验空间类型
        SpaceTypeEnum enumByValue = SpaceTypeEnum.getEnumByValue(space.getSpaceType());
        if (enumByValue == null) {
            return Collections.emptyList();
        }
        switch (enumByValue) {
            // 私有空间
            case PRIVATE:
                // 仅管理员和本人有所有权限
                if (userService.isAdmin(loginUser) || loginUser.getId().equals(space.getUserId())) {
                    return ADMIN_PERMISSIONS;
                } else {
                    return Collections.emptyList();
                }
                // 团队空间
            case TEAM:
                // 查询当前用户是否为团队空间成员
                SpaceUser spaceUser = spaceUserService.lambdaQuery().eq(SpaceUser::getSpaceId, space.getId()).eq(SpaceUser::getUserId, loginUser.getId()).one();
                // 不是团队空间成员
                if (spaceUser == null) {
                    return Collections.emptyList();
                }
                return this.getPermissionsByRole(spaceUser.getSpaceRole());
        }
        // 未知空间类型无权限
        return Collections.emptyList();
    }
}

