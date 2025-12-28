package com.anji.captcha.service.impl;

import com.anji.captcha.model.common.RepCodeEnum;
import com.anji.captcha.model.common.ResponseModel;
import com.anji.captcha.model.vo.CaptchaVO;
import com.anji.captcha.model.vo.PointVO;
import com.anji.captcha.service.CaptchaCacheService;
import com.anji.captcha.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Base64;
import java.util.Properties;
import java.util.Random;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * 数字验证码服务实现
 * 展示如何通过SPI机制扩展自定义验证码类型
 */
public class NumberCaptchaServiceImpl extends AbstractCaptchaService {

    private static final Logger logger = LoggerFactory.getLogger(NumberCaptchaServiceImpl.class);
    
    // 验证码宽度
    private static final int WIDTH = 120;
    // 验证码高度
    private static final int HEIGHT = 40;
    // 验证码字符数
    private static final int CODE_COUNT = 4;
    // 干扰线数量
    private static final int LINE_COUNT = 5;
    
    // 数字字符集
    private static final String NUMBER_CHAR = "0123456789";
    
    private Random random = new Random();
    
    @Override
    public void init(Properties config) {
        super.init(config);
        logger.info("NumberCaptchaServiceImpl initialized");
    }
    
    @Override
    public ResponseModel get(CaptchaVO captchaVO) {
        ResponseModel limitResult = super.get(captchaVO);
        if (limitResult != null) {
            return limitResult;
        }
        
        try {
            // 生成随机数字验证码
            String code = generateRandomCode(CODE_COUNT);
            
            // 生成验证码图片
            BufferedImage image = generateCodeImage(code);
            
            // 转换为base64
            String base64Image = imageToBase64(image);
            
            // 生成token
            String token = java.util.UUID.randomUUID().toString().replace("-", "");
            
            // 保存到缓存
            CaptchaCacheService cacheService = getCacheService(cacheType);
            String key = String.format(REDIS_CAPTCHA_KEY, token);
            cacheService.set(key, code, 60); // 60秒过期
            
            // 构建响应
            CaptchaVO resultVO = new CaptchaVO();
            resultVO.setToken(token);
            resultVO.setOriginalImageBase64(base64Image);
            resultVO.setPoint(new PointVO()); // 数字验证码不需要点坐标
            
            ResponseModel responseModel = new ResponseModel();
            responseModel.setRepCode(RepCodeEnum.SUCCESS.getCode());
            responseModel.setRepData(resultVO);
            
            return responseModel;
        } catch (Exception e) {
            logger.error("Generate number captcha failed: {}", e.getMessage(), e);
            ResponseModel responseModel = new ResponseModel();
            responseModel.setRepCode(RepCodeEnum.ERROR.getCode());
            return responseModel;
        }
    }
    
    @Override
    public ResponseModel check(CaptchaVO captchaVO) {
        ResponseModel limitResult = super.check(captchaVO);
        if (limitResult != null) {
            return limitResult;
        }
        
        try {
            String token = captchaVO.getToken();
            String pointJson = captchaVO.getPointJson();
            
            if (StringUtils.isEmpty(token) || StringUtils.isEmpty(pointJson)) {
                return RepCodeEnum.NULL_ERROR.parseError("参数");
            }
            
            // 从缓存获取正确答案
            CaptchaCacheService cacheService = getCacheService(cacheType);
            String key = String.format(REDIS_CAPTCHA_KEY, token);
            String correctCode = cacheService.get(key);
            
            if (correctCode == null || correctCode.isEmpty()) {
                return RepCodeEnum.API_CAPTCHA_INVALID.parseError();
            }
            
            // 验证答案
            if (correctCode.equals(pointJson)) {
                // 验证成功后删除缓存
                cacheService.delete(key);
                
                ResponseModel responseModel = new ResponseModel();
                responseModel.setRepCode(RepCodeEnum.SUCCESS.getCode());
                return responseModel;
            } else {
                // 验证失败，记录失败次数
                afterValidateFail(captchaVO);
                return RepCodeEnum.API_CAPTCHA_COORDINATE_ERROR.parseError();
            }
            
        } catch (Exception e) {
            logger.error("Check number captcha failed: {}", e.getMessage(), e);
            ResponseModel responseModel = new ResponseModel();
            responseModel.setRepCode(RepCodeEnum.ERROR.getCode());
            return responseModel;
        }
    }
    
    @Override
    public ResponseModel verification(CaptchaVO captchaVO) {
        ResponseModel limitResult = super.verification(captchaVO);
        if (limitResult != null) {
            return limitResult;
        }
        
        // 二次验证，复用check逻辑
        return check(captchaVO);
    }
    
    @Override
    public String captchaType() {
        return "number";
    }
    
    /**
     * 生成随机数字验证码
     */
    private String generateRandomCode(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int index = random.nextInt(NUMBER_CHAR.length());
            sb.append(NUMBER_CHAR.charAt(index));
        }
        return sb.toString();
    }
    
    /**
     * 生成验证码图片
     */
    private BufferedImage generateCodeImage(String code) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        
        // 设置背景色
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, WIDTH, HEIGHT);
        
        // 设置字体
        Font font = new Font("Arial", Font.BOLD, 28);
        g.setFont(font);
        
        // 绘制干扰线
        for (int i = 0; i < LINE_COUNT; i++) {
            int x1 = random.nextInt(WIDTH);
            int y1 = random.nextInt(HEIGHT);
            int x2 = random.nextInt(WIDTH);
            int y2 = random.nextInt(HEIGHT);
            
            g.setColor(getRandomColor(100, 200));
            g.setStroke(new BasicStroke(2));
            g.drawLine(x1, y1, x2, y2);
        }
        
        // 绘制数字
        for (int i = 0; i < code.length(); i++) {
            char c = code.charAt(i);
            g.setColor(getRandomColor(50, 150));
            
            // 随机旋转角度
            double rotate = random.nextDouble() * 0.4 - 0.2;
            g.rotate(rotate, 20 + i * 25, 25);
            
            g.drawString(String.valueOf(c), 20 + i * 25, 30);
            
            g.rotate(-rotate, 20 + i * 25, 25);
        }
        
        // 绘制噪点
        for (int i = 0; i < 50; i++) {
            int x = random.nextInt(WIDTH);
            int y = random.nextInt(HEIGHT);
            g.setColor(getRandomColor(150, 250));
            g.fillRect(x, y, 2, 2);
        }
        
        g.dispose();
        return image;
    }
    
    /**
     * 获取随机颜色
     */
    private Color getRandomColor(int min, int max) {
        if (min > 255) min = 255;
        if (max > 255) max = 255;
        if (min < 0) min = 0;
        if (max < 0) max = 0;
        
        int r = min + random.nextInt(max - min);
        int g = min + random.nextInt(max - min);
        int b = min + random.nextInt(max - min);
        
        return new Color(r, g, b);
    }
    
    /**
     * 图片转base64
     */
    private String imageToBase64(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        byte[] bytes = baos.toByteArray();
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
    }
    
    @Override
    public void destroy(Properties config) {
        // 清理资源
        logger.info("NumberCaptchaServiceImpl destroyed");
    }
}
