package com.classpet.util;

import com.classpet.entity.Student;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * 学生 evolutionItems 库存调整工具。
 * 字段存储为 JSON 字符串，统一处理 +1 / -1。
 */
public final class EvolutionItemUtil {

    private static final Logger log = LoggerFactory.getLogger(EvolutionItemUtil.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Integer>> TYPE = new TypeReference<Map<String, Integer>>() {};

    private EvolutionItemUtil() {}

    /**
     * 调整 evolutionItems 库存（delta 可为 -1 / +1）。
     * JSON 损坏会抛出异常而非静默忽略。
     */
    public static void adjust(Student student, String itemKey, int delta) {
        try {
            Map<String, Integer> map = new HashMap<>();
            String json = student.getEvolutionItems();
            if (json != null && !json.isEmpty() && !"{}".equals(json)) {
                map = MAPPER.readValue(json, TYPE);
            }
            int cur = map.getOrDefault(itemKey, 0);
            int next = cur + delta;
            if (next > 0) map.put(itemKey, next);
            else map.remove(itemKey);
            student.setEvolutionItems(MAPPER.writeValueAsString(map));
        } catch (Exception e) {
            log.warn("Failed to adjust evolutionItems for student {} key {} delta {}: {}",
                    student.getId(), itemKey, delta, e.getMessage());
            throw new RuntimeException("evolutionItems JSON 损坏或读写失败", e);
        }
    }
}