package com.classpet.migration;

import com.classpet.entity.ExchangeRecord;
import com.classpet.entity.SchoolClass;
import com.classpet.entity.ScoreHistory;
import com.classpet.entity.Student;
import com.classpet.entity.Teacher;
import com.classpet.repository.ExchangeRecordRepository;
import com.classpet.repository.SchoolClassRepository;
import com.classpet.repository.ScoreHistoryRepository;
import com.classpet.repository.StudentRepository;
import com.classpet.repository.TeacherRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 多班级迁移器（老库 → 多班级模型）。
 *
 * <p>做四件事，全部幂等，可以反复启动：</p>
 * <ol>
 *   <li>每个还没有班级的教师，创建一个默认班级（名字取教师原来的 className）。</li>
 *   <li>把 classId 为空的学生挂到该教师的默认班级，并把学号从
 *       {@code 教师号-S0001} 改成 {@code 班级号-S0001}。</li>
 *   <li>把这些学生的积分历史 / 兑换记录补上 classId（用于按班级分页）。</li>
 *   <li>把教师级的 showOnBoard 复制到默认班级上（看板从此按班级展示）。</li>
 * </ol>
 */
@Component
@Order(Integer.MIN_VALUE + 2)
public class ClassMigration implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(ClassMigration.class);
    private static final Pattern TAIL_NO = Pattern.compile(".*S(\\d{1,6})$");

    private final TeacherRepository teacherRepo;
    private final SchoolClassRepository classRepo;
    private final StudentRepository studentRepo;
    private final ScoreHistoryRepository scoreHistoryRepo;
    private final ExchangeRecordRepository exchangeRecordRepo;

    public ClassMigration(TeacherRepository teacherRepo,
                          SchoolClassRepository classRepo,
                          StudentRepository studentRepo,
                          ScoreHistoryRepository scoreHistoryRepo,
                          ExchangeRecordRepository exchangeRecordRepo) {
        this.teacherRepo = teacherRepo;
        this.classRepo = classRepo;
        this.studentRepo = studentRepo;
        this.scoreHistoryRepo = scoreHistoryRepo;
        this.exchangeRecordRepo = exchangeRecordRepo;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Teacher> teachers = teacherRepo.findAll();
        int createdClasses = 0;
        int movedStudents = 0;

        for (Teacher t : teachers) {
            List<SchoolClass> classes = classRepo.findByTeacherIdOrderBySortOrderAscCreatedAtAsc(t.getId());
            if (classes.isEmpty()) {
                String name = (t.getClassName() == null || t.getClassName().isBlank())
                        ? "我的班级" : t.getClassName().trim();
                SchoolClass c = new SchoolClass(t.getId(), name, generateCode());
                c.setShowOnBoard(Boolean.TRUE.equals(t.isShowOnBoard()));
                c.setSortOrder(0);
                c = classRepo.save(c);
                classes = List.of(c);
                createdClasses++;
                logger.info("迁移：为教师 {} 创建默认班级 {}({})", t.getUsername(), name, c.getCode());
            }

            SchoolClass primary = classes.get(0);
            // 学生挂班级 + 学号改造
            List<Student> students = studentRepo.findByTeacherIdOrderByCreatedAtAsc(t.getId());
            int seq = nextSeqOf(students, primary.getCode());
            for (Student s : students) {
                if (s.getClassId() != null) continue;
                s.setClassId(primary.getId());
                s.setStudentNo(primary.getCode() + "-" + tailOf(s.getStudentNo(), seq++));
                studentRepo.save(s);
                movedStudents++;
            }
        }

        // 历史 / 兑换记录补班级
        Map<String, String> studentClass = new HashMap<>();
        for (Student s : studentRepo.findAll()) {
            if (s.getClassId() != null) studentClass.put(s.getId(), s.getClassId());
        }
        int fixedHistory = 0;
        for (ScoreHistory h : scoreHistoryRepo.findByClassIdIsNull()) {
            String cid = studentClass.get(h.getStudentId());
            if (cid != null) {
                h.setClassId(cid);
                scoreHistoryRepo.save(h);
                fixedHistory++;
            }
        }
        int fixedExchange = 0;
        for (ExchangeRecord e : exchangeRecordRepo.findByClassIdIsNull()) {
            String cid = studentClass.get(e.getStudentId());
            if (cid != null) {
                e.setClassId(cid);
                exchangeRecordRepo.save(e);
                fixedExchange++;
            }
        }

        if (createdClasses + movedStudents + fixedHistory + fixedExchange > 0) {
            logger.info("多班级迁移完成：新建班级 {}，学生归班 {}，历史补班级 {}，兑换补班级 {}",
                    createdClasses, movedStudents, fixedHistory, fixedExchange);
        }
    }

    /** 保留原学号里的 S0001 数字部分，取不到就顺延 */
    private String tailOf(String studentNo, int fallbackSeq) {
        if (studentNo != null) {
            Matcher m = TAIL_NO.matcher(studentNo.trim().toUpperCase(Locale.ROOT));
            if (m.matches()) {
                try {
                    return String.format("S%04d", Integer.parseInt(m.group(1)));
                } catch (NumberFormatException ignored) { }
            }
        }
        return String.format("S%04d", fallbackSeq);
    }

    private int nextSeqOf(List<Student> students, String code) {
        int max = 0;
        for (Student s : students) {
            String sno = s.getStudentNo();
            if (sno == null) continue;
            Matcher m = TAIL_NO.matcher(sno.toUpperCase(Locale.ROOT));
            if (m.matches()) {
                try { max = Math.max(max, Integer.parseInt(m.group(1))); } catch (NumberFormatException ignored) { }
            }
        }
        return max + 1;
    }

    private String generateCode() {
        for (int i = 0; i < 60; i++) {
            String code = "C" + UUID.randomUUID().toString().replace("-", "")
                    .substring(0, 3).toUpperCase(Locale.ROOT);
            if (!classRepo.existsByCode(code)) return code;
        }
        return "C" + UUID.randomUUID().toString().replace("-", "").substring(0, 5).toUpperCase(Locale.ROOT);
    }
}
