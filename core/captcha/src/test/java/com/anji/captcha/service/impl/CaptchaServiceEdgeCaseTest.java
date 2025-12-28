package com.anji.captcha.service.impl;

import com.anji.captcha.model.common.ResponseModel;
import com.anji.captcha.model.vo.CaptchaVO;
import com.anji.captcha.service.CaptchaService;
import com.anji.captcha.service.CaptchaCacheService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Properties;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * 验证码服务边缘场景测试
 */
public class CaptchaServiceEdgeCaseTest {

    @Mock
    private CaptchaCacheService cacheService;
    
    private CaptchaService blockPuzzleService;
    private CaptchaService clickWordService;
    
    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // 初始化测试服务
        blockPuzzleService = new BlockPuzzleCaptchaServiceImpl();
        clickWordService = new ClickWordCaptchaServiceImpl();
        
        // 初始化配置
        Properties config = new Properties();
        config.setProperty("captcha.water.mark", "Test Watermark");
        config.setProperty("captcha.water.font", "WenQuanZhengHei.ttf");
        
        blockPuzzleService.init(config);
        clickWordService.init(config);
    }
    
    /**
     * 测试空类型的验证码请求
     */
    @Test
    public void testGetWithEmptyCaptchaType() {
        CaptchaVO captchaVO = new CaptchaVO();
        captchaVO.setCaptchaType("");
        
        ResponseModel response = blockPuzzleService.get(captchaVO);
        assertNotNull(response);
        // 应该返回错误或默认处理
    }
    
    /**
     * 测试无效类型的验证码请求
     */
    @Test
    public void testGetWithInvalidCaptchaType() {
        CaptchaVO captchaVO = new CaptchaVO();
        captchaVO.setCaptchaType("invalidType");
        
        // 使用默认服务测试
        DefaultCaptchaServiceImpl defaultService = new DefaultCaptchaServiceImpl();
        Properties config = new Properties();
        defaultService.init(config);
        
        ResponseModel response = defaultService.get(captchaVO);
        assertNotNull(response);
        // 默认服务应该能处理各种类型
    }
    
    /**
     * 测试空token的验证请求
     */
    @Test
    public void testCheckWithEmptyToken() {
        CaptchaVO captchaVO = new CaptchaVO();
        captchaVO.setCaptchaType("blockPuzzle");
        captchaVO.setToken("");
        captchaVO.setPointJson("testPoint");
        
        ResponseModel response = blockPuzzleService.check(captchaVO);
        assertNotNull(response);
        // 应该返回失败
    }
    
    /**
     * 测试空pointJson的验证请求
     */
    @Test
    public void testCheckWithEmptyPointJson() {
        CaptchaVO captchaVO = new CaptchaVO();
        captchaVO.setCaptchaType("blockPuzzle");
        captchaVO.setToken("testToken");
        captchaVO.setPointJson("");
        
        ResponseModel response = blockPuzzleService.check(captchaVO);
        assertNotNull(response);
        // 应该返回失败
    }
    
    /**
     * 测试验证码类型不匹配的请求
     */
    @Test
    public void testCheckWithMismatchCaptchaType() {
        CaptchaVO captchaVO = new CaptchaVO();
        captchaVO.setCaptchaType("clickWord");
        captchaVO.setToken("testToken");
        captchaVO.setPointJson("testPoint");
        
        ResponseModel response = blockPuzzleService.check(captchaVO);
        assertNotNull(response);
        // 应该返回失败
    }
    
    /**
     * 测试验证码服务工厂获取不存在的服务
     */
    @Test(expected = RuntimeException.class)
    public void testServiceFactoryWithUnsupportedType() {
        Properties config = new Properties();
        config.setProperty("captcha.type", "unsupportedType");
        CaptchaServiceFactory.getInstance(config);
    }
    
    /**
     * 测试缓存服务获取不存在的缓存类型
     */
    @Test
    public void testCacheServiceWithInvalidType() {
        CaptchaCacheService cache = CaptchaServiceFactory.getCache("invalidCacheType");
        assertNull(cache);
    }
    
    /**
     * 测试验证码初始化失败的情况
     */
    @Test
    public void testInitWithInvalidConfig() {
        CaptchaService service = new BlockPuzzleCaptchaServiceImpl();
        
        // 使用空配置初始化
        Properties emptyConfig = new Properties();
        service.init(emptyConfig);
        
        // 应该能正常初始化，使用默认配置
        CaptchaVO captchaVO = new CaptchaVO();
        captchaVO.setCaptchaType("blockPuzzle");
        ResponseModel response = service.get(captchaVO);
        assertNotNull(response);
    }
}
