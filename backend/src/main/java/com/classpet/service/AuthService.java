package com.classpet.service;

import com.classpet.dto.AuthDto;
import com.classpet.entity.Teacher;
import com.classpet.repository.TeacherRepository;
import com.classpet.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    @Autowired private TeacherRepository teacherRepo;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider tokenProvider;

    public AuthDto.LoginResponse register(AuthDto.RegisterRequest req) {
        if (!req.password.equals(req.confirmPassword)) {
            throw new IllegalArgumentException("两次密码不一致");
        }
        if (teacherRepo.existsByUsername(req.username)) {
            throw new IllegalArgumentException("用户名已存在");
        }
        Teacher teacher = new Teacher(req.username, passwordEncoder.encode(req.password));
        teacher = teacherRepo.save(teacher);
        String token = tokenProvider.generateToken(teacher.getUsername(), teacher.getId());
        return new AuthDto.LoginResponse(token, teacher.getUsername(), teacher.getId(),
                teacher.isActivated(), teacher.getSystemName(),
                teacher.getClassName(), teacher.getTheme());
    }

    public AuthDto.LoginResponse login(AuthDto.LoginRequest req) {
        Teacher teacher = teacherRepo.findByUsername(req.username)
                .orElseThrow(() -> new IllegalArgumentException("用户名或密码错误"));
        if (!passwordEncoder.matches(req.password, teacher.getPassword())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        String token = tokenProvider.generateToken(teacher.getUsername(), teacher.getId());
        return new AuthDto.LoginResponse(token, teacher.getUsername(), teacher.getId(),
                teacher.isActivated(), teacher.getSystemName(),
                teacher.getClassName(), teacher.getTheme());
    }

    public AuthDto.LoginResponse activate(String username, String teacherId, String code) {
        Teacher teacher = teacherRepo.findById(teacherId)
                .orElseThrow(() -> new IllegalArgumentException("账号不存在"));
        if (teacher.isActivated()) {
            return new AuthDto.LoginResponse(null, teacher.getUsername(), teacher.getId(),
                    true, teacher.getSystemName(), teacher.getClassName(), teacher.getTheme());
        }
        // 仅接受 memory_clear 作为激活码
        if (code == null || !code.equalsIgnoreCase("memory_clear")) {
            throw new IllegalArgumentException("激活码错误，请输入 memory_clear");
        }
        teacher.setActivated(true);
        teacher.setActivateCode(code);
        teacherRepo.save(teacher);
        String token = tokenProvider.generateToken(teacher.getUsername(), teacher.getId());
        return new AuthDto.LoginResponse(token, teacher.getUsername(), teacher.getId(),
                true, teacher.getSystemName(), teacher.getClassName(), teacher.getTheme());
    }

    public Optional<Teacher> findById(String id) {
        return teacherRepo.findById(id);
    }

    public Teacher save(Teacher teacher) {
        return teacherRepo.save(teacher);
    }
}