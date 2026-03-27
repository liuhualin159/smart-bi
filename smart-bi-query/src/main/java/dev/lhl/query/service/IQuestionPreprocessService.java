package dev.lhl.query.service;

/**
 * 问题预处理服务：纠错与意图归一化
 * 失败时降级返回原文
 *
 * @author smart-bi
 */
public interface IQuestionPreprocessService {

    /**
     * 对用户输入进行纠错与意图归一化
     *
     * @param rawQuestion 原始问题
     * @return 纠正后的标准问题；若预处理失败或一致则返回原文
     */
    String preprocess(String rawQuestion);
}
