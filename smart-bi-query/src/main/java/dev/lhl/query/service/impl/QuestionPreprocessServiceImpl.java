package dev.lhl.query.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import dev.lhl.query.service.IQuestionPreprocessService;
import dev.lhl.query.service.LlmService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 问题预处理：调用 LLM 纠错与意图归一化
 * 失败时降级返回原文
 *
 * @author smart-bi
 */
@Service
public class QuestionPreprocessServiceImpl implements IQuestionPreprocessService {

    private static final Logger log = LoggerFactory.getLogger(QuestionPreprocessServiceImpl.class);

    private static final String PREPROCESS_TEMPLATE = """
        用户可能输入有错别字或语序混乱，请在不改变意图的前提下输出规范表述。
        若无法确定则原样返回。
        仅输出 JSON，不要其他文字：{"correctedQuestion": "纠正后的标准问题", "originalQuestion": "原始问题"}
        
        用户输入：{question}
        """;

    @Autowired(required = false)
    private LlmService llmService;

    @Value("${smart.bi.query.preprocess.enabled:true}")
    private boolean enabled;

    @Override
    public String preprocess(String rawQuestion) {
        if (!enabled || rawQuestion == null || rawQuestion.trim().isEmpty()) {
            return rawQuestion;
        }
        if (llmService == null || !llmService.isAvailable()) {
            log.debug("LLM 不可用，预处理降级返回原文");
            return rawQuestion;
        }
        try {
            PromptTemplate tpl = new PromptTemplate(PREPROCESS_TEMPLATE);
            Prompt prompt = tpl.create(Map.of("question", rawQuestion));
            String response = llmService.callPrompt(prompt);
            if (response == null || response.trim().isEmpty()) {
                return rawQuestion;
            }
            String trimmed = response.trim();
            if (trimmed.startsWith("```")) {
                int start = trimmed.indexOf("{");
                int end = trimmed.lastIndexOf("}");
                if (start >= 0 && end > start) {
                    trimmed = trimmed.substring(start, end + 1);
                }
            }
            JSONObject obj = JSON.parseObject(trimmed);
            String corrected = obj != null ? obj.getString("correctedQuestion") : null;
            if (corrected != null && !corrected.trim().isEmpty() && !corrected.trim().equals(rawQuestion.trim())) {
                log.debug("预处理纠正: {} -> {}", rawQuestion, corrected);
                return corrected.trim();
            }
            return rawQuestion;
        } catch (Exception e) {
            log.warn("预处理失败，降级返回原文: {}", e.getMessage());
            return rawQuestion;
        }
    }
}
