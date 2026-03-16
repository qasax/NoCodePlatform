package my.nocodeplatform.ai.utils;

import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.extern.slf4j.Slf4j;
import my.nocodeplatform.exception.BusinessException;
import my.nocodeplatform.exception.ErrorCode;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.time.Duration;

@Slf4j
public class WebScreenshotUtils {

    private static final int DEFAULT_WIDTH = 1600;
    private static final int DEFAULT_HEIGHT = 900;

    /**
     * 创建浏览器
     */
    private static WebDriver createDriver() {

        try {

            WebDriverManager.chromedriver().setup();

            ChromeOptions options = new ChromeOptions();

            options.addArguments(
                    "--headless=new",
                    "--disable-gpu",
                    "--no-sandbox",
                    "--disable-dev-shm-usage",
                    "--disable-extensions",
                    "--window-size=" + DEFAULT_WIDTH + "," + DEFAULT_HEIGHT
            );

            WebDriver driver = new ChromeDriver(options);

            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));

            return driver;

        } catch (Exception e) {
            log.error("初始化 ChromeDriver 失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "初始化 Chrome 浏览器失败");
        }
    }

    /**
     * 等待页面加载
     */
    private static void waitForPageLoad(WebDriver driver) {

        try {

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

            wait.until(webDriver ->
                    ((JavascriptExecutor) webDriver)
                            .executeScript("return document.readyState")
                            .equals("complete")
            );

            Thread.sleep(2000);

        } catch (Exception e) {

            log.warn("页面加载等待失败，继续截图", e);

        }
    }

    /**
     * 保存图片
     */
    private static void saveImage(byte[] imageBytes, String path) {

        try {

            FileUtil.writeBytes(imageBytes, path);

        } catch (Exception e) {

            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存截图失败");

        }
    }

    /**
     * 压缩图片
     */
    private static void compressImage(String original, String compressed) {

        final float QUALITY = 0.3f;

        try {

            ImgUtil.compress(
                    FileUtil.file(original),
                    FileUtil.file(compressed),
                    QUALITY
            );

        } catch (Exception e) {

            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "压缩图片失败");

        }
    }

    /**
     * 生成截图
     */
    public static String saveWebPageScreenshot(String webUrl) {

        if (StrUtil.isBlank(webUrl)) {
            return null;
        }

        WebDriver driver = null;

        try {

            driver = createDriver();

            String relativePath = File.separator + UUID.randomUUID().toString().substring(0, 8);

            String rootPath = System.getProperty("user.dir")
                    + File.separator + "tmp"
                    + File.separator + "screenshots"
                    + relativePath;

            FileUtil.mkdir(rootPath);

            String imagePath = rootPath
                    + File.separator
                    + RandomUtil.randomNumbers(5)
                    + ".png";

            driver.get(webUrl);

            waitForPageLoad(driver);

            byte[] screenshotBytes =
                    ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);

            saveImage(screenshotBytes, imagePath);

            String compressedRelative =
                    File.separator + RandomUtil.randomNumbers(5) + "_compressed.jpg";

            String compressedPath = rootPath + compressedRelative;

            compressImage(imagePath, compressedPath);

            FileUtil.del(imagePath);

            return relativePath + compressedRelative;

        } catch (Exception e) {

            log.error("截图失败 {}", webUrl, e);

            return null;

        } finally {

            if (driver != null) {

                try {
                    driver.quit();
                } catch (Exception ignored) {}

            }

        }
    }
}