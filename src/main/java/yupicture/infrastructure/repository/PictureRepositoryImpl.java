package yupicture.infrastructure.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import yupicture.domain.picture.entity.Picture;
import yupicture.domain.picture.repository.PictureRepository;
import yupicture.infrastructure.mapper.PictureMapper;

@Service
public class PictureRepositoryImpl extends ServiceImpl<PictureMapper, Picture> implements PictureRepository {
}
