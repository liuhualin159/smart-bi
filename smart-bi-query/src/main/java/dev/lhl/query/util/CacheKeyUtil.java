package dev.lhl.query.util;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

/**
 * 缓存键生成工具
 *
 * @author smart-bi
 */
public final class CacheKeyUtil {

    private CacheKeyUtil() {}

    /**
     * 根据 SQL 和 userId 生成缓存键（与 QueryExecutionServiceImpl 一致）
     */
    public static String generateCacheKey(String sql, Long userId) {
        try {
            String input = sql + "_" + (userId != null ? userId : 0);
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf((sql + "_" + (userId != null ? userId : 0)).hashCode());
        }
    }
}
