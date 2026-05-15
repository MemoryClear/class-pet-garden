package com.classpet.repository;

import com.classpet.entity.ClassroomPokemonPool;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClassroomPokemonPoolRepository extends JpaRepository<ClassroomPokemonPool, String> {
    
    Optional<ClassroomPokemonPool> findByClassroomId(String classroomId);
    
    void deleteByClassroomId(String classroomId);
}
