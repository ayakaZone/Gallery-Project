package yupicture.domain.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.yupicturebackend.manager.auth.StpKit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import yupicture.domain.service.UserDomainService;
import yupicture.domain.user.entity.User;
import yupicture.domain.user.repository.UserRepository;
import yupicture.domain.user.valueobject.UserRoleEnum;
import yupicture.infrastructure.exception.BusinessException;
import yupicture.infrastructure.exception.ThrowUtils;
import yupicture.interfaces.dto.user.UserLoginRequest;
import yupicture.interfaces.dto.user.UserQueryRequest;
import yupicture.interfaces.dto.user.UserRegisterRequest;
import yupicture.interfaces.vo.user.LoginUserVO;
import yupicture.interfaces.vo.user.UserVO;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static yupicture.domain.user.constant.UserConstant.USER_LOGIN_STATE;
import static yupicture.infrastructure.exception.ErrorCode.*;

/**
 * @author Ayaki
 * @description 针对表【user(用户)】的数据库操作Service实现
 * @createDate 2025-11-21 22:29:53
 */
@Slf4j
@Service
public class UserDomainServiceImpl implements UserDomainService {

    @Resource
    private UserRepository userRepository;

    /**
     * 用户注册
     *
     * @param userRegisterRequest
     * @return
     */
    @Override
    public long userRegister(UserRegisterRequest userRegisterRequest) {
        // 用户账号是否重复
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userRegisterRequest.getUserAccount());
        Long count = userRepository.getBaseMapper().selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException(PARAMS_ERROR, "账号已重复");
        }
        // 密码加密
        String encryptPassword = getEncryptPassword(userRegisterRequest.getUserPassword());
        // 封装用户
        User user = new User();
        user.setUserAccount(userRegisterRequest.getUserAccount()); // 账号
        user.setUserPassword(encryptPassword); // 加密后的密码
        user.setUserName("默认用户_" + UUID.randomUUID()); // 默认昵称
        user.setUserRole(UserRoleEnum.USER.getValue()); // 角色

        // 注册用户
        boolean saveResult = userRepository.save(user);
        if (!saveResult) {
            throw new BusinessException(SYSTEM_ERROR, "注册失败，数据库异常");
        }
        return user.getId();
    }

    /**
     * 用户登录
     *
     * @param userLoginRequest
     * @param request
     * @return
     */
    @Override
    public LoginUserVO userLogin(UserLoginRequest userLoginRequest, HttpServletRequest request) {
        // 参数校验
        User.validUserLogin(userLoginRequest);
        // 密码加密
        String userPassword = userLoginRequest.getUserPassword();
        String encryptPassword = getEncryptPassword(userPassword);
        userLoginRequest.setUserPassword(encryptPassword);
        // 查询数据库
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.eq("userAccount", userLoginRequest.getUserAccount())
                .eq("userPassword", userLoginRequest.getUserPassword());
        User user = userRepository.getBaseMapper().selectOne(userQueryWrapper);
        if (user == null) {
            log.info("user login failed, userAccount cannot match userPassword");
            throw new BusinessException(PARAMS_ERROR, "用户不存在或密码错误");
        }
        // Session 的用户登陆状态
        request.getSession().setAttribute(USER_LOGIN_STATE, user);
        // 使用 Sa-Token 记录登录态
        StpKit.SPACE.login(user.getId());
        // 记录用户信息
        StpKit.SPACE.getSession().set(USER_LOGIN_STATE, user);
        // 用户脱敏
        return this.getLoginUserVO(user);
    }

    /**
     * 封装 LoginUserVO
     *
     * @param user
     * @return
     */
    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        return BeanUtil.copyProperties(user, LoginUserVO.class);
    }

    /**
     * 封装 UserVO
     *
     * @param user
     * @return
     */
    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        return BeanUtil.copyProperties(user, UserVO.class);
    }

    /**
     * 封装 List<UserVO>
     *
     * @param userList
     * @return
     */
    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream()
                .map(this::getUserVO)
                .collect(Collectors.toList());
    }

    /**
     * 获得当前登录用户
     *
     * @param request
     * @return
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 从 Session 中获取当前登录用户
        HttpSession session = request.getSession();
        Object userObj = session.getAttribute(USER_LOGIN_STATE);
        User user = (User) userObj;
        // 校验
        if (ObjUtil.isEmpty(user) || ObjUtil.isEmpty(user.getId())) {
            throw new BusinessException(NOT_LOGIN_ERROR);
        }
        // 查询数据库（不一致性）
        Long userId = user.getId();
        user = this.getById(userId);
        if (user == null) {
            throw new BusinessException(NOT_LOGIN_ERROR);
        }
        return user;
    }

    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    @Override
    public boolean userLogout(HttpServletRequest request) {
        // 校验
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        if (ObjUtil.isEmpty(userObj)) {
            throw new BusinessException(OPERATION_ERROR, "未登录");
        }
        // 移除 Session 登陆状态
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return true;
    }

    /**
     * 用户分页查询条件
     *
     * @param userQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        // 校验
        if (ObjUtil.isEmpty(userQueryRequest)) {
            throw new BusinessException(PARAMS_ERROR, "查询参数为空");
        }
        // 查询条件对象
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        // 用户字段
        Long id = userQueryRequest.getId();
        String userAccount = userQueryRequest.getUserAccount();
        String userName = userQueryRequest.getUserName();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        // 查询条件
        return wrapper.eq(ObjUtil.isNotEmpty(id), "id", id)
                .eq(StrUtil.isNotEmpty(userRole), "userRole", userRole)
                .like(StrUtil.isNotEmpty(userAccount), "userAccount", userAccount)
                .like(StrUtil.isNotEmpty(userName), "userName", userName)
                .like(StrUtil.isNotEmpty(userProfile), "userProfile", userProfile)
                .orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
    }

    /**
     * 获取加密处理的密码
     */
    @Override
    public String getEncryptPassword(String userPassword) {
        // 加固密钥
        final String KEY = "yupi";
        // md5 加密算法
        return DigestUtils.md5DigestAsHex((KEY + userPassword).getBytes());
    }

    /**
     * 根据 id 查询
     * @param id
     * @return
     */
    @Override
    public User getById(long id) {
        return userRepository.getById(id);
    }

    /**
     * 删除用户
     * @param userId
     * @return
     */
    @Override
    public boolean removeById(Long userId) {
        return userRepository.removeById(userId);
    }

    /**
     * 保存用户
     * @param user
     * @return
     */
    @Override
    public Long saveUser(User user) {
        // 默认密码
        final String DEFAULT_PASSWORD = "12345678";
        // 密码加密
        user.setUserPassword(this.getEncryptPassword(DEFAULT_PASSWORD));
        // 保存用户
        boolean result = userRepository.save(user);
        ThrowUtils.throwIf(!result, OPERATION_ERROR);
        // 返回用户Id
        return user.getId();
    }

    /**
     * 更新用户
     * @param user
     * @return
     */
    @Override
    public boolean updateById(User user) {
        return userRepository.updateById(user);
    }

    /**
     * 批量查询用户
     * @param userIdSet
     * @return
     */
    @Override
    public List<User> listByIds(Set<Long> userIdSet) {
        return userRepository.listByIds(userIdSet);
    }

    /**
     * 分页查询用户
     * @param userPage
     * @param queryWrapper
     * @return
     */
    @Override
    public Page<User> page(Page<User> userPage, QueryWrapper<User> queryWrapper) {
        return userRepository.page(userPage, queryWrapper);
    }

    /**
     * 分页查询用户
     * @param userQueryRequest
     * @return
     */
    @Override
    public Page<UserVO> listUserVOByPage(UserQueryRequest userQueryRequest) {
        // 分页参数
        int current = userQueryRequest.getCurrent();
        int pageSize = userQueryRequest.getPageSize();
        // 分页查询
        Page<User> page = userRepository.page(new Page<>(current, pageSize), this.getQueryWrapper(userQueryRequest));
        List<UserVO> userVOList = this.getUserVOList(page.getRecords());
        // 封装VO
        Page<UserVO> userVOPage = new Page<>(current, pageSize, page.getTotal());
        userVOPage.setRecords(userVOList);
        return userVOPage;
    }
}




