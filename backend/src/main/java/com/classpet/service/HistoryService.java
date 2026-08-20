package com.classpet.service;

import com.classpet.entity.ExchangeRecord;
import com.classpet.entity.ScoreHistory;
import com.classpet.entity.Student;
import com.classpet.repository.ExchangeRecordRepository;
import com.classpet.repository.ScoreHistoryRepository;
import com.classpet.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class HistoryService {

    @Autowired private ScoreHistoryRepository historyRepo;
    @Autowired private StudentRepository studentRepo;
    @Autowired private ExchangeRecordRepository exchangeRecordRepo;

    // 匹配「购买「道具名」」中的道具名
    private static final Pattern PURCHASE_ITEM_PATTERN = Pattern.compile("购买「(.+?)」");

    /**
     * 判断该 score_history 是否对应一条已赠送出去的兑换道具。
     * 仅对“购买「道具名」”类型的扣分有效。
     */
    private boolean isGiftedOut(String teacherId, ScoreHistory sh) {
        String name = sh.getScoreItemName();
        if (name == null || !name.contains("购买「")) return false;
        Matcher m = PURCHASE_ITEM_PATTERN.matcher(name);
        if (!m.find()) return false;
        String itemName = m.group(1);
        LocalDateTime base = sh.getCreatedAt() != null ? sh.getCreatedAt() : LocalDateTime.now();
        LocalDateTime from = base.minusDays(30);
        LocalDateTime to = base.plusDays(30);
        List<ExchangeRecord> matches = exchangeRecordRepo
                .findByTeacherIdAndStudentIdAndItemNameAndCreatedAtBetweenOrderByCreatedAtDesc(
                        teacherId, sh.getStudentId(), itemName, from, to);
        for (ExchangeRecord er : matches) {
            if ("GIFT_OUT".equals(er.getActionType())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 游标分页：响应包含 items / hasMore / nextCursor
     * - teacherId 必填
     * - studentId/from/to 可选过滤（兼容旧版全量接口）
     * - cursorTime + cursorId 决定从哪条之后继续（首次为 null）
     * - limit 上限 100
     */
    public Map<String, Object> getHistoryPage(String teacherId,
                                              String studentId,
                                              LocalDate from, LocalDate to,
                                              LocalDateTime cursorTime, String cursorId,
                                              int limit) {
        int safeLimit = Math.max(1, Math.min(100, limit));
        // 多取1条用于判断 hasMore
        PageRequest pageReq = PageRequest.of(0, safeLimit + 1);
        List<ScoreHistory> rows;
        if (studentId != null && !studentId.isEmpty()) {
            rows = historyRepo.findTeacherStudentPage(teacherId, studentId, cursorTime, cursorId, pageReq);
        } else if (from != null && to != null) {
            LocalDateTime fromDt = from.atStartOfDay();
            LocalDateTime toDt = to.plusDays(1).atStartOfDay();
            rows = historyRepo.findTeacherBetweenPage(teacherId, fromDt, toDt, cursorTime, cursorId, pageReq);
        } else {
            rows = historyRepo.findTeacherPage(teacherId, cursorTime, cursorId, pageReq);
        }
        boolean hasMore = rows.size() > safeLimit;
        if (hasMore) rows = rows.subList(0, safeLimit);

        // 转换每条为 Map，附带 giftedOut 标记（是否对应已赠送的购买道具）
        List<Map<String, Object>> items = new ArrayList<>(rows.size());
        for (ScoreHistory sh : rows) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", sh.getId());
            m.put("studentId", sh.getStudentId());
            m.put("studentName", sh.getStudentName());
            m.put("scoreItemName", sh.getScoreItemName());
            m.put("scoreItemIcon", sh.getScoreItemIcon());
            m.put("point", sh.getPoint());
            m.put("teacherId", sh.getTeacherId());
            m.put("revoked", sh.getRevoked());
            m.put("revokedAt", sh.getRevokedAt());
            m.put("createdAt", sh.getCreatedAt());
            m.put("giftedOut", isGiftedOut(teacherId, sh));
            items.add(m);
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("items", items);
        resp.put("hasMore", hasMore);
        if (hasMore && !rows.isEmpty()) {
            ScoreHistory tail = rows.get(rows.size() - 1);
            Map<String, Object> cursor = new HashMap<>();
            cursor.put("createdAt", tail.getCreatedAt());
            cursor.put("id", tail.getId());
            resp.put("nextCursor", cursor);
        } else {
            resp.put("nextCursor", null);
        }
        return resp;
    }

    @Transactional
    public ScoreHistory revokeScore(String historyId, String teacherId) {
        ScoreHistory record = historyRepo.findById(historyId)
                .orElseThrow(() -> new IllegalArgumentException("记录不存在"));
        if (!record.getTeacherId().equals(teacherId)) {
            throw new IllegalArgumentException("无权限操作");
        }
        if (Boolean.TRUE.equals(record.getRevoked())) {
            throw new IllegalArgumentException("该记录已被撤销");
        }

        // 防护：如果该扣分对应"购买道具"，且对应道具已赠送出去，则禁止撤销
        String name = record.getScoreItemName();
        if (name != null && name.contains("购买「")) {
            Matcher m = PURCHASE_ITEM_PATTERN.matcher(name);
            if (m.find()) {
                String itemName = m.group(1);
                // 在 score_history 时间点前后 30 天内查找该学生该道具的兑换记录（覆盖兑换在后期被送出的场景）
                LocalDateTime from = record.getCreatedAt() != null
                        ? record.getCreatedAt().minusDays(30)
                        : LocalDateTime.now().minusDays(30);
                LocalDateTime to = record.getCreatedAt() != null
                        ? record.getCreatedAt().plusDays(30)
                        : LocalDateTime.now().plusDays(1);
                List<ExchangeRecord> matches = exchangeRecordRepo
                        .findByTeacherIdAndStudentIdAndItemNameAndCreatedAtBetweenOrderByCreatedAtDesc(
                                teacherId, record.getStudentId(), itemName, from, to);
                for (ExchangeRecord er : matches) {
                    if ("GIFT_OUT".equals(er.getActionType())) {
                        throw new IllegalArgumentException("该道具已赠送，无法撤销");
                    }
                }
            }
        }

        // 反向调整学生积分
        Student student = studentRepo.findById(record.getStudentId())
                .orElseThrow(() -> new IllegalArgumentException("学生不存在"));
        student.setFood(Math.max(0, student.getFood() - record.getPoint()));
        studentRepo.save(student);

        // 标记撤销
        record.setRevoked(true);
        record.setRevokedAt(LocalDateTime.now());
        return historyRepo.save(record);
    }
}