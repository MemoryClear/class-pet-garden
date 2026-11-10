package com.classpet.repository;

import com.classpet.entity.PokemonBall;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PokemonBallRepository extends JpaRepository<PokemonBall, String> {

    /** 最早的一颗未开球（先发先开） */
    Optional<PokemonBall> findFirstByStudentIdAndOpenedAtIsNullOrderByGrantedAtAscIdAsc(String studentId);

    List<PokemonBall> findByStudentIdAndOpenedAtIsNullOrderByGrantedAtAscIdAsc(String studentId);

    long countByStudentIdAndOpenedAtIsNull(String studentId);

    long countByStudentId(String studentId);

    void deleteByStudentId(String studentId);
}
