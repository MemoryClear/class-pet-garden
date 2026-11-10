package com.classpet.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 精灵球（单个实例）。
 *
 * <p>以前精灵球只是 {@code students.pokemon_bells} 上的一个数字，开球时才去读当时的池子，
 * 导致"上个月发的球会被这个月改的池子影响"。现在每发一个球就落一条记录，
 * 并把<b>发放那一刻的班级池</b>存进 {@code pool_snapshot}，开球时按快照抽，
 * 池子之后怎么改都不影响已经发出去的球。</p>
 *
 * <p>{@code students.pokemon_bells} 仍然保留为"未开球数量"的快速计数，
 * 与这里的未开球行数保持同步；历史遗留的球（没有行）开球时回退到当前池子。</p>
 */
@Entity
@Table(name = "pokemon_balls", indexes = {
        @Index(name = "idx_ball_student", columnList = "student_id"),
        @Index(name = "idx_ball_open", columnList = "opened_at")
})
public class PokemonBall {

    /** 发放来源 */
    public static final String SOURCE_MONTHLY = "MONTHLY";
    public static final String SOURCE_MANUAL = "MANUAL";
    public static final String SOURCE_MIGRATED = "MIGRATED";
    public static final String SOURCE_SHOP = "SHOP";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "student_id", nullable = false, length = 36)
    private String studentId;

    @Column(name = "teacher_id", length = 36)
    private String teacherId;

    /** 发放时学生所属班级 */
    @Column(name = "class_id", length = 36)
    private String classId;

    /**
     * 发放时的池子快照（JSON 数组，含 pokedexId / weight）。
     * 空或 "[]" 表示当时池子为空，开球时按默认池（Gen1）处理。
     */
    @Column(name = "pool_snapshot", length = 4000)
    private String poolSnapshot = "[]";

    @Column(name = "source", length = 20)
    private String source;

    @Column(name = "granted_at")
    private LocalDateTime grantedAt;

    /** 开球时间；为空表示还没开 */
    @Column(name = "opened_at")
    private LocalDateTime openedAt;

    /** 开出来的图鉴编号，便于追溯 */
    @Column(name = "opened_pokedex_id")
    private Integer openedPokedexId;

    @PrePersist
    protected void onCreate() {
        if (grantedAt == null) grantedAt = LocalDateTime.now();
    }

    public PokemonBall() { }

    public PokemonBall(String studentId, String teacherId, String classId,
                       String poolSnapshot, String source) {
        this.studentId = studentId;
        this.teacherId = teacherId;
        this.classId = classId;
        this.poolSnapshot = (poolSnapshot == null || poolSnapshot.isBlank()) ? "[]" : poolSnapshot;
        this.source = source;
        this.grantedAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getTeacherId() { return teacherId; }
    public void setTeacherId(String teacherId) { this.teacherId = teacherId; }

    public String getClassId() { return classId; }
    public void setClassId(String classId) { this.classId = classId; }

    public String getPoolSnapshot() { return poolSnapshot; }
    public void setPoolSnapshot(String poolSnapshot) { this.poolSnapshot = poolSnapshot; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public LocalDateTime getGrantedAt() { return grantedAt; }
    public void setGrantedAt(LocalDateTime grantedAt) { this.grantedAt = grantedAt; }

    public LocalDateTime getOpenedAt() { return openedAt; }
    public void setOpenedAt(LocalDateTime openedAt) { this.openedAt = openedAt; }

    public Integer getOpenedPokedexId() { return openedPokedexId; }
    public void setOpenedPokedexId(Integer openedPokedexId) { this.openedPokedexId = openedPokedexId; }

    public boolean isOpened() { return openedAt != null; }
}
