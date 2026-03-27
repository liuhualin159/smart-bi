package dev.lhl.query.service;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PromptTemplateService Few-shot 注入单元测试
 */
class PromptTemplateServiceFewshotTest
{
    private final PromptTemplateService service = new PromptTemplateService();

    @Test
    void 有示例时_包含参考示例区块()
    {
        List<Map<String, String>> examples = List.of(
            Map.of("question", "查询总销售额", "sql", "SELECT SUM(amount) FROM orders"),
            Map.of("question", "查询客户数量", "sql", "SELECT COUNT(*) FROM customers")
        );
        String result = service.formatFewshotExamples(examples);
        assertNotNull(result);
        assertTrue(result.contains("## 参考示例"));
        assertTrue(result.contains("示例 1"));
        assertTrue(result.contains("示例 2"));
        assertTrue(result.contains("查询总销售额"));
        assertTrue(result.contains("SELECT SUM(amount) FROM orders"));
    }

    @Test
    void 无示例时_返回空字符串()
    {
        String result = service.formatFewshotExamples(List.of());
        assertEquals("", result);
    }

    @Test
    void null示例列表_返回空字符串()
    {
        String result = service.formatFewshotExamples(null);
        assertEquals("", result);
    }

    @Test
    void 单个示例_格式正确()
    {
        List<Map<String, String>> examples = List.of(
            Map.of("question", "月度订单数", "sql", "SELECT DATE_FORMAT(order_date,'%Y-%m') AS month, COUNT(*) FROM orders GROUP BY month")
        );
        String result = service.formatFewshotExamples(examples);
        assertTrue(result.contains("示例 1"));
        assertFalse(result.contains("示例 2"));
        assertTrue(result.contains("月度订单数"));
    }
}
