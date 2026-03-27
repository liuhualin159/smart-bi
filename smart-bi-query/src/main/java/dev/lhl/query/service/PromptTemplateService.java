package dev.lhl.query.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * NL2SQL提示词模板服务
 * 使用Spring AI的PromptTemplate构建NL2SQL提示词
 * 
 * @author smart-bi
 */
@Service
public class PromptTemplateService
{
    private static final Logger log = LoggerFactory.getLogger(PromptTemplateService.class);
    
    /**
     * NL2SQL提示词模板：禁止臆造表名/字段名；仅可使用「全部可用表」中的表，字段必须来自「相关表结构」或指标定义。
     * 系统通过工具提供全部表名与注释，LLM 必须依赖这些信息而非联想（如禁止 test_user 等未出现的表）。
     */
    private static final String NL2SQL_TEMPLATE = """
        你是一个严格的企业级 SQL 生成助手，负责把用户的自然语言问题转成安全、可执行的 SQL。

        ## 目标数据库版本（必须遵守）
        当前数据库版本：{databaseVersion}
        请严格按该版本兼容的语法生成 SQL。**表别名禁止使用保留字**（如 TO、ORDER、GROUP、KEY、SELECT、WHERE 等），请使用 t1、t2、ord、cust、to1、oi 等简短非保留字作为表别名，避免语法错误。

        ## 核心约束（必须遵守）
        1. **仅使用下方「全部可用表」中出现的表名**：SQL 中 FROM/JOIN 的表名必须来自该列表，**严禁臆造任何未出现的表**（例如禁止使用 test_user、user、users、projects 等未在列表中出现的表名）。
        2. **仅使用「相关表结构」或「指标定义」中的字段**：SELECT、WHERE、GROUP BY、ORDER BY 中的列必须来自这些结构，禁止使用未定义的字段。
        3. **业务语义映射**：当用户提到「用户名称」「客户名称」等时，必须在现有表与字段中找对应（如 test_customer 的 customer_name），不得编造新表名。
        4. **枚举/状态字段**：当字段标注了「枚举值(显示→存储)」时，用户说的中文/显示值（如「已支付」）必须转换为对应的存储值（编码）写入 WHERE 条件，不得直接使用显示文本。例如用户问「状态为已支付的订单」且枚举为 已支付→1，应写 WHERE status = 1 或 status = '1'，而非 status = '已支付'。
        4.1 **「新增订单」「新增用户」的语义**：在同比/环比语境下，「新增」表示「按时间周期统计的数量」（如每月新增订单数 = 该月下单的订单数），应仅按时间字段（order_date/create_time 等）GROUP BY 统计，**不得**随意添加 status='CREATED' 等状态过滤；仅当用户明确指定状态（如「已支付订单环比」「待审核用户新增」）时才加 status 条件。
        5. **仅生成 SELECT 语句**：禁止 DDL/DML（INSERT/UPDATE/DELETE/CREATE 等）。
        6. **不确定时**：若无法从下方信息中找到与用户问题对应的表或字段，应基于已有表结构给出最贴近的 SQL，或明确说明缺少哪类表/字段，而不要凭空编造表名。

        ## 系统已提供的工具结果（请严格依据以下内容）
        - **全部可用表（表名与注释）**：以下为当前库中所有可用的业务表，SQL 中只能使用这些表名。
        {allTablesList}

        - **相关表结构（字段与注释）**：以下为与问题相关的表结构详情，字段必须从中选择。
        {tableStructures}

        - **指标定义**：涉及统计/指标时优先使用其中的 expression。
        {metricDefinitions}

        - **推荐 JOIN（优先使用）**：以下为配置的推荐关联，生成 SQL 时请优先从该列表选择 JOIN 条件。
        {recommendedJoins}

        - **敏感字段规则**：exposure_policy=FORBIDDEN 的字段仅可用于 WHERE/JOIN，不可出现在 SELECT 列表；exposure_policy=AGG_ONLY 的字段仅可用于聚合结果（如 SUM(x)、COUNT(x)），不可单独出现在 SELECT 或 WHERE。
        {sensitiveFieldRules}
        {feedbackCorrectionsHint}
        {fewshotExamples}

        ## 指代消解规则
        若用户问题中出现「它」「这个」「那个」「上述」等指代词，请结合「对话上下文」中的上一轮问答内容，将指代替换为具体实体（表名、字段名、维度值）后再生成 SQL。
        例如：上一轮答「产品A销量最高」，本轮问「它的毛利率呢」→ 应理解为「产品A的毛利率」，并在 SQL 中加入 product_name='产品A' 或对应主键条件。

        ## 同比/环比/占比规则
        - 同比：与去年同期对比，需使用 DATE_SUB/INTERVAL 等构造同期时间范围。
        - 环比：与上一周期对比（如上月、上周）。**禁止使用 LAG/LEAD/OVER 等窗口函数**（MySQL 5.7 不支持）。必须用自连接：内层子查询按周期 GROUP BY 聚合得到本期数据，再用 LEFT JOIN 连接「上期」子查询（通过月份字符串比较或 DATE_SUB(STR_TO_DATE(month,'%Y-%m-%d'),INTERVAL 1 MONTH) 关联上月），最后计算 (本期-上期)/上期*100。
        - 环比示例（MySQL 5.7 兼容，自连接写法）：
          SELECT a.month, a.cnt AS new_users, CASE WHEN b.cnt IS NULL OR b.cnt=0 THEN NULL ELSE ROUND((a.cnt-b.cnt)/b.cnt*100,2) END AS ring_growth FROM (SELECT DATE_FORMAT(create_time,'%Y-%m') AS month, COUNT(id) AS cnt FROM test_customer WHERE create_time>=DATE_SUB(CURDATE(),INTERVAL 12 MONTH) GROUP BY DATE_FORMAT(create_time,'%Y-%m')) a LEFT JOIN (SELECT DATE_FORMAT(create_time,'%Y-%m') AS month, COUNT(id) AS cnt FROM test_customer WHERE create_time>=DATE_SUB(CURDATE(),INTERVAL 12 MONTH) GROUP BY DATE_FORMAT(create_time,'%Y-%m')) b ON b.month=DATE_FORMAT(DATE_SUB(STR_TO_DATE(CONCAT(a.month,'-01'),'%Y-%m-%d'),INTERVAL 1 MONTH),'%Y-%m') ORDER BY a.month
        - 环比需多周期数据：WHERE 时间范围必须包含至少两个周期（如最近12个月），不能只筛选「本月」。
        - 占比：某部分/总值 * 100，需先计算总值再 JOIN 或子查询。
        当用户问题包含「同比」「环比」「占比」「和上期比较」「和去年对比」时，必须在 SQL 中体现上述逻辑。
        {temporalRatioHint}

        ## 用户与上下文
        用户问题：{question}
        用户身份：{userIdentity}
        对话上下文（如有）：{conversationContext}

        ## 输出要求
        请输出一个 JSON 对象，格式为：\\{"sql": "SELECT...", "confidence": 0.9, "disambiguationQuestions": []\\}
        - sql：生成的 SQL 查询语句；若 confidence < 0.7 可将 sql 留空或给最佳猜测。
        - confidence：0~1 的置信度，表示对问题理解的把握程度。
        - disambiguationQuestions：当 confidence < 0.7 时，必须给出 1~2 个澄清问题（如「您说的利润是指毛利润还是净利润？」）；否则为空数组。
        仅输出该 JSON，不要其他文字或 markdown 代码块。
        """;
    
    /**
     * 创建NL2SQL提示词模板
     * 
     * @return PromptTemplate对象
     */
    public PromptTemplate createNl2SqlPromptTemplate()
    {
        try
        {
            PromptTemplate promptTemplate = new PromptTemplate(NL2SQL_TEMPLATE);
            log.debug("创建NL2SQL提示词模板成功");
            return promptTemplate;
        }
        catch (Exception e)
        {
            log.error("创建NL2SQL提示词模板失败", e);
            throw new RuntimeException("创建NL2SQL提示词模板失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 创建NL2SQL提示词（带变量，含全部可用表列表以禁止表名幻觉）
     *
     * @param question 用户问题
     * @param userIdentity 用户身份
     * @param allTablesList 全部可用表（表名与注释），用于约束 LLM 仅使用这些表名
     * @param tableStructures 相关表结构信息（含字段）
     * @param metricDefinitions 指标定义（含 expression）
     * @param conversationContext 对话历史上下文
     * @return Prompt对象
     */
    /**
     * 创建NL2SQL提示词（带变量，含全部可用表、推荐 JOIN、敏感字段规则）
     */
    public Prompt createNl2SqlPrompt(
        String question,
        String userIdentity,
        String allTablesList,
        String tableStructures,
        String metricDefinitions,
        String conversationContext,
        String recommendedJoins,
        String sensitiveFieldRules,
        boolean hasTemporalOrRatioIntent,
        String feedbackCorrectionsHint,
        String databaseVersion)
    {
        return createNl2SqlPrompt(question, userIdentity, allTablesList, tableStructures, metricDefinitions,
            conversationContext, recommendedJoins, sensitiveFieldRules, hasTemporalOrRatioIntent, feedbackCorrectionsHint, databaseVersion, null);
    }

    public Prompt createNl2SqlPrompt(
        String question,
        String userIdentity,
        String allTablesList,
        String tableStructures,
        String metricDefinitions,
        String conversationContext,
        String recommendedJoins,
        String sensitiveFieldRules,
        boolean hasTemporalOrRatioIntent,
        String feedbackCorrectionsHint,
        String databaseVersion,
        String fewshotExamplesBlock)
    {
        try
        {
            PromptTemplate promptTemplate = new PromptTemplate(NL2SQL_TEMPLATE);
            Map<String, Object> variables = new HashMap<>();
            variables.put("question", question != null ? question : "");
            variables.put("userIdentity", userIdentity != null ? userIdentity : "");
            variables.put("databaseVersion", databaseVersion != null && !databaseVersion.isEmpty()
                ? databaseVersion : "未知（请使用通用 SQL 语法，表别名勿用保留字如 TO/ORDER/GROUP）");
            variables.put("allTablesList", allTablesList != null && !allTablesList.isEmpty()
                ? allTablesList : "（暂无，请仅使用下方「相关表结构」中出现的表名，禁止臆造任何未列出的表）");
            variables.put("tableStructures", tableStructures != null ? tableStructures : "（暂无表结构，若元数据未配置请先配置）");
            variables.put("metricDefinitions", metricDefinitions != null ? metricDefinitions : "（暂无指标定义）");
            variables.put("conversationContext", conversationContext != null ? conversationContext : "无");
            variables.put("recommendedJoins", recommendedJoins != null && !recommendedJoins.isEmpty() ? recommendedJoins : "（暂无推荐 JOIN，可在已下表与字段范围内推断）");
            variables.put("sensitiveFieldRules", sensitiveFieldRules != null && !sensitiveFieldRules.isEmpty() ? sensitiveFieldRules : "（无额外敏感规则）");
            variables.put("temporalRatioHint", hasTemporalOrRatioIntent
                ? "【重要】当前问题涉及时间对比或占比，请生成包含时间对比/占比计算的完整 SQL，不要省略。"
                : "");
            variables.put("feedbackCorrectionsHint", feedbackCorrectionsHint != null && !feedbackCorrectionsHint.isEmpty()
                ? feedbackCorrectionsHint : "");
            variables.put("fewshotExamples", fewshotExamplesBlock != null && !fewshotExamplesBlock.isEmpty()
                ? fewshotExamplesBlock : "");
            Prompt prompt = promptTemplate.create(variables);
            log.debug("创建NL2SQL提示词: question={}", question);
            return prompt;
        }
        catch (Exception e)
        {
            log.error("创建NL2SQL提示词失败: question={}", question, e);
            throw new RuntimeException("创建NL2SQL提示词失败: " + e.getMessage(), e);
        }
    }

    /** 兼容旧调用（无推荐 JOIN、敏感规则、数据库版本） */
    public Prompt createNl2SqlPrompt(
        String question,
        String userIdentity,
        String allTablesList,
        String tableStructures,
        String metricDefinitions,
        String conversationContext)
    {
        return createNl2SqlPrompt(question, userIdentity, allTablesList, tableStructures, metricDefinitions, conversationContext, null, null, false, null, null, null);
    }
    
    /**
     * SQL 自修正提示词模板：将 DB 错误反馈给 LLM 修正 SQL
     */
    private static final String SQL_CORRECTION_TEMPLATE = """
        你是一个 SQL 修正助手。用户提出了一个自然语言问题，系统已生成 SQL 但执行时出错。
        请根据错误信息修正 SQL，仅返回修正后的 SQL，不要其他文字。

        ## 原始问题
        {question}

        ## 原始 SQL
        {originalSql}

        ## 数据库错误信息
        {errorMessage}

        ## 相关表结构
        {tableStructures}

        ## 修正要求
        1. 仅修正导致错误的部分，保持原始查询意图
        2. 仅生成 SELECT 语句
        3. 修正后的 SQL 必须与目标数据库兼容
        4. 请直接输出修正后的 SQL，不要包含任何说明文字或 markdown 代码块
        """;

    /**
     * 创建 SQL 修正提示词
     */
    public Prompt createSqlCorrectionPrompt(String question, String originalSql, String errorMessage, String tableStructures)
    {
        try
        {
            PromptTemplate tpl = new PromptTemplate(SQL_CORRECTION_TEMPLATE);
            Map<String, Object> vars = new HashMap<>();
            vars.put("question", question != null ? question : "");
            vars.put("originalSql", originalSql != null ? originalSql : "");
            vars.put("errorMessage", errorMessage != null ? errorMessage : "未知错误");
            vars.put("tableStructures", tableStructures != null ? tableStructures : "（暂无）");
            return tpl.create(vars);
        }
        catch (Exception e)
        {
            log.error("创建SQL修正提示词失败", e);
            throw new RuntimeException("创建SQL修正提示词失败: " + e.getMessage(), e);
        }
    }

    /**
     * 将 few-shot 参考示例注入到 NL2SQL prompt 模板变量中
     *
     * @param fewshotExamples 示例列表，每项含 question 和 sql
     * @return 格式化的参考示例文本块，若无示例则返回空字符串
     */
    public String formatFewshotExamples(List<Map<String, String>> fewshotExamples)
    {
        if (fewshotExamples == null || fewshotExamples.isEmpty())
        {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("\n        ## 参考示例（请参考上述示例的 SQL 风格和思路生成 SQL）\n\n");
        for (int i = 0; i < fewshotExamples.size(); i++)
        {
            Map<String, String> example = fewshotExamples.get(i);
            sb.append("        示例 ").append(i + 1).append("：\n");
            sb.append("        问题：").append(example.get("question")).append("\n");
            sb.append("        SQL：").append(example.get("sql")).append("\n\n");
        }
        return sb.toString();
    }

    /**
     * 图表推荐提示词模板：根据 SQL 与查询结果特征选择展示形式，禁止硬编码
     */
    private static final String CHART_RECOMMEND_TEMPLATE = """
        你是一个「智能可视化选择器」，根据 SQL 与查询结果自动选择最合适的数据展示形式（表格或图表）。禁止根据表名、字段名硬编码，必须根据 SQL 结构、列类型、数据特征推理。

        【输入】
        用户问题（可选）：{question}
        SQL（可选）：{sql}
        结果列名：{columns}
        结果行数：{rowCount}
        结果样本（前若干行）：{sampleRows}

        【可选展示类型】
        table（表格）、bar（柱状图）、line（折线图）、pie（饼图）、groupedBar（分组柱状图）、heatmap（热力图）、funnel（漏斗图）、boxplot（箱线图）、scatter（散点图）、sankey（桑基图）、kpi（指标卡）

        【规则】
        1. 无聚合、无 GROUP BY、列为属性/维度为主（如名称、城市、等级、编码）→ 优先 table。
        2. 有聚合（SUM/COUNT/AVG 等）或 GROUP BY、或有明显数值指标+少量维度 → 可选 bar/line/pie/groupedBar/kpi。
        3. 有日期/时间维度且适合看趋势 → 优先 line。
        4. **多主体趋势比较**：当用户问题涉及「比较各…趋势」「每个…的…趋势」「各…走势」等，且结果含「时间维度+主体维度（如用户/客户/产品名）+数值指标」时，必须选 line（折线图），并期望呈现为「每个主体一条线」的形态，X 轴为时间、Y 轴为指标、图例为各主体。
        5. 无法确定时选 table。只输出一个 JSON，不要其他文字。

        【输出格式】
        请输出一个 JSON 对象，包含：chartType（如 \"table\"）、confidence（0~1 的置信度）、alternatives（1~2 个备选类型的数组，如 [\"bar\",\"line\"]）、reason（简要说明）。不要添加额外文字。
        """;

    /**
     * 创建图表推荐提示词（供 LLM 选择展示类型）
     *
     * @param question 用户问题（可为空）
     * @param sql      SQL 语句（可为空）
     * @param columns  列名列表
     * @param rowCount 结果行数
     * @param sampleRows 样本行 JSON 字符串（前几行）
     * @return Prompt 对象
     */
    public Prompt createChartRecommendPrompt(
        String question,
        String sql,
        List<String> columns,
        int rowCount,
        String sampleRows)
    {
        try
        {
            PromptTemplate tpl = new PromptTemplate(CHART_RECOMMEND_TEMPLATE);
            Map<String, Object> vars = new HashMap<>();
            vars.put("question", question != null ? question : "（未提供）");
            vars.put("sql", sql != null ? sql : "（未提供）");
            vars.put("columns", columns != null ? String.join(", ", columns) : "（无）");
            vars.put("rowCount", rowCount);
            vars.put("sampleRows", sampleRows != null ? sampleRows : "[]");
            return tpl.create(vars);
        }
        catch (Exception e)
        {
            log.error("创建图表推荐提示词失败", e);
            throw new RuntimeException("创建图表推荐提示词失败: " + e.getMessage(), e);
        }
    }

    /**
     * 创建向量检索提示词
     * 
     * @param query 查询文本
     * @return Prompt对象
     */
    public Prompt createVectorSearchPrompt(String query)
    {
        try
        {
            String template = "根据以下查询文本，检索相关的表、字段、指标信息：\n{query}";
            PromptTemplate promptTemplate = new PromptTemplate(template);
            Map<String, Object> variables = Map.of("query", query != null ? query : "");
            Prompt prompt = promptTemplate.create(variables);
            log.debug("创建向量检索提示词: query={}", query);
            return prompt;
        }
        catch (Exception e)
        {
            log.error("创建向量检索提示词失败: query={}", query, e);
            throw new RuntimeException("创建向量检索提示词失败: " + e.getMessage(), e);
        }
    }
}
