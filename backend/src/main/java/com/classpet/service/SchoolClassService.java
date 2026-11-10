package com.classpet.service;

import com.classpet.entity.SchoolClass;
import com.classpet.entity.Student;
import com.classpet.repository.ClassroomPokemonPoolRepository;
import com.classpet.repository.SchoolClassRepository;
import com.classpet.repository.StudentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 班级服务：多班级的增删改查、班级上下文解析、学号拼装。
 *
 * <p>学号规则：{@code 班级号 + "-" + S0001}，例如 {@code C7F3-S0001}。
 * 学生登录时只输入 {@code S0001}，客户端把选中的 classId 一起提交，
 * 服务端拼出完整学号再查库。</p>
 */
@Service
public class SchoolClassService {

    private static final Logger logger = LoggerFactory.getLogger(SchoolClassService.class);

    private final SchoolClassRepository classRepository;
    private final StudentRepository studentRepository;
    private final ClassroomPokemonPoolRepository poolRepository;

    public SchoolClassService(SchoolClassRepository classRepository,
                              StudentRepository studentRepository,
                              ClassroomPokemonPoolRepository poolRepository) {
        this.classRepository = classRepository;
        this.studentRepository = studentRepository;
        this.poolRepository = poolRepository;
    }

    // ---------------- 查询 ----------------

    public List<SchoolClass> list(String teacherId) {
        return classRepository.findByTeacherIdOrderBySortOrderAscCreatedAtAsc(teacherId);
    }

    public Optional<SchoolClass> find(String classId) {
        return classId == null ? Optional.empty() : classRepository.findById(classId);
    }

    /**
     * 解析"当前班级"：
     * 1) 传了 classId 且属于该教师 → 用它；
     * 2) 否则取该教师排序第一的班级；
     * 3) 一个都没有 → 自动创建一个默认班级（兼容老账号 / 首次登录）。
     */
    @Transactional
    public SchoolClass resolve(String classId, String teacherId, String defaultName) {
        if (classId != null && !classId.isBlank()) {
            Optional<SchoolClass> owned = classRepository.findByIdAndTeacherId(classId, teacherId);
            if (owned.isPresent()) return owned.get();
        }
        return classRepository.findFirstByTeacherIdOrderBySortOrderAscCreatedAtAsc(teacherId)
                .orElseGet(() -> create(teacherId, defaultName == null || defaultName.isBlank() ? "我的班级" : defaultName));
    }

    public SchoolClass resolve(String classId, String teacherId) {
        return resolve(classId, teacherId, null);
    }

    /** 只取 classId，方便传给 service 层做过滤 */
    public String resolveId(String classId, String teacherId) {
        return resolve(classId, teacherId, null).getId();
    }

    // ---------------- 增删改 ----------------

    @Transactional
    public SchoolClass create(String teacherId, String name) {
        String cleanName = name == null ? "" : name.trim();
        if (cleanName.isEmpty()) throw new IllegalArgumentException("班级名称不能为空");
        if (cleanName.length() > 20) throw new IllegalArgumentException("班级名称最多 20 个字");
        if (classRepository.existsByTeacherIdAndName(teacherId, cleanName)) {
            throw new IllegalArgumentException("已存在同名班级");
        }
        List<SchoolClass> exists = list(teacherId);
        SchoolClass c = new SchoolClass(teacherId, cleanName, generateCode());
        c.setSortOrder(exists.size());
        SchoolClass saved = classRepository.save(c);
        logger.info("教师 {} 新建班级 {}({})", teacherId, cleanName, c.getCode());
        return saved;
    }

    @Transactional
    public SchoolClass rename(String classId, String teacherId, String name) {
        SchoolClass c = classRepository.findByIdAndTeacherId(classId, teacherId)
                .orElseThrow(() -> new IllegalArgumentException("班级不存在"));
        String cleanName = name == null ? "" : name.trim();
        if (cleanName.isEmpty()) throw new IllegalArgumentException("班级名称不能为空");
        if (cleanName.length() > 20) throw new IllegalArgumentException("班级名称最多 20 个字");
        if (classRepository.existsByTeacherIdAndNameAndIdNot(teacherId, cleanName, classId)) {
            throw new IllegalArgumentException("已存在同名班级");
        }
        c.setName(cleanName);
        return classRepository.save(c);
    }

    @Transactional
    public void delete(String classId, String teacherId) {
        SchoolClass c = classRepository.findByIdAndTeacherId(classId, teacherId)
                .orElseThrow(() -> new IllegalArgumentException("班级不存在"));
        long count = studentRepository.countByClassId(classId);
        if (count > 0) {
            throw new IllegalArgumentException("班级里还有 " + count + " 位学生，请先删除或转出学生");
        }
        if (list(teacherId).size() <= 1) {
            throw new IllegalArgumentException("至少保留一个班级");
        }
        classRepository.delete(c);
        // 班级没了，班级级宝可梦池也一起清掉
        poolRepository.deleteByClassroomId(classId);
    }

    @Transactional
    public SchoolClass setShowOnBoard(String classId, String teacherId, boolean show) {
        SchoolClass c = classRepository.findByIdAndTeacherId(classId, teacherId)
                .orElseThrow(() -> new IllegalArgumentException("班级不存在"));
        c.setShowOnBoard(show);
        return classRepository.save(c);
    }

    // ---------------- 学号 ----------------

    /** 学生登录用：把用户输入的 S0001 补成 班级号-S0001 */
    public String toFullStudentNo(SchoolClass c, String input) {
        String raw = input == null ? "" : input.trim().toUpperCase(Locale.ROOT);
        if (raw.isEmpty()) return raw;
        int dash = raw.lastIndexOf('-');
        if (dash >= 0) raw = raw.substring(dash + 1);
        if (raw.isEmpty()) return "";
        return c.getCode() + "-" + raw;
    }

    /** 生成「班级号-S0001」，序号按班级内已有序号递增 */
    public String nextStudentNo(SchoolClass c) {
        int max = 0;
        String prefix = c.getCode() + "-S";
        for (Student s : studentRepository.findByClassIdOrderByCreatedAtAsc(c.getId())) {
            String sno = s.getStudentNo();
            if (sno != null && sno.startsWith(prefix)) {
                try {
                    max = Math.max(max, Integer.parseInt(sno.substring(prefix.length())));
                } catch (NumberFormatException ignored) {
                    // 非标准学号（手工改过），跳过
                }
            }
        }
        return String.format("%s-S%04d", c.getCode(), max + 1);
    }

    // ---------------- 公开信息（学生登录下拉框） ----------------

    /**
     * 学生登录页的班级下拉数据：只返回某个教师名下的班级。
     * 调用方（AuthController）负责先用"学校/教师码"定位教师，
     * 避免把全站班级名单暴露出去。
     */
    public List<Map<String, Object>> classOptionsOfTeacher(String teacherId) {
        List<SchoolClass> list = list(teacherId);
        List<Map<String, Object>> out = new ArrayList<>(list.size());
        for (SchoolClass c : list) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("name", c.getName());
            out.add(m);
        }
        return out;
    }

    // ---------------- 内部 ----------------

    /** 生成 4 位班级号：C + UUID 前 3 位，冲突则重试 */
    private String generateCode() {
        for (int i = 0; i < 60; i++) {
            String hex = UUID.randomUUID().toString().replace("-", "").substring(0, 3).toUpperCase(Locale.ROOT);
            String code = "C" + hex;
            if (!classRepository.existsByCode(code)) return code;
        }
        // 极端情况下退化为 6 位
        return "C" + UUID.randomUUID().toString().replace("-", "").substring(0, 5).toUpperCase(Locale.ROOT);
    }
}
