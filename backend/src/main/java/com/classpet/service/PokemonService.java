package com.classpet.service;

import com.classpet.dto.EvolutionRuleEntry;
import com.classpet.entity.StudentPokemon;
import com.classpet.repository.StudentPokemonRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PokemonService {
    
    @Autowired
    private StudentPokemonRepository studentPokemonRepository;
    
    // 进化规则: fromPokedexId -> List of next evolution options
    private Map<Integer, List<EvolutionRuleEntry>> evolutionRulesMap;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @PostConstruct
    public void init() throws IOException {
        // 加载进化规则（新Map格式）
        ClassPathResource resource = new ClassPathResource("data/evolution_rules.json");
        evolutionRulesMap = objectMapper.readValue(resource.getInputStream(), 
                new TypeReference<Map<Integer, List<EvolutionRuleEntry>>>() {});
    }
    
    /**
     * 获取指定宝可梦的进化选项
     */
    public List<EvolutionRuleEntry> getEvolutionOptions(Integer pokedexId, Integer currentStage) {
        if (evolutionRulesMap == null) return Collections.emptyList();
        List<EvolutionRuleEntry> options = evolutionRulesMap.get(pokedexId);
        if (options == null) return Collections.emptyList();
        return options.stream()
                .filter(r -> r.getFromStage() != null && r.getFromStage().equals(currentStage))
                .collect(Collectors.toList());
    }
    
    /**
     * 检查宝可梦是否可以进化
     */
    public boolean canEvolve(StudentPokemon pokemon) {
        return !getEvolutionOptions(pokemon.getPokedexId(), pokemon.getEvolutionStage()).isEmpty();
    }
    
    /**
     * 获取所有进化规则（转回列表格式）
     */
    public List<Map<String, Object>> getAllEvolutionRules() {
        List<Map<String, Object>> result = new ArrayList<>();
        if (evolutionRulesMap == null) return result;
        for (Map.Entry<Integer, List<EvolutionRuleEntry>> entry : evolutionRulesMap.entrySet()) {
            for (EvolutionRuleEntry rule : entry.getValue()) {
                Map<String, Object> r = new HashMap<>();
                r.put("fromPokedexId", entry.getKey());
                r.put("toPokedexId", rule.getToPokedexId());
                r.put("toName", rule.getToName());
                r.put("condition", rule.getCondition());
                r.put("toFormName", rule.getToFormName());
                r.put("fromStage", rule.getFromStage());
                r.put("toStage", rule.getToStage());
                result.add(r);
            }
        }
        return result;
    }
    
    /**
     * 喂食宝可梦
     */
    @Transactional
    public StudentPokemon feedPokemon(String pokemonId, String studentId, int foodAmount) {
        Optional<StudentPokemon> optPokemon = studentPokemonRepository.findById(pokemonId);
        if (optPokemon.isEmpty()) {
            throw new IllegalArgumentException("找不到该宝可梦");
        }
        
        StudentPokemon pokemon = optPokemon.get();
        if (!pokemon.getStudentId().equals(studentId)) {
            throw new IllegalArgumentException("无权操作该宝可梦");
        }
        
        pokemon.setFood(pokemon.getFood() + foodAmount);
        return studentPokemonRepository.save(pokemon);
    }
    
    /**
     * 获取学生的所有宝可梦及其进化状态
     */
    public List<Map<String, Object>> getStudentPokemonWithEvolutionInfo(String studentId) {
        List<StudentPokemon> pokemons = studentPokemonRepository.findByStudentId(studentId);
        
        return pokemons.stream().map(pokemon -> {
            Map<String, Object> info = new HashMap<>();
            info.put("id", pokemon.getId());
            info.put("pokedexId", pokemon.getPokedexId());
            info.put("name", pokemon.getName());
            info.put("image", pokemon.getImage());
            info.put("types", pokemon.getTypes());
            info.put("food", pokemon.getFood());
            info.put("level", pokemon.getLevel());
            info.put("evolutionStage", pokemon.getEvolutionStage());
            info.put("formName", pokemon.getFormName());
            info.put("createdAt", pokemon.getCreatedAt());
            info.put("updatedAt", pokemon.getUpdatedAt());
            
            List<EvolutionRuleEntry> options = getEvolutionOptions(pokemon.getPokedexId(), pokemon.getEvolutionStage());
            if (!options.isEmpty()) {
                info.put("canEvolve", true);
                info.put("evolutionOptionCount", options.size());
                
                // 返回第一个选项的简要信息（完整列表通过专门API获取）
                EvolutionRuleEntry first = options.get(0);
                info.put("nextEvolution", Map.of(
                    "name", first.getToName() != null ? first.getToName() : "未知",
                    "pokedexId", first.getToPokedexId(),
                    "condition", first.getCondition() != null ? first.getCondition() : "满足条件后进化",
                    "toFormName", first.getToFormName() != null ? first.getToFormName() : "",
                    "optionCount", options.size()
                ));
            } else {
                info.put("canEvolve", false);
                info.put("nextEvolution", null);
            }
            
            return info;
        }).collect(Collectors.toList());
    }
}
