package dev.lhl.query.service.impl;

import dev.lhl.query.domain.Feedback;
import dev.lhl.query.domain.FeedbackCorrection;
import dev.lhl.query.domain.QueryRecord;
import dev.lhl.query.mapper.FeedbackCorrectionMapper;
import dev.lhl.query.mapper.FeedbackMapper;
import dev.lhl.query.service.IFeedbackCorrectionService;
import dev.lhl.query.service.IQueryRecordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 反馈修正服务实现
 *
 * @author smart-bi
 */
@Service
public class FeedbackCorrectionServiceImpl implements IFeedbackCorrectionService {

    private static final Logger log = LoggerFactory.getLogger(FeedbackCorrectionServiceImpl.class);

    @Autowired
    private FeedbackCorrectionMapper feedbackCorrectionMapper;

    @Autowired
    private FeedbackMapper feedbackMapper;

    @Autowired(required = false)
    private IQueryRecordService queryRecordService;

    @Value("${smart.bi.feedback.similarity.threshold:0.8}")
    private double defaultThreshold;

    @Override
    public int createFromApprovedFeedback(Long feedbackId, Long reviewerId) {
        if (feedbackId == null || queryRecordService == null) {
            return 0;
        }
        Feedback fb = feedbackMapper.selectFeedbackById(feedbackId);
        if (fb == null || !"APPROVED".equals(fb.getReviewStatus())) {
            return 0;
        }
        QueryRecord qr = queryRecordService.selectQueryRecordById(fb.getQueryId());
        if (qr == null) {
            log.warn("反馈关联的查询记录不存在: queryId={}", fb.getQueryId());
            return 0;
        }
        String correctedSql = fb.getSuggestedSql() != null ? fb.getSuggestedSql().trim() : qr.getGeneratedSql();
        if (correctedSql == null || correctedSql.isEmpty()) {
            log.warn("无可用 correctedSql: feedbackId={}", feedbackId);
            return 0;
        }
        FeedbackCorrection fc = new FeedbackCorrection();
        fc.setQueryId(fb.getQueryId());
        fc.setOriginalQuestion(qr.getQuestion());
        fc.setCorrectedSql(correctedSql);
        fc.setReviewedBy(reviewerId);
        fc.setReviewedAt(new java.util.Date());
        fc.setStatus("APPROVED");
        fc.setUsedInNl2sql(0);
        feedbackCorrectionMapper.insert(fc);
        log.info("创建反馈修正: feedbackId={}, fcId={}", feedbackId, fc.getId());
        return 1;
    }

    @Override
    public List<FeedbackCorrection> findSimilarCorrections(String question, double threshold) {
        if (question == null || question.trim().isEmpty()) {
            return List.of();
        }
        List<FeedbackCorrection> all = feedbackCorrectionMapper.selectByStatus("APPROVED");
        if (all == null || all.isEmpty()) {
            return List.of();
        }
        String q = question.trim();
        double t = threshold > 0 && threshold <= 1 ? threshold : defaultThreshold;
        List<Scored> scored = new ArrayList<>();
        for (FeedbackCorrection fc : all) {
            String orig = fc.getOriginalQuestion();
            if (orig == null || orig.trim().isEmpty()) continue;
            double sim = similarity(q, orig.trim());
            if (sim >= t) {
                scored.add(new Scored(fc, sim));
            }
        }
        scored.sort((a, b) -> Double.compare(b.score, a.score));
        return scored.stream().map(s -> s.fc).collect(Collectors.toList());
    }

    private static double similarity(String a, String b) {
        if (a.equals(b)) return 1.0;
        Set<String> setA = tokenize(a);
        Set<String> setB = tokenize(b);
        if (setA.isEmpty() && setB.isEmpty()) return 1.0;
        if (setA.isEmpty() || setB.isEmpty()) return 0.0;
        Set<String> inter = new HashSet<>(setA);
        inter.retainAll(setB);
        Set<String> union = new HashSet<>(setA);
        union.addAll(setB);
        return (double) inter.size() / union.size();
    }

    private static Set<String> tokenize(String s) {
        Set<String> set = new HashSet<>();
        Matcher m = Pattern.compile("[\\p{L}\\p{N}]+").matcher(s);
        while (m.find()) set.add(m.group().toLowerCase());
        if (set.isEmpty() && !s.isEmpty()) {
            for (int i = 0; i < s.length(); i++) set.add(String.valueOf(s.charAt(i)));
        }
        return set;
    }

    private static class Scored {
        final FeedbackCorrection fc;
        final double score;
        Scored(FeedbackCorrection fc, double score) { this.fc = fc; this.score = score; }
    }
}
