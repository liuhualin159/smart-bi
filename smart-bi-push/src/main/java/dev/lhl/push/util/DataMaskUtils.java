package dev.lhl.push.util;

import java.util.regex.Pattern;

/**
 * 推送内容脱敏工具
 * 识别手机号、身份证、邮箱等敏感字段并脱敏
 *
 * @author smart-bi
 */
public final class DataMaskUtils {

    private static final Pattern PHONE = Pattern.compile("(\\d{3})\\d{4}(\\d{4})");
    private static final Pattern ID_CARD = Pattern.compile("(\\d{6})\\d{8}(\\d{4})");
    private static final Pattern EMAIL = Pattern.compile("([a-zA-Z0-9_.+-]{1,3})[a-zA-Z0-9_.+-]*@([a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+)");

    private DataMaskUtils() {}

    /**
     * 对单个值脱敏（根据值内容自动识别类型）
     */
    public static String mask(String value) {
        if (value == null || value.isEmpty()) return value;
        String v = value.trim();
        if (v.matches("1[3-9]\\d{9}")) return maskPhone(v);
        if (v.matches("\\d{17}[0-9Xx]")) return maskIdCard(v);
        if (v.contains("@") && v.indexOf('@') > 0 && v.indexOf('@') < v.length() - 1) return maskEmail(v);
        return value;
    }

    /**
     * 手机号脱敏：138****5678
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 11) return phone;
        return phone.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2");
    }

    /**
     * 身份证脱敏：110101********1234
     */
    public static String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 18) return idCard;
        return idCard.replaceAll("(\\d{6})\\d{8}(\\d{4})", "$1********$2");
    }

    /**
     * 邮箱脱敏：abc***@example.com
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        int at = email.indexOf('@');
        String local = email.substring(0, at);
        String domain = email.substring(at);
        if (local.length() <= 3) return local.charAt(0) + "***" + domain;
        return local.substring(0, 3) + "***" + domain;
    }

    /**
     * 对表格行数据按列脱敏（columns 中列名含 phone/email/idcard/mobile 等关键字则脱敏）
     */
    public static Object[] maskTableRow(Object[] row, String[] columns) {
        if (row == null || columns == null || row.length != columns.length) return row;
        Object[] out = new Object[row.length];
        for (int i = 0; i < row.length; i++) {
            Object val = row[i];
            String col = columns[i] != null ? columns[i].toLowerCase() : "";
            if (val instanceof String && (col.contains("phone") || col.contains("mobile") || col.contains("tel"))) {
                out[i] = maskPhone((String) val);
            } else if (val instanceof String && (col.contains("idcard") || col.contains("id_card") || col.contains("identity"))) {
                out[i] = maskIdCard((String) val);
            } else if (val instanceof String && col.contains("email")) {
                out[i] = maskEmail((String) val);
            } else if (val instanceof String) {
                out[i] = mask((String) val);
            } else {
                out[i] = val;
            }
        }
        return out;
    }
}
