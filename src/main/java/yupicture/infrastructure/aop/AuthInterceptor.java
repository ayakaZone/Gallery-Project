package yupicture.infrastructure.aop;

import yupicture.application.service.UserApplicationService;
import yupicture.infrastructure.annotation.AuthCheck;
import yupicture.infrastructure.exception.BusinessException;
import yupicture.domain.user.entity.User;
import yupicture.domain.user.valueobject.UserRoleEnum;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import static yupicture.infrastructure.exception.ErrorCode.NO_AUTH_ERROR;
import static yupicture.domain.user.valueobject.UserRoleEnum.ADMIN;

@Aspect
@Component
public class AuthInterceptor {

    @Resource
    private UserApplicationService userApplicationService;

    /**
     * 拦截请求，用户权限认证
     * @param joinPoint
     * @param authCheck
     * @return
     * @throws Throwable
     */
    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        // 必要角色(访问权限)
        String mustRole = authCheck.mustRole();
        // 转换成枚举
        UserRoleEnum mustRoleEnum = UserRoleEnum.getEnumByValue(mustRole);
        // 通过上下文获取请求
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        // 通过获取用户校验是否登录
        User loginUser = userApplicationService.getLoginUser(request);
        // 获取用户权限
        String userRole = loginUser.getUserRole();
        // 转换成枚举
        UserRoleEnum userRoleEnum = UserRoleEnum.getEnumByValue(userRole);
        // 判断接口访问权限
        if (mustRoleEnum == null) {
            return joinPoint.proceed();
        }
        // 判断用户访问权限
        if (userRoleEnum == null) {
            throw new BusinessException(NO_AUTH_ERROR);
        }
        // 需要管理员权限
        if (mustRoleEnum.equals(ADMIN) && !userRoleEnum.equals(ADMIN)) {
            throw new BusinessException(NO_AUTH_ERROR);
        }
        // 用权限认证通过，放行
        return joinPoint.proceed();
    }

}
