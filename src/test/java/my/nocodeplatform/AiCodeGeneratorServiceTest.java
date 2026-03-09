package my.nocodeplatform;

import jakarta.annotation.Resource;
import my.nocodeplatform.ai.AiCodeGeneratorService;
import my.nocodeplatform.ai.model.HtmlCodeResult;
import my.nocodeplatform.ai.model.MultiFileCodeResult;
import my.nocodeplatform.ai.model.core.AiCodeGeneratorFacade;
import my.nocodeplatform.ai.model.core.builder.VueProjectBuilder;
import my.nocodeplatform.ai.model.enums.CodeGenTypeEnum;
import my.nocodeplatform.ai.utils.WebScreenshotUtils;
import my.nocodeplatform.langgraph4j.CodeGenWorkflow;
import my.nocodeplatform.langgraph4j.state.WorkflowContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

import java.io.File;
import java.util.List;

@SpringBootTest
class AiCodeGeneratorServiceTest {

    @Resource
    private AiCodeGeneratorService aiCodeGeneratorService;
    @Resource
    private VueProjectBuilder vueProjectBuilder;

    @Test
    void generateHtmlCode() {
        HtmlCodeResult result = aiCodeGeneratorService.generateHtmlCode("个人博客网站");
        System.out.println(result);
        Assertions.assertNotNull(result);
    }

    @Test
    void generateMultiFileCode() {
        MultiFileCodeResult multiFileCode = aiCodeGeneratorService.generateMultiFileCode("游戏论坛网站");
        Assertions.assertNotNull(multiFileCode);
    }
    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;

    @Test
    void generateAndSaveCode() {
        File file = aiCodeGeneratorFacade.generateAndSaveCode("任务记录网站", CodeGenTypeEnum.MULTI_FILE, 1L);
        Assertions.assertNotNull(file);
    }

    @Test
    void generateAndSaveCodeStream() {
        Flux<String> codeStream = aiCodeGeneratorFacade.generateAndSaveCodeStream("任务记录网站", CodeGenTypeEnum.MULTI_FILE, 1L);
        // 阻塞等待所有数据收集完成
        List<String> result = codeStream.collectList().block();
        // 验证结果
        Assertions.assertNotNull(result);
        String completeContent = String.join("", result);
        Assertions.assertNotNull(completeContent);
    }

    @Test
    void generateVueProjectCodeStream() {
        Flux<String> codeStream = aiCodeGeneratorFacade.generateAndSaveCodeStream(
                "简单的任务记录网站，总代码量不超过 200 行",
                CodeGenTypeEnum.VUE_PROJECT, 1L);
        // 阻塞等待所有数据收集完成
        List<String> result = codeStream.collectList().block();
        // 验证结果
        Assertions.assertNotNull(result);
        String completeContent = String.join("", result);
        Assertions.assertNotNull(completeContent);
    }
    @Test
    void testbuild() {
        boolean buildSuccess = vueProjectBuilder.buildProject("C:\\Users\\zhangfajin\\Documents\\AI\\NoCodePlatform\\tmp\\code_output\\vue_project_388489922509668352");
    }
    @Test
    void saveWebPageScreenshot() {
        String testUrl = "https://www.codefather.cn";
        String webPageScreenshot = WebScreenshotUtils.saveWebPageScreenshot(testUrl);
        Assertions.assertNotNull(webPageScreenshot);
    }
    @Test
    void testTechBlogWorkflow() {
        WorkflowContext result = new CodeGenWorkflow().executeWorkflow("创建一个技术博客网站，需要展示编程教程和系统架构");
        Assertions.assertNotNull(result);
        System.out.println("生成类型: " + result.getGenerationType());
        System.out.println("生成的代码目录: " + result.getGeneratedCodeDir());
        System.out.println("构建结果目录: " + result.getBuildResultDir());
    }

    @Test
    void testCorporateWorkflow() {
        WorkflowContext result = new CodeGenWorkflow().executeWorkflow("创建企业官网，展示公司形象和业务介绍");
        Assertions.assertNotNull(result);
        System.out.println("生成类型: " + result.getGenerationType());
        System.out.println("生成的代码目录: " + result.getGeneratedCodeDir());
        System.out.println("构建结果目录: " + result.getBuildResultDir());
    }

    @Test
    void testVueProjectWorkflow() {
        WorkflowContext result = new CodeGenWorkflow().executeWorkflow("创建一个Vue前端项目，包含用户管理和数据展示功能");
        Assertions.assertNotNull(result);
        System.out.println("生成类型: " + result.getGenerationType());
        System.out.println("生成的代码目录: " + result.getGeneratedCodeDir());
        System.out.println("构建结果目录: " + result.getBuildResultDir());
    }

    @Test
    void testSimpleHtmlWorkflow() {
        WorkflowContext result = new CodeGenWorkflow().executeWorkflow("创建一个简单的个人主页");
        Assertions.assertNotNull(result);
        System.out.println("生成类型: " + result.getGenerationType());
        System.out.println("生成的代码目录: " + result.getGeneratedCodeDir());
        System.out.println("构建结果目录: " + result.getBuildResultDir());
    }

}