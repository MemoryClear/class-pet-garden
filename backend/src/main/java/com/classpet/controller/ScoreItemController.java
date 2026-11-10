package com.classpet.controller;

import com.classpet.dto.ScoreItemDto;
import com.classpet.entity.ScoreItem;
import com.classpet.security.JwtAuthenticationFilter.AuthenticatedUser;
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
            @AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(scoreItemService.getScoreItems(principal.teacherId()));
    }

    @PostMapping
    public ResponseEntity<?> createScoreItem(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody ScoreItemDto.CreateRequest req) {
        return ResponseEntity.ok(scoreItemService.createScoreItem(
                req.icon, req.name, req.point, principal.teacherId()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateScoreItem(
            @PathVariable String id,
            @AuthenticationPrincipal AuthenticatedUser principal,
            @Valid @RequestBody ScoreItemDto.UpdateRequest req) {
        try {
            return ResponseEntity.ok(scoreItemService.updateScoreItem(
                    id, req.icon, req.name, req.point, principal.teacherId()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteScoreItem(
            @PathVariable String id,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        scoreItemService.deleteScoreItem(id, principal.teacherId());
        return ResponseEntity.ok(Map.of("success", true));
    }
}