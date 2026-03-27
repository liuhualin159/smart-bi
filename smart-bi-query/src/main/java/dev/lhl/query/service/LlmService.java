package dev.lhl.query.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 大模型调用服务
 * 使用Spring AI的ChatClient调用云端大模型API
 *
 * @author smart-bi
 */
@Service
public class LlmService
{
    private static final Logger log = LoggerFactory.getLogger(LlmService.class);

    @Autowired(required = false)
    private ChatClient chatClient;

    @Autowired(required = false)
    private ChatModel chatModel;

    /**
     * 调用大模型生成SQL
     *
     * @param prompt 提示词对象
     * @return 生成的SQL
     */
    public String generateSQL(Prompt prompt)
    {
        if (chatModel == null && chatClient == null)
        {
            log.error("ChatModel和ChatClient均未配置，无法调用大模型");
            throw new RuntimeException("大模型服务未配置，请检查application.yml中的spring.ai.openai配置");
        }

        try
        {
            log.info("调用大模型生成SQL");

            String sql;
            if (chatModel != null)
            {
                // 使用ChatModel调用大模型
                ChatResponse response = chatModel.call(prompt);
                sql = response.getResult().getOutput().getText();
                log.debug("使用ChatModel生成SQL成功");
            }
            else if (chatClient != null)
            {
                // 使用ChatClient调用大模型
                // ChatClient需要从Prompt中提取消息内容
                String messageContent = prompt.getInstructions().get(0).getText();
                sql = chatClient.prompt(messageContent).call().content();
                log.debug("使用ChatClient生成SQL成功");
            }
            else
            {
                throw new RuntimeException("ChatModel和ChatClient均未配置");
            }

            // 清理SQL结果（去除可能的markdown代码块标记）
            sql = cleanSqlResult(sql);

            log.info("大模型生成SQL: sql={}", sql);
            return sql;
        }
        catch (Exception e)
        {
            log.error("调用大模型失败", e);
            throw new RuntimeException("调用大模型失败: " + e.getMessage(), e);
        }
    }

    /**
     * 正则：提取 ```sql ... ``` 或 ``` ... ``` 代码块内容
     */
    private static final Pattern SQL_CODE_BLOCK = Pattern.compile(
        "```(?:sql)?\\s*([\\s\\S]*?)```",
        Pattern.CASE_INSENSITIVE
    );

    /**
     * 正则：兜底提取以 SELECT 开头的 SQL 语句（到分号或换行结束）
     */
    private static final Pattern SELECT_STATEMENT = Pattern.compile(
        "(SELECT\\s+[\\s\\S]*?)(?:;\\s*$|;\\s*\\n|$)",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    /**
     * 清理SQL结果：从 LLM 回复中提取纯 SQL
     * 支持：纯 SQL、```sql ... ```、``` ... ```、前后带说明文字的回复
     *
     * @param sql 原始回复内容
     * @return 提取后的 SQL 语句
     */
    private String cleanSqlResult(String sql)
    {
        if (sql == null)
        {
            return "";
        }

        String trimmed = sql.trim();
        if (trimmed.isEmpty())
        {
            return "";
        }

        // 1. 优先从 markdown 代码块中提取
        Matcher blockMatcher = SQL_CODE_BLOCK.matcher(trimmed);
        if (blockMatcher.find())
        {
            String extracted = blockMatcher.group(1).trim();
            if (!extracted.isEmpty())
            {
                return extracted;
            }
        }

        // 2. 若整体以 ```sql 或 ``` 开头，去掉外层标记（兼容旧格式）
        if (trimmed.startsWith("```sql"))
        {
            trimmed = trimmed.substring(6);
        }
        else if (trimmed.startsWith("```"))
        {
            trimmed = trimmed.substring(3);
        }
        if (trimmed.endsWith("```"))
        {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        trimmed = trimmed.trim();

        // 3. 若仍包含 SELECT，尝试提取第一条 SELECT 语句
        Matcher selectMatcher = SELECT_STATEMENT.matcher(trimmed);
        if (selectMatcher.find())
        {
            String extracted = selectMatcher.group(1).trim();
            if (!extracted.isEmpty())
            {
                return extracted;
            }
        }

        // 4. 已是纯 SQL 或简单格式，直接返回
        return trimmed;
    }

    /**
     * 调用大模型进行语义分析
     *
     * @param text 文本内容
     * @return 分析结果
     */
    public String analyze(String text)
    {
        if (chatModel == null && chatClient == null)
        {
            log.error("ChatModel和ChatClient均未配置，无法调用大模型");
            throw new RuntimeException("大模型服务未配置，请检查application.yml中的spring.ai.openai配置");
        }

        try
        {
            log.info("调用大模型进行语义分析: text={}", text);

            String result;
            if (chatModel != null)
            {
                // 使用ChatModel进行语义分析
                UserMessage userMessage = new UserMessage(text);
                Prompt prompt = new Prompt(userMessage);
                ChatResponse response = chatModel.call(prompt);
                result = response.getResult().getOutput().getText();
                log.debug("使用ChatModel进行语义分析成功");
            }
            else
            {
                // 使用ChatClient进行语义分析
                result = chatClient.prompt(text).call().content();
                log.debug("使用ChatClient进行语义分析成功");
            }

            log.info("大模型分析结果: result={}", result);
            return result;
        }
        catch (Exception e)
        {
            log.error("调用大模型进行语义分析失败: text={}", text, e);
            throw new RuntimeException("调用大模型失败: " + e.getMessage(), e);
        }
    }

    /**
     * 调用大模型并返回原始文本（用于图表推荐等需解析 JSON 的场景）
     *
     * @param prompt 提示词
     * @return 模型输出的原始文本，未做清理
     */
    public String callPrompt(Prompt prompt)
    {
        if (chatModel == null && chatClient == null)
        {
            log.debug("大模型未配置，无法调用");
            return null;
        }
        try
        {
            String text;
            if (chatModel != null)
            {
                ChatResponse response = chatModel.call(prompt);
                text = response.getResult().getOutput().getText();
            }
            else
            {
                String messageContent = prompt.getInstructions().get(0).getText();
                text = chatClient.prompt(messageContent).call().content();
            }
            return text != null ? text.trim() : null;
        }
        catch (Exception e)
        {
            if (isInterrupted(e))
            {
                Thread.currentThread().interrupt();
                log.warn("调用大模型被中断: {}", e.getMessage());
                throw new RuntimeException("请求被中断（可能因超时或网络断开），请重试", e);
            }
            log.warn("调用大模型失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 判断异常是否由线程中断引起（如客户端断开、超时取消等）
     */
    private static boolean isInterrupted(Throwable t)
    {
        for (Throwable c = t; c != null; c = c.getCause())
        {
            if (c instanceof InterruptedException)
            {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查大模型服务是否可用
     *
     * @return true表示可用
     */
    public boolean isAvailable()
    {
        return chatModel != null || chatClient != null;
    }
}
