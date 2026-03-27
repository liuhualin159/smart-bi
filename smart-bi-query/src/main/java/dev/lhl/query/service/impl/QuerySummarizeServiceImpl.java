package dev.lhl.query.service.impl;

import dev.lhl.query.service.IQuerySummarizeService;
import dev.lhl.query.service.LlmService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 查询结果总结：LLM 输出 1~3 句总结
 *
 * @author smart-bi
 */
@Service
public class QuerySummarizeServiceImpl implements IQuerySummarizeService {

    private static final Logger log = LoggerFactory.getLogger(QuerySummarizeServiceImpl.class);

    private static final String TEMPLATE = """
        根据以下数据表格，用 1~3 句话总结关键发现或结论。语言简洁，直接给出结论，不要冗余说明。
        列名：{columns}
        数据样本（前若干行）：{sampleRows}
        请直接输出总结文本，不要其他格式。
        """;

    @Autowired(required = false)
    private LlmService llmService;

    @Value("${smart.bi.query.summarize.enabled:true}")
    private boolean enabled;

    @Override
    public String summarize(Long queryId, String chartType, List<String> columns, List<Map<String, Object>> data) {
        if (!enabled || columns == null || columns.isEmpty() || data == null || data.isEmpty()) {
            return null;
        }
        if (llmService == null || !llmService.isAvailable()) {
            log.debug("LLM 不可用，跳过总结");
            return null;
        }
        try {
            String colsStr = String.join(", ", columns);
            String sampleStr = data.stream().limit(10)
                .map(row -> row.entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining(", ")))
                .collect(Collectors.joining("\n"));
            Prompt prompt = new PromptTemplate(TEMPLATE).create(Map.of(
                "columns", colsStr,
                "sampleRows", sampleStr.isEmpty() ? "（无数据）" : sampleStr
            ));
            String result = llmService.callPrompt(prompt);
            return result != null ? result.trim() : null;
        } catch (Exception e) {
            log.warn("生成总结失败: {}", e.getMessage());
            return null;
        }
    }
}
