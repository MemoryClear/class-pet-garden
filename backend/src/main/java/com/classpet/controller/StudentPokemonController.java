package com.classpet.controller;

import com.classpet.entity.StudentPokemon;
import com.classpet.security.JwtAuthenticationFilter.AuthenticatedUser;
import com.classpet.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/student")
public class StudentPokemonController {

    @Autowired private StudentService studentService;

    // 获取当前学生的宝可梦列表
    @GetMapping("/pokemon")
    public ResponseEntity<?> getMyPokemon(@AuthenticationPrincipal AuthenticatedUser principal) {
        if (!principal.isStudent()) {
            return ResponseEntity.status(403).body(Map.of("error", "仅限学生访问"));
        }
        List<Map<String, Object>> pokemon = studentService.getStudentPokemon(principal.studentId());
        return ResponseEntity.ok(pokemon);
    }

    // 领养新宝可梦（无上限，直接新增）
    @PostMapping("/adopt-pokemon")
    public ResponseEntity<?> adoptPokemon(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestBody Map<String, Object> body) {
        if (!principal.isStudent()) {
            return ResponseEntity.status(403).body(Map.of("error", "仅限学生使用"));
        }
        Integer pokedexId = body.get("pokedexId") != null ? Integer.parseInt(body.get("pokedexId").toString()) : null;
        String name = body.get("name") != null ? body.get("name").toString() : "";
        String image = body.get("image") != null ? body.get("image").toString() : "";
        String types = body.get("types") != null ? body.get("types").toString() : "[]";
        
        if (pokedexId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "pokedexId不能为空"));
        }
        
        StudentPokemon pokemon = studentService.adoptPokemon(principal.studentId(), pokedexId, name, image, types);
        return ResponseEntity.ok(Map.of("success", true, "pokemon", pokemon));
    }

    // 喂养宝可梦（加分）
    @PostMapping("/feed-pokemon")
    public ResponseEntity<?> feedPokemon(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestBody Map<String, Object> body) {
        if (!principal.isStudent()) {
            return ResponseEntity.status(403).body(Map.of("error", "仅限学生使用"));
        }
        String pokemonId = body.get("pokemonId") != null ? body.get("pokemonId").toString() : null;
        Integer points = body.get("points") != null ? ((Number) body.get("points")).intValue() : 1;
        
        if (pokemonId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "pokemonId不能为空"));
        }
        
        try {
            StudentPokemon pokemon = studentService.feedPokemon(principal.studentId(), pokemonId, points);
            return ResponseEntity.ok(Map.of("success", true, "pokemon", pokemon));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 获取学生宝可梦数量
    @GetMapping("/pokemon-count")
    public ResponseEntity<?> getPokemonCount(@AuthenticationPrincipal AuthenticatedUser principal) {
        if (!principal.isStudent()) {
            return ResponseEntity.status(403).body(Map.of("error", "仅限学生访问"));
        }
        long count = studentService.getPokemonCount(principal.studentId());
        return ResponseEntity.ok(Map.of("count", count));
    }
    
    // 获取精灵球数量
    @GetMapping("/pokemon-balls")
    public ResponseEntity<?> getPokemonBalls(@AuthenticationPrincipal AuthenticatedUser principal) {
        if (!principal.isStudent()) {
            return ResponseEntity.status(403).body(Map.of("error", "仅限学生访问"));
        }
        int balls = studentService.getPokemonBalls(principal.studentId());
        return ResponseEntity.ok(Map.of("balls", balls));
    }
    
    // 使用精灵球领取宝可梦
    @PostMapping("/use-pokemon-ball")
    public ResponseEntity<?> usePokemonBall(@AuthenticationPrincipal AuthenticatedUser principal) {
        if (!principal.isStudent()) {
            return ResponseEntity.status(403).body(Map.of("error", "仅限学生使用"));
        }
        try {
            StudentPokemon pokemon = studentService.usePokemonBall(principal.studentId());
            // 返回时补全 image 路径
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("id", pokemon.getId());
            result.put("studentId", pokemon.getStudentId());
            result.put("pokedexId", pokemon.getPokedexId());
            result.put("name", pokemon.getName());
            String img = pokemon.getImage();
            // 防护：确保 image 不重复拼前缀
            if (img != null && !img.isEmpty()) {
                if (img.startsWith("/pokemon/") || img.startsWith("pokemon/")) {
                    result.put("image", img.startsWith("/") ? img : "/" + img);
                } else if (!img.startsWith("/")) {
                    result.put("image", "/pokemon/" + img);
                } else {
                    result.put("image", img);
                }
            } else {
                result.put("image", img);
            }
            result.put("types", pokemon.getTypes());
            result.put("food", pokemon.getFood());
            result.put("evolutionStage", pokemon.getEvolutionStage());
            result.put("formName", pokemon.getFormName());
            result.put("level", pokemon.getLevel());
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    // 获取进化选项（前端弹窗展示可选进化目标）
    @GetMapping("/pokemon/{pokemonId}/evolution-options")
    public ResponseEntity<?> getEvolutionOptions(
            @PathVariable String pokemonId,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        if (!principal.isStudent()) {
            return ResponseEntity.status(403).body(Map.of("error", "仅限学生访问"));
        }
        
        try {
            Map<String, Object> options = studentService.getEvolutionOptions(principal.studentId(), pokemonId);
            return ResponseEntity.ok(options);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    // 进化宝可梦（指定目标）
    @PostMapping("/pokemon/{pokemonId}/evolve")
    public ResponseEntity<?> evolvePokemon(
            @PathVariable String pokemonId,
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestBody(required = false) Map<String, Object> body) {
        if (!principal.isStudent()) {
            return ResponseEntity.status(403).body(Map.of("error", "仅限学生使用"));
        }
        
        Integer toPokedexId = null;
        String toFormName = null;
        if (body != null) {
            if (body.get("toPokedexId") != null) {
                toPokedexId = Integer.parseInt(body.get("toPokedexId").toString());
            }
            if (body.get("toFormName") != null) {
                toFormName = body.get("toFormName").toString();
            }
        }
        
        try {
            StudentPokemon pokemon = studentService.evolvePokemon(principal.studentId(), pokemonId, toPokedexId, toFormName);
            return ResponseEntity.ok(Map.of("success", true, "pokemon", pokemon));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    // 获取进化道具库存
    @GetMapping("/evolution-items")
    public ResponseEntity<?> getEvolutionItems(@AuthenticationPrincipal AuthenticatedUser principal) {
        if (!principal.isStudent()) {
            return ResponseEntity.status(403).body(Map.of("error", "仅限学生访问"));
        }
        Map<String, Integer> items = studentService.getEvolutionItems(principal.studentId());
        return ResponseEntity.ok(items);
    }
    
    // 使用进化道具让宝可梦进化
    @PostMapping("/pokemon/{pokemonId}/use-evolution-item")
    public ResponseEntity<?> useEvolutionItem(
            @PathVariable String pokemonId,
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestBody Map<String, String> body) {
        if (!principal.isStudent()) {
            return ResponseEntity.status(403).body(Map.of("error", "仅限学生使用"));
        }
        String itemKey = body.get("itemKey");
        if (itemKey == null || itemKey.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "道具名称不能为空"));
        }
        try {
            Map<String, Object> result = studentService.useEvolutionItem(principal.studentId(), pokemonId, itemKey);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 获取进化信息（兼容旧接口）
    @GetMapping("/pokemon/{pokemonId}/evolution-info")
    public ResponseEntity<?> getEvolutionInfo(
            @PathVariable String pokemonId,
            @AuthenticationPrincipal AuthenticatedUser principal) {
        if (!principal.isStudent()) {
            return ResponseEntity.status(403).body(Map.of("error", "仅限学生访问"));
        }
        
        try {
            Map<String, Object> info = studentService.getEvolutionOptions(principal.studentId(), pokemonId);
            return ResponseEntity.ok(info);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
