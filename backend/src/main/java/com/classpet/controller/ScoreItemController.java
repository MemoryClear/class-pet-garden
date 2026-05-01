package com.classpet.controller;

import com.classpet.dto.ScoreItemDto;
import com.classpet.entity.ScoreItem;
import com.classpet.security.JwtAuthenticationFilter.AuthenticatedTeacher;
import com.classpet.service.ScoreItemService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/score-items")
public class ScoreItemController {

    @Autowired private ScoreItemService scoreItemService;

    @GetMapping
    public ResponseEntity<List<ScoreItem>> getScoreItems(
            @AuthenticationPrincipal AuthenticatedTeacher principal) {
        return ResponseEntity.ok(scoreItemService.getScoreItems(principal.teacherId()));
    }

    @PostMapping
    public ResponseEntity<?> createScoreItem(
            @AuthenticationPrincipal AuthenticatedTeacher principal,
            @Valid @RequestBody ScoreItemDto.CreateRequest req) {
        return ResponseEntity.ok(scoreItemService.createScoreItem(
                req.icon, req.name, req.point, principal.teacherId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteScoreItem(
            @PathVariable String id,
            @AuthenticationPrincipal AuthenticatedTeacher principal) {
        scoreItemService.deleteScoreItem(id, principal.teacherId());
        return ResponseEntity.ok(Map.of("success", true));
    }
}