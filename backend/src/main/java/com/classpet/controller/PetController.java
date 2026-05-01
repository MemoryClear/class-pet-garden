package com.classpet.controller;

import com.classpet.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pets")
public class PetController {

    @Autowired private StudentService studentService;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getPetLibrary() {
        return ResponseEntity.ok(studentService.getPetLibrary());
    }
}