package com.classpet.controller;

import com.classpet.entity.ScoreHistory;
import com.classpet.security.JwtAuthenticationFilter.AuthenticatedTeacher;
import com.classpet.service.HistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/history")
public class HistoryController {

    @Autowired private HistoryService historyService;

    @GetMapping
    public ResponseEntity<List<ScoreHistory>> getHistory(
            @AuthenticationPrincipal AuthenticatedTeacher principal,
            @RequestParam(required = false) String studentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(historyService.getHistory(principal.teacherId(), studentId, from, to));
    }

    @PostMapping("/{id}/revoke")
    public ResponseEntity<?> revokeScore(
            @PathVariable String id,
            @AuthenticationPrincipal AuthenticatedTeacher principal) {
        try {
            return ResponseEntity.ok(historyService.revokeScore(id, principal.teacherId()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}