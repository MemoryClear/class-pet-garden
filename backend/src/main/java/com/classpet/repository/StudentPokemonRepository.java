package com.classpet.repository;

import com.classpet.entity.StudentPokemon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentPokemonRepository extends JpaRepository<StudentPokemon, String> {
    List<StudentPokemon> findByStudentId(String studentId);
    List<StudentPokemon> findByStudentIdOrderByCreatedAtDesc(String studentId);
    void deleteByStudentId(String studentId);
}
