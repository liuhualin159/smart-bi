package dev.lhl.common.utils;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AES-256加密工具类
 * 用于数据源密码等敏感信息的加密存储
 * 
 * @author smart-bi
 */
public class EncryptUtils
{
    private static final Logger log = LoggerFactory.getLogger(EncryptUtils.class);
    
    /**
     * AES算法
     */
    private static final String ALGORITHM = "AES";
    
    /**
     * AES加密模式：AES/ECB/PKCS5Padding
     */
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";
    
    /**
     * 密钥长度：256位
     */
    private static final int KEY_LENGTH = 256;
    
    /**
     * 默认密钥（实际使用时应从配置文件或密钥管理系统获取）
     * 注意：生产环境必须使用安全的密钥管理方式，不能硬编码
     */
    private static final String DEFAULT_KEY = "SmartBI2024SecretKeyForAES256Encryption!";
    
    /**
     * 从配置获取密钥，如果未配置则使用默认密钥
     * 优先级：系统属性 > 环境变量 > 配置文件 > 默认密钥
     */
    private static String getSecretKey() {
        // 1. 尝试从系统属性获取
        String key = System.getProperty("smart.bi.encrypt.key");
        if (key != null && !key.trim().isEmpty()) {
            return key;
        }
        
        // 2. 尝试从环境变量获取
        key = System.getenv("SMART_BI_ENCRYPT_KEY");
        if (key != null && !key.trim().isEmpty()) {
            return key;
        }
        
        // 3. 尝试从Spring配置获取（如果Spring上下文可用）
        try {
            // 使用反射检查SpringUtils是否可用
            Class<?> springUtilsClass = Class.forName("dev.lhl.common.utils.spring.SpringUtils");
            Object configBean = springUtilsClass.getMethod("getBean", Class.class)
                .invoke(null, Class.forName("dev.lhl.common.config.RuoYiConfig"));
            if (configBean != null) {
                java.lang.reflect.Method getEncryptKeyMethod = configBean.getClass().getMethod("getEncryptKey");
                Object encryptKey = getEncryptKeyMethod.invoke(configBean);
                if (encryptKey != null && !encryptKey.toString().trim().isEmpty()) {
                    return encryptKey.toString();
                }
            }
        } catch (Exception e) {
            // Spring上下文不可用或配置不存在，使用默认密钥
            log.debug("无法从Spring配置获取加密密钥，使用默认密钥: {}", e.getMessage());
        }
        
        // 4. 使用默认密钥
        log.warn("使用默认加密密钥，生产环境请通过系统属性、环境变量或配置文件设置smart.bi.encrypt.key");
        return DEFAULT_KEY;
    }
    
    /**
     * 将任意长度的密钥转换为32字节（256位）的AES密钥
     * 使用SHA-256哈希算法确保密钥长度符合AES-256要求
     * 
     * @param key 原始密钥
     * @return 32字节的密钥字节数组
     */
    private static byte[] getKeyBytes(String key) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = sha.digest(key.getBytes(StandardCharsets.UTF_8));
            return keyBytes;
        }
        catch (Exception e) {
            log.error("生成密钥字节数组失败", e);
            throw new RuntimeException("生成密钥字节数组失败", e);
        }
    }
    
    /**
     * 生成AES密钥
     * 
     * @return Base64编码的密钥
     */
    public static String generateKey()
    {
        try
        {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
            keyGenerator.init(KEY_LENGTH, new SecureRandom());
            SecretKey secretKey = keyGenerator.generateKey();
            return Base64.getEncoder().encodeToString(secretKey.getEncoded());
        }
        catch (Exception e)
        {
            log.error("生成AES密钥失败", e);
            throw new RuntimeException("生成AES密钥失败", e);
        }
    }
    
    /**
     * AES加密
     * 
     * @param plainText 明文
     * @return Base64编码的密文
     */
    public static String encrypt(String plainText)
    {
        if (StringUtils.isEmpty(plainText))
        {
            return plainText;
        }
        
        try
        {
            String key = getSecretKey();
            byte[] keyBytes = getKeyBytes(key);
            SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, ALGORITHM);
            
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
            
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        }
        catch (Exception e)
        {
            log.error("AES加密失败", e);
            throw new RuntimeException("AES加密失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * AES解密
     * 
     * @param cipherText Base64编码的密文
     * @return 明文
     */
    public static String decrypt(String cipherText)
    {
        if (StringUtils.isEmpty(cipherText))
        {
            return cipherText;
        }
        
        try
        {
            String key = getSecretKey();
            byte[] keyBytes = getKeyBytes(key);
            SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, ALGORITHM);
            
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
            
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(cipherText));
            return new String(decrypted, StandardCharsets.UTF_8);
        }
        catch (Exception e)
        {
            log.error("AES解密失败", e);
            throw new RuntimeException("AES解密失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 验证加密/解密功能是否正常
     * 
     * @return true表示功能正常
     */
    public static boolean validate()
    {
        try
        {
            String testText = "test123456";
            String encrypted = encrypt(testText);
            String decrypted = decrypt(encrypted);
            return testText.equals(decrypted);
        }
        catch (Exception e)
        {
            log.error("加密工具验证失败", e);
            return false;
        }
    }
}
