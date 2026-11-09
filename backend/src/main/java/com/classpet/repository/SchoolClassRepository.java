package com.classpet.repository;

import com.classpet.entity.SchoolClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SchoolClassRepository extends JpaRepository<SchoolClass, String> {

    List<SchoolClass> findByTeacherIdOrderBySortOrderAscCreatedAtAsc(String teacherId);

    Optional<SchoolClass> findByIdAndTeacherId(String id, String teacherId);

    Optional<SchoolClass> findByCode(String code);

    Optional<SchoolClass> findFirstByTeacherIdOrderBySortOrderAscCreatedAtAsc(String teacherId);

    List<SchoolClass> findByShowOnBoardTrue();

    boolean existsByCode(String code);

    boolean existsByTeacherIdAndName(String teacherId, String name);

    boolean existsByTeacherIdAndNameAndIdNot(String teacherId, String name, String id);
}
