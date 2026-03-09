package my.nocodeplatform.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import jakarta.servlet.http.HttpServletRequest;
import my.nocodeplatform.entity.App;
import my.nocodeplatform.entity.User;
import my.nocodeplatform.model.dto.app.AppQueryRequest;
import my.nocodeplatform.model.vo.AppVO;
import reactor.core.publisher.Flux;

/**
 * 应用 服务层。
 *
 * @author zhangfajin
 */
public interface AppService extends IService<App> {

    /**
     * 获取查询条件
     *
     * @param appQueryRequest
     * @return
     */
    QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest);

    /**
     * 获取应用封装
     *
     * @param app
     * @param request
     * @return
     */
    AppVO getAppVO(App app, HttpServletRequest request);

    /**
     * 分页获取应用封装
     *
     * @param appPage
     * @param request
     * @return
     */
    Page<AppVO> getAppVOPage(Page<App> appPage, HttpServletRequest request);

    Flux<String> chatToGenCode(Long appId, String message, User loginUser);

    String deployApp(Long appId, User loginUser);

    String getPreviewUrl(Long appId, User loginUser);

    void saveCover(Long appId,String deployUrl);
}
