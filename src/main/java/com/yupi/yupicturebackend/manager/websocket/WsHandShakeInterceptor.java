package com.yupi.yupicturebackend.manager.websocket;

import cn.hutool.core.util.ObjUtil;
import com.yupi.yupicturebackend.manager.auth.SpaceUserAuthManager;
import com.yupi.yupicturebackend.manager.auth.model.SpaceUserPermissionConstant;
import com.yupi.yupicturebackend.model.entity.Picture;
import com.yupi.yupicturebackend.model.entity.Space;
import yupicture.application.service.UserApplicationService;
import yupicture.domain.user.entity.User;
import com.yupi.yupicturebackend.model.enums.SpaceTypeEnum;
import com.yupi.yupicturebackend.service.PictureService;
import com.yupi.yupicturebackend.service.SpaceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class WsHandShakeInterceptor implements HandshakeInterceptor {

    @Resource
    private UserApplicationService userApplicationService;

    @Resource
    private PictureService pictureService;
    @Resource
    private SpaceService spaceService;
    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        if (request instanceof ServletServerHttpRequest) {
            // 获得 httpServletRequest
            HttpServletRequest httpServletRequest = ((ServletServerHttpRequest) request).getServletRequest();
            // 从请求信息中获取 pictureId
            String pictureIdStr = httpServletRequest.getParameter("pictureId");
            Long pictureId = Long.valueOf(pictureIdStr);
            // 校验编辑的图片id
            if (ObjUtil.isEmpty(pictureId)) {
                log.error("缺少图片请求参数，拒绝建立连接");
                return false;
            }
            // 校验用户登录
            User loginUser = userApplicationService.getLoginUser(httpServletRequest);
            if (ObjUtil.isEmpty(loginUser)) {
                log.error("用户未登录，拒绝建立连接");
                return false;
            }
            // 校验图片
            Picture picture = pictureService.getById(pictureId);
            if (ObjUtil.isEmpty(picture)) {
                log.error("图片不存在，拒绝建立连接");
                return false;
            }
            // 校验空间
            Long spaceId = picture.getSpaceId();
            Space space = null;
            if (ObjUtil.isNotEmpty(spaceId)) {
                // 空间不存在
                space = spaceService.getById(spaceId);
                if (ObjUtil.isEmpty(space)) {
                    log.error("空间不存在，拒绝建立连接");
                    return false;
                }
                // 私有空间
                if (!space.getSpaceType().equals(SpaceTypeEnum.TEAM.getValue())) {
                    log.error("不是团队空间，拒绝建立连接");
                    return false;
                }
            }
            // 团队空间和公共图库
            // 获得用户权限
            List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);
            if (!permissionList.contains(SpaceUserPermissionConstant.PICTURE_EDIT)) {
                log.error("没有图片编辑权限，拒绝建立连接");
                return false;
            }
            // 保存信息到 attributes 中
            attributes.put("pictureId", pictureId);
            attributes.put("userId", loginUser.getId());
            attributes.put("user", loginUser);
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {

    }
}
