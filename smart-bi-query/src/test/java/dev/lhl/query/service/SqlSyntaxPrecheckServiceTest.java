package dev.lhl.query.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SqlSyntaxPrecheckService 单元测试
 */
class SqlSyntaxPrecheckServiceTest
{
    private SqlSyntaxPrecheckService service;

    @BeforeEach
    void setUp()
    {
        service = new SqlSyntaxPrecheckService();
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "fallbackOnParseError", false);
    }

    @Test
    void 合法SELECT语句_通过预检()
    {
        SqlSyntaxPrecheckService.PrecheckResult result = service.check("SELECT id, name FROM users WHERE age > 18");
        assertTrue(result.passed());
        assertNull(result.errorMessage());
    }

    @Test
    void 带JOIN的合法SQL_通过预检()
    {
        String sql = "SELECT o.id, c.name FROM orders o JOIN customers c ON o.customer_id = c.id WHERE o.status = 1";
        SqlSyntaxPrecheckService.PrecheckResult result = service.check(sql);
        assertTrue(result.passed());
    }

    @Test
    void 带GROUP_BY和HAVING的合法SQL_通过预检()
    {
        String sql = "SELECT department, COUNT(*) AS cnt FROM employees GROUP BY department HAVING COUNT(*) > 5";
        SqlSyntaxPrecheckService.PrecheckResult result = service.check(sql);
        assertTrue(result.passed());
    }

    @Test
    void 明显语法错误_fallback关闭时返回错误()
    {
        ReflectionTestUtils.setField(service, "fallbackOnParseError", false);
        SqlSyntaxPrecheckService.PrecheckResult result = service.check("SELEC id FORM users");
        assertFalse(result.passed());
        assertNotNull(result.errorMessage());
        assertTrue(result.errorMessage().contains("SQL 语法预检失败"));
    }

    @Test
    void 明显语法错误_fallback开启时放行()
    {
        ReflectionTestUtils.setField(service, "fallbackOnParseError", true);
        SqlSyntaxPrecheckService.PrecheckResult result = service.check("SELEC id FORM users");
        assertTrue(result.passed());
    }

    @Test
    void 禁用预检时直接通过()
    {
        ReflectionTestUtils.setField(service, "enabled", false);
        SqlSyntaxPrecheckService.PrecheckResult result = service.check("this is not sql at all");
        assertTrue(result.passed());
    }

    @Test
    void 空SQL返回失败()
    {
        SqlSyntaxPrecheckService.PrecheckResult result = service.check("");
        assertFalse(result.passed());
    }

    @Test
    void nullSQL返回失败()
    {
        SqlSyntaxPrecheckService.PrecheckResult result = service.check(null);
        assertFalse(result.passed());
    }

    @Test
    void 带尾部分号的SQL_通过预检()
    {
        SqlSyntaxPrecheckService.PrecheckResult result = service.check("SELECT 1;");
        assertTrue(result.passed());
    }
}
