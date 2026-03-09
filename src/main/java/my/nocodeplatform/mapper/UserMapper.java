package my.nocodeplatform.mapper;

import com.mybatisflex.core.BaseMapper;
import my.nocodeplatform.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * 用户 映射层。
 *
 */
@Repository
public interface UserMapper extends BaseMapper<User> {

}
