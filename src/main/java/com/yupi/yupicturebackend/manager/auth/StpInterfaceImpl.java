package com.yupi.yupicturebackend.manager.auth;

import cn.dev33.satoken.stp.StpInterface;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import cn.hutool.json.JSONUtil;
import com.yupi.yupicturebackend.manager.auth.model.SpaceUserPermissionConstant;
import yupicture.application.service.UserApplicationService;
import yupicture.infrastructure.exception.BusinessException;
import yupicture.infrastructure.exception.ErrorCode;
import com.yupi.yupicturebackend.model.entity.Picture;
import com.yupi.yupicturebackend.model.entity.Space;
import com.yupi.yupicturebackend.model.entity.SpaceUser;
import yupicture.domain.user.entity.User;
import com.yupi.yupicturebackend.model.enums.SpaceRoleEnum;
import com.yupi.yupicturebackend.model.enums.SpaceTypeEnum;
import com.yupi.yupicturebackend.service.PictureService;
import com.yupi.yupicturebackend.service.SpaceService;
import com.yupi.yupicturebackend.service.SpaceUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

import static yupicture.domain.user.constant.UserConstant.USER_LOGIN_STATE;


/**
 * 自定义权限加载接口实现类
 */
@Component    // 保证此类被 SpringBoot 扫描，完成 Sa-Token 的自定义权限验证扩展
public class StpInterfaceImpl implements StpInterface {

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;
    @Autowired
    private SpaceUserService spaceUserService;
    @Autowired
    private PictureService pictureService;
    @Autowired
    private SpaceService spaceService;


    /**
     * 返回一个账号所拥有的权限码集合
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        // 1. 校验登录类型 仅允许 space 类型能获得权限
        if (!loginType.equals(StpKit.SPACE_TYPE)) {
            return new ArrayList<>();
        }
        // 2. 获取管理员权限列表
        List<String> ADMIN_PERMISSIONS = spaceUserAuthManager.getPermissionsByRole(SpaceRoleEnum.ADMIN.getValue());
        // 3. 获得上下文对象
        SpaceUserAuthContext authContext = this.getAuthContextByRequest();
        // 校验是否为空
        if (isAllFieldsNull(authContext)) {
            // 公共图库请求——返回管理员权限
            return ADMIN_PERMISSIONS;
        }
        // 4. 获取用户信息、判断用户是否登录、获取用户ID
        User user = (User) StpKit.SPACE.getSessionByLoginId(loginId).get(USER_LOGIN_STATE);
        if (user == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "请先登录");
        }
        // 获得 userId
        Long userId = user.getId();
        // 5. 获取 SpaceUser
        SpaceUser spaceUser = authContext.getSpaceUser();
        if (spaceUser != null) {
            return spaceUserAuthManager.getPermissionsByRole(spaceUser.getSpaceRole());
        }
        // 6. 获取 SpaceUserId
        Long spaceUserId = authContext.getSpaceUserId();
        if (spaceUserId != null) {
            // 获取 SpaceUser
            spaceUser = spaceUserService.getById(spaceUserId);
            if (spaceUser == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            }
            // 查询当前团队空间是否存在该用户
            SpaceUser loginSpaceUser = spaceUserService.lambdaQuery()
                    .eq(SpaceUser::getSpaceId, spaceUser.getSpaceId())
                    .eq(SpaceUser::getUserId, userId)
                    .one();
            // 团队空间不存在该用户—无权限
            if (loginSpaceUser == null) {
                return new ArrayList<>();
            }
            // 根据角色返回对应权限
            return spaceUserAuthManager.getPermissionsByRole(loginSpaceUser.getSpaceRole());
        }
        // 7. 获取 spaceId
        Long spaceId = authContext.getSpaceId();
        if (spaceId == null) {
            // 获取 pictureId
            Long pictureId = authContext.getPictureId();
            if (pictureId == null) {
                // 所有ID都为空，返回管理员权限
                return ADMIN_PERMISSIONS;
            }
            // 根据 pictureId 查询 picture
            Picture picture = pictureService.lambdaQuery()
                    .eq(Picture::getId, pictureId)
                    .select(Picture::getId, Picture::getSpaceId, Picture::getUserId)
                    .one();
            // 找不到图片就抛异常
            if (picture == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图片不存在");
            }
            // 找到图片就获得 spaceId
            spaceId = picture.getSpaceId();
            // 有图片没空间ID说明是公共图库,判断是上传者或是管理员
            if (spaceId == null) {
                if (userId.equals(picture.getUserId()) || user.isAdmin()) {
                    // 返回管理员权限
                    return ADMIN_PERMISSIONS;
                }
                // 返回查看图片的权限
                return Collections.singletonList(SpaceUserPermissionConstant.PICTURE_VIEW);
            }
        }
        // 8. 排除所有没有 spaceId 的情况，走到这里说明获得了 spaceId
        Space space = spaceService.getById(spaceId);
        if (space == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        }
        // 获取空间类型
        if (space.getSpaceType().equals(SpaceTypeEnum.PRIVATE.getValue())){
            // 私有空间
            if (userId.equals(space.getUserId()) || user.isAdmin()) {
                // 获取管理员权限
                return ADMIN_PERMISSIONS;
            } else {
                // 无权限
                return new ArrayList<>();
            }
        } else {
            // 团队空间
            spaceUser = spaceUserService.lambdaQuery()
                    .eq(SpaceUser::getSpaceId, spaceId)
                    .eq(SpaceUser::getUserId, userId)
                    .one();
            // 用户不是该团队成员——无权限
            if (spaceUser == null) {
                return new ArrayList<>();
            }
            // 根据角色返回对应权限
            return spaceUserAuthManager.getPermissionsByRole(spaceUser.getSpaceRole());
        }

    }

    /**
     * 返回一个账号所拥有的角色标识集合 (权限与角色可分开校验)
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        return new ArrayList<>();
    }

    @Value("${server.servlet.context-path}")
    private String contextPath;

    /**
     * 从请求中获取上下文对象
     */
    private SpaceUserAuthContext getAuthContextByRequest() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String contentType = request.getHeader(Header.CONTENT_TYPE.getValue());
        SpaceUserAuthContext authRequest;
        // 兼容 get 和 post 操作
        if (ContentType.JSON.getValue().equals(contentType)) {
            String body = ServletUtil.getBody(request);
            authRequest = JSONUtil.toBean(body, SpaceUserAuthContext.class);
        } else {
            Map<String, String> paramMap = ServletUtil.getParamMap(request);
            authRequest = BeanUtil.toBean(paramMap, SpaceUserAuthContext.class);
        }
        // 根据请求路径区分 id 字段的含义
        Long id = authRequest.getId();
        if (ObjUtil.isNotNull(id)) {
            // api/picture/aaa?a=1
            String requestUri = request.getRequestURI();
            // picture/aaa?a=1
            String partUri = requestUri.replace(contextPath + "/", "");
            // picture
            String moduleName = StrUtil.subBefore(partUri, "/", false);
            switch (moduleName) {
                case "picture":
                    authRequest.setPictureId(id);
                    break;
                case "spaceUser":
                    authRequest.setSpaceUserId(id);
                    break;
                case "space":
                    authRequest.setSpaceId(id);
                    break;
                default:
            }
        }
        return authRequest;
    }


    /**
     * 判断上下文对象是否为空
     * @param object
     * @return
     */
    private boolean isAllFieldsNull(Object object) {
        if (object == null) {
            return true; // 对象本身为空
        }
        // 获取所有字段并判断是否所有字段都为空
        return Arrays.stream(ReflectUtil.getFields(object.getClass()))
                // 获取字段值
                .map(field -> ReflectUtil.getFieldValue(object, field))
                // 检查是否所有字段都为空
                .allMatch(ObjectUtil::isEmpty);
    }

}


