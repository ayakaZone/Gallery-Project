package yupicture.domain.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import yupicture.domain.user.entity.User;
import yupicture.interfaces.dto.user.UserLoginRequest;
import yupicture.interfaces.dto.user.UserQueryRequest;
import yupicture.interfaces.dto.user.UserRegisterRequest;
import yupicture.interfaces.vo.user.LoginUserVO;
import yupicture.interfaces.vo.user.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;

/**
* @author Ayaki
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2025-11-21 22:29:53
*/
public interface UserDomainService{

    /**
     * 用户注册
     * @param userRegisterRequest
     * @return
     */
    long userRegister(UserRegisterRequest userRegisterRequest);

    /**
     * 用户登录
     * @param userLoginRequest
     * @param request
     * @return
     */
    LoginUserVO userLogin(UserLoginRequest userLoginRequest, HttpServletRequest request);

    /**
     * 封装 LoginUserVO
     *
     * @param user
     * @return
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 封装 UserVO
     * @param user
     * @return
     */
    UserVO getUserVO(User user);

    /**
     * 封装 List<UserVO>
     *
     * @param userList
     * @return
     */
    List<UserVO> getUserVOList(List<User> userList);

    /**
     * 获取当前登录用户
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 用户注销
     * @param request
     * @return
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 用户分页查询条件
     *
     * @param userQueryRequest
     * @return
     */
    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);

    /**
     * 获取加密密码
     * @param userPassword
     * @return
     */
    String getEncryptPassword(String userPassword);

    /**
     * 获取用户
     * @param id
     * @return
     */
    User getById(long id);

    /**
     * 删除用户
     * @param userId
     * @return
     */
    boolean removeById(Long userId);

    /**
     * 修改用户
     * @param user
     */
    Long saveUser(User user);

    /**
     * 修改用户
     * @param user
     * @return
     */
    boolean updateById(User user);

    /**
     * 批量查询
     * @param userIdSet
     * @return
     */
    List<User> listByIds(Set<Long> userIdSet);

    /**
     * 分页查询
     * @param userPage
     * @param queryWrapper
     * @return
     */
    Page<User> page(Page<User> userPage, QueryWrapper<User> queryWrapper);

    /**
     * 获取用户分页列表
     * @param userQueryRequest
     * @return
     */
    Page<UserVO> listUserVOByPage(UserQueryRequest userQueryRequest);

}
