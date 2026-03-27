package dev.lhl.common.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 敏感字段脱敏工具类
 * 
 * @author smart-bi
 */
public class DesensitizeUtils
{
    private static final Logger log = LoggerFactory.getLogger(DesensitizeUtils.class);

    /**
     * 脱敏手机号
     * 示例：138****5678
     */
    public static String desensitizePhone(String phone)
    {
        if (StringUtils.isEmpty(phone) || phone.length() < 7)
        {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /**
     * 脱敏身份证号
     * 示例：110***********1234
     */
    public static String desensitizeIdCard(String idCard)
    {
        if (StringUtils.isEmpty(idCard) || idCard.length() < 8)
        {
            return idCard;
        }
        return idCard.substring(0, 3) + "***********" + idCard.substring(idCard.length() - 4);
    }

    /**
     * 脱敏邮箱
     * 示例：abc***@example.com
     */
    public static String desensitizeEmail(String email)
    {
        if (StringUtils.isEmpty(email) || !email.contains("@"))
        {
            return email;
        }
        int atIndex = email.indexOf("@");
        String prefix = email.substring(0, atIndex);
        String suffix = email.substring(atIndex);
        if (prefix.length() <= 3)
        {
            return prefix.charAt(0) + "***" + suffix;
        }
        return prefix.substring(0, 3) + "***" + suffix;
    }

    /**
     * 脱敏银行卡号
     * 示例：6222********1234
     */
    public static String desensitizeBankCard(String bankCard)
    {
        if (StringUtils.isEmpty(bankCard) || bankCard.length() < 8)
        {
            return bankCard;
        }
        return bankCard.substring(0, 4) + "********" + bankCard.substring(bankCard.length() - 4);
    }

    /**
     * 通用脱敏（保留前后各N位）
     */
    public static String desensitize(String value, int prefixLength, int suffixLength)
    {
        if (StringUtils.isEmpty(value))
        {
            return value;
        }
        int totalLength = value.length();
        if (totalLength <= prefixLength + suffixLength)
        {
            return "***";
        }
        String prefix = value.substring(0, prefixLength);
        String suffix = value.substring(totalLength - suffixLength);
        int maskLength = totalLength - prefixLength - suffixLength;
        return prefix + "*".repeat(Math.min(maskLength, 10)) + suffix;
    }
}
