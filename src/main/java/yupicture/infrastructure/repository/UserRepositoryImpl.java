package yupicture.infrastructure.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import yupicture.domain.user.entity.User;
import yupicture.domain.user.repository.UserRepository;
import yupicture.infrastructure.mapper.UserMapper;

@Service
public class UserRepositoryImpl extends ServiceImpl<UserMapper, User> implements UserRepository {
}
