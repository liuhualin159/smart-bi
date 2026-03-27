package dev.lhl.query.service;

import dev.lhl.query.service.impl.Nl2SqlServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * NL2SQL 自修正逻辑单元测试
 */
@ExtendWith(MockitoExtension.class)
class Nl2SqlSelfCorrectionTest
{
    @InjectMocks
    private Nl2SqlServiceImpl nl2SqlService;

    @Mock
    private LlmService llmService;

    @Mock
    private PromptTemplateService promptTemplateService;

    @Mock
    private SqlSyntaxPrecheckService syntaxPrecheckService;

    @BeforeEach
    void setUp()
    {
        ReflectionTestUtils.setField(nl2SqlService, "selfCorrectionEnabled", true);
        ReflectionTestUtils.setField(nl2SqlService, "selfCorrectionMaxRetries", 2);
    }

    @Test
    void maxRetries为0时_correctSQL返回null()
    {
        ReflectionTestUtils.setField(nl2SqlService, "selfCorrectionMaxRetries", 0);
        String result = nl2SqlService.correctSQL("查询销售额", "SELECT bad_sql", "column not found", 1L);
        assertNull(result);
    }

    @Test
    void selfCorrection禁用时_correctSQL返回null()
    {
        ReflectionTestUtils.setField(nl2SqlService, "selfCorrectionEnabled", false);
        String result = nl2SqlService.correctSQL("查询销售额", "SELECT bad_sql", "column not found", 1L);
        assertNull(result);
    }

    @Test
    void 修正成功_返回修正后的SQL()
    {
        when(promptTemplateService.createSqlCorrectionPrompt(any(), any(), any(), any()))
            .thenReturn(mock(Prompt.class));
        when(llmService.callPrompt(any()))
            .thenReturn("SELECT SUM(amount) FROM orders");
        when(syntaxPrecheckService.check(any()))
            .thenReturn(new SqlSyntaxPrecheckService.PrecheckResult(true, null));

        String result = nl2SqlService.correctSQL("查询销售额", "SELECT bad_col FROM orders", "Unknown column 'bad_col'", 1L);
        assertNotNull(result);
        assertTrue(result.contains("SELECT"));

        verify(llmService, atMost(1)).callPrompt(any());
    }

    @Test
    void LLM返回空_修正失败返回null()
    {
        when(promptTemplateService.createSqlCorrectionPrompt(any(), any(), any(), any()))
            .thenReturn(mock(Prompt.class));
        when(llmService.callPrompt(any())).thenReturn(null);

        String result = nl2SqlService.correctSQL("查询", "SELECT x", "error", 1L);
        assertNull(result);

        verify(llmService, times(2)).callPrompt(any());
    }

    @Test
    void 最大重试次数生效_不超过maxRetries()
    {
        ReflectionTestUtils.setField(nl2SqlService, "selfCorrectionMaxRetries", 2);
        when(promptTemplateService.createSqlCorrectionPrompt(any(), any(), any(), any()))
            .thenReturn(mock(Prompt.class));
        when(llmService.callPrompt(any())).thenReturn("");

        nl2SqlService.correctSQL("q", "sql", "err", 1L);

        verify(llmService, times(2)).callPrompt(any());
    }
}
