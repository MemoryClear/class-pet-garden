package com.classpet.service;

import com.classpet.dto.EvolutionOptionDto;
import com.classpet.dto.EvolutionRuleEntry;
import com.classpet.entity.ScoreItem;
import com.classpet.entity.ScoreHistory;
import com.classpet.entity.Student;
import com.classpet.repository.ScoreHistoryRepository;
import com.classpet.repository.ScoreItemRepository;
import com.classpet.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.classpet.entity.ClassroomPokemonPool;
import com.classpet.repository.ClassroomPokemonPoolRepository;
import java.util.Random;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.classpet.entity.ExchangeRecord;
import com.classpet.entity.ShopItem;
import com.classpet.entity.StudentPokemon;
import com.classpet.repository.ShopItemRepository;
import com.classpet.repository.ExchangeRecordRepository;
import com.classpet.repository.StudentPokemonRepository;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.regex.*;

@Service
public class StudentService {
    private static final Logger logger = LoggerFactory.getLogger(StudentService.class);

    @Autowired private StudentRepository studentRepository;

    @Autowired
    private ClassroomPokemonPoolRepository classroomPokemonPoolRepository;
    @Autowired private ScoreItemRepository scoreItemRepository;
    @Autowired private ScoreHistoryRepository scoreHistoryRepository;
    @Autowired private ShopItemRepository shopItemRepository;
    @Autowired private ExchangeRecordRepository exchangeRecordRepository;
    @Autowired private StudentPokemonRepository studentPokemonRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // 宝可梦物种数据缓存（所有形态）
    private List<Map<String, Object>> allSpeciesCache;
    
    // 进化规则缓存: fromPokedexId -> List of next evolution options
    private Map<Integer, List<EvolutionRuleEntry>> evolutionRulesMap;
    
    @PostConstruct
    public void loadEvolutionRules() {
        try {
            ClassPathResource resource = new ClassPathResource("data/evolution_rules.json");
            evolutionRulesMap = objectMapper.readValue(resource.getInputStream(), 
                    new TypeReference<Map<Integer, List<EvolutionRuleEntry>>>() {});
        } catch (IOException e) {
            evolutionRulesMap = new HashMap<>();
        }
    }
    
    // 加载所有宝可梦物种数据
    private List<Map<String, Object>> getAllSpecies() {
        if (allSpeciesCache != null) return allSpeciesCache;
        try {
            ClassPathResource resource = new ClassPathResource("data/species.json");
            allSpeciesCache = objectMapper.readValue(resource.getInputStream(), 
                    new TypeReference<List<Map<String, Object>>>() {});
        } catch (IOException e) {
            allSpeciesCache = new ArrayList<>();
        }
        return allSpeciesCache;
    }
    
    // 根据pokedexId和formName查找物种条目
    private Map<String, Object> findSpeciesEntry(Integer pokedexId, String formName) {
        for (Map<String, Object> s : getAllSpecies()) {
            Integer sid = s.get("pokedexId") != null ? ((Number) s.get("pokedexId")).intValue() : 0;
            Object sForm = s.get("formName");
            String sFormName = sForm != null ? sForm.toString() : null;
            if (sid.equals(pokedexId) && 
                ((formName == null && sFormName == null) || (formName != null && formName.equals(sFormName)))) {
                return s;
            }
        }
        return null;
    }
    
    // 获取指定宝可梦的下一进化阶段选项列表
    private List<EvolutionRuleEntry> getEvolutionOptions(Integer pokedexId, Integer currentStage) {
        if (evolutionRulesMap == null) return new ArrayList<>();
        List<EvolutionRuleEntry> options = evolutionRulesMap.get(pokedexId);
        if (options == null) return new ArrayList<>();
        return options.stream()
                .filter(r -> r.getFromStage() != null && r.getFromStage().equals(currentStage))
                .toList();
    }

    public List<Student> getStudents(String teacherId) {
        return studentRepository.findByTeacherIdOrderByCreatedAtAsc(teacherId);
    }

    @Transactional
    public Student createStudent(String name, String teacherId) {
        Student stu = new Student();
        stu.setName(name.trim());
        stu.setTeacherId(teacherId);
        stu.setStudentNo(generateStudentNo(teacherId));
        stu.setPetChangeCards(3); // 新学生初始3张宠物更换卡
        return studentRepository.save(stu);
    }

    @Transactional
    public List<Student> batchCreateStudents(List<String> names, String teacherId) {
        int counter = getNextStudentNoCounter(teacherId);
        List<Student> students = new ArrayList<>();
        for (String n : names) {
            Student s = new Student();
            s.setName(n.trim());
            s.setTeacherId(teacherId);
            s.setStudentNo(String.format("S%04d", counter++));
            s.setPetChangeCards(3); // 新学生初始3张宠物更换卡
            students.add(s);
        }
        return studentRepository.saveAll(students);
    }

    private String generateStudentNo(String teacherId) {
        int counter = getNextStudentNoCounter(teacherId);
        return String.format("S%04d", counter);
    }

    private int getNextStudentNoCounter(String teacherId) {
        List<Student> existing = studentRepository.findByTeacherIdOrderByCreatedAtAsc(teacherId);
        int max = 0;
        for (Student s : existing) {
            if (s.getStudentNo() != null && s.getStudentNo().startsWith("S")) {
                try {
                    int num = Integer.parseInt(s.getStudentNo().substring(1));
                    if (num > max) max = num;
                } catch (NumberFormatException ignored) {}
            }
        }
        return max + 1;
    }

    @Transactional
    public Student updateStudent(String id, String teacherId, String name, String studentNo) {
        Student stu = findByIdAndTeacherId(id, teacherId);
        if (name != null) stu.setName(name.trim());
        if (studentNo != null) stu.setStudentNo(studentNo.trim());
        return studentRepository.save(stu);
    }

    @Transactional
    public void deleteStudent(String id, String teacherId) {
        Student stu = findByIdAndTeacherId(id, teacherId);
        studentRepository.delete(stu);
    }

    @Transactional
    public Student adoptPet(String studentId, String teacherId, Integer petId, String petName, String petIcon) {
        Student stu = findByIdAndTeacherId(studentId, teacherId);
        
        // 如果已有宠物，需要消耗宠物更换卡
        if (stu.getPetId() != null) {
            int cards = stu.getPetChangeCards() != null ? stu.getPetChangeCards() : 0;
            if (cards <= 0) {
                throw new RuntimeException("请先购买宠物更换卡");
            }
            stu.setPetChangeCards(cards - 1);
        }
        
        stu.setPetId(petId);
        stu.setPetName(petName);
        stu.setPetIcon(petIcon);
        return studentRepository.save(stu);
    }

    @Transactional
    public List<Student> assignPetsRandomly(String teacherId) {
        List<Student> students = studentRepository.findByTeacherIdOrderByCreatedAtAsc(teacherId);
        int[][] petIds = {{1, 2}, {3, 4}, {5, 6}, {7, 8}, {9, 10}};
        for (int i = 0; i < students.size(); i++) {
            students.get(i).setPetId(petIds[i % petIds.length][i % 2]);
        }
        return studentRepository.saveAll(students);
    }

    @Transactional
    public Student applyScore(String studentId, String teacherId, String scoreItemId) {
        Student stu = findByIdAndTeacherId(studentId, teacherId);
        Optional<ScoreItem> item = scoreItemRepository.findById(scoreItemId);
        int delta = item.map(ScoreItem::getPoint).orElse(1);
        stu.setFood(Math.max(0, stu.getFood() + delta));
        studentRepository.save(stu);

        // 写入历史记录
        ScoreHistory history = new ScoreHistory(
            stu.getId(), stu.getName(),
            item.map(ScoreItem::getName).orElse("手动加分"),
            item.map(ScoreItem::getIcon).orElse("✏️"),
            delta, teacherId
        );
        scoreHistoryRepository.save(history);

        return stu;
    }

    public List<Student> getLeaderboard(String teacherId) {
        return studentRepository.findByTeacherIdOrderByFoodDesc(teacherId);
    }

    // 学生答题加分（无需教师身份验证，仅限课堂答题使用）
    @Transactional
    public Student addScoreForQuiz(String studentId, int points, String reason) {
        Student stu = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("未找到该学生"));
        stu.setFood(Math.max(0, stu.getFood() + points));
        studentRepository.save(stu);

        ScoreHistory history = new ScoreHistory(
            stu.getId(), stu.getName(),
            reason != null ? reason : "\uD83D\uDCDA 课堂答题",
            "\uD83D\uDCDA",
            points, stu.getTeacherId()
        );
        scoreHistoryRepository.save(history);
        return stu;
    }

    // 总积分排名：历史所有加分 - 历史所有扣分（不包括道具消耗）
    public List<Map<String, Object>> getTotalScoreLeaderboard(String teacherId) {
        List<Student> students = studentRepository.findByTeacherIdOrderByCreatedAtAsc(teacherId);
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (Student stu : students) {
            // 计算该学生的总积分：所有未撤销的历史记录的point之和
            List<ScoreHistory> histories = scoreHistoryRepository.findByStudentIdAndRevokedFalse(stu.getId());
            int totalScore = histories.stream().mapToInt(h -> h.getPoint() != null ? h.getPoint() : 0).sum();
            
            Map<String, Object> item = new HashMap<>();
            item.put("id", stu.getId());
            item.put("name", stu.getName());
            item.put("petIcon", stu.getPetIcon());
            item.put("petName", stu.getPetName());
            item.put("food", stu.getFood()); // 当前积分
            item.put("totalScore", totalScore); // 总积分
            item.put("equippedItems", stu.getEquippedItems());
            result.add(item);
        }
        
        // 按总积分降序排序
        result.sort((a, b) -> Integer.compare((Integer) b.get("totalScore"), (Integer) a.get("totalScore")));
        return result;
    }

    private Student findByIdAndTeacherId(String id, String teacherId) {
        return studentRepository.findByIdAndTeacherId(id, teacherId)
                .orElseThrow(() -> new IllegalArgumentException("未找到该学生"));
    }

    // ============== 装备装饰 ==============
    @Transactional
    public Student equipItem(String studentId, String teacherId, String itemId) {
        Student stu = findByIdAndTeacherId(studentId, teacherId);
        // 验证商品存在且属于该教师
        ShopItem item = shopItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("商品不存在"));
        if (!item.getTeacherId().equals(teacherId)) {
            throw new IllegalArgumentException("无权限");
        }
        // 解析当前装备列表
        List<String> equipped = parseEquippedList(stu.getEquippedItems());
        if (!equipped.contains(itemId)) {
            equipped.add(itemId);
            try { stu.setEquippedItems(objectMapper.writeValueAsString(equipped)); } 
            catch (JsonProcessingException e) { stu.setEquippedItems("[]"); }
        }
        return studentRepository.save(stu);
    }

    @Transactional
    public Student unequipItem(String studentId, String teacherId, String itemId) {
        Student stu = findByIdAndTeacherId(studentId, teacherId);
        List<String> equipped = parseEquippedList(stu.getEquippedItems());
        equipped.remove(itemId);
        try { stu.setEquippedItems(objectMapper.writeValueAsString(equipped)); }
        catch (JsonProcessingException e) { stu.setEquippedItems("[]"); }
        return studentRepository.save(stu);
    }

    public List<String> parseEquippedList(String json) {
        try {
            if (json == null || json.isBlank()) return new ArrayList<>();
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public List<Map<String, Object>> getPetLibrary() {
        return List.of(
            Map.of("id", 1, "icon", "🐱", "name", "小橘猫"),
            Map.of("id", 2, "icon", "🐶", "name", "柴犬"),
            Map.of("id", 3, "icon", "🐰", "name", "垂耳兔"),
            Map.of("id", 4, "icon", "🐹", "name", "金丝熊"),
            Map.of("id", 5, "icon", "🦊", "name", "小狐狸"),
            Map.of("id", 6, "icon", "🐼", "name", "熊猫"),
            Map.of("id", 7, "icon", "🦁", "name", "小狮子"),
            Map.of("id", 8, "icon", "🐯", "name", "小老虎"),
            Map.of("id", 9, "icon", "🐨", "name", "考拉"),
            Map.of("id", 10, "icon", "\uD83D\uDC2E", "name", "奶牛")
        );
    }

    // 宝可梦图鉴（从 species.json 加载，仅 tier=1 初始形态）
    public List<Map<String, Object>> getPokemonLibrary() {
        List<Map<String, Object>> allSpecies = getAllSpecies();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> s : allSpecies) {
            Integer tier = s.get("tier") != null ? ((Number) s.get("tier")).intValue() : 0;
            if (tier != 1) continue; // 只返回初始形态
            
            Map<String, Object> pokemon = new HashMap<>();
            pokemon.put("id", 100 + ((Number) s.get("pokedexId")).intValue());
            pokemon.put("pokedexId", s.get("pokedexId"));
            pokemon.put("name", s.get("name"));
            pokemon.put("types", s.get("types"));
            String img = s.get("image") != null ? s.get("image").toString() : null;
            pokemon.put("image", img != null ? "/pokemon/" + img : null);
            result.add(pokemon);
        }
        return result;
    }

    // 学生自助领养/更换宠物（无需更换卡，首次领养免费）
    @Transactional
    public Student studentAdoptPet(String studentId, String teacherId, Integer petId, String petName, String petIcon) {
        Student stu = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("学生不存在"));
        if (!stu.getTeacherId().equals(teacherId)) {
            throw new IllegalArgumentException("无权限");
        }
        // 如果已有宠物，需要消耗宠物更换卡
        if (stu.getPetId() != null) {
            int cards = stu.getPetChangeCards() != null ? stu.getPetChangeCards() : 0;
            if (cards <= 0) {
                throw new RuntimeException("请先购买宠物更换卡");
            }
            stu.setPetChangeCards(cards - 1);
        }
        stu.setPetId(petId);
        stu.setPetName(petName);
        stu.setPetIcon(petIcon);
        return studentRepository.save(stu);
    }

    // 学生自助兑换商品
    @Transactional
    public Map<String, Object> studentExchange(String studentId, String teacherId, String itemId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("学生不存在"));
        if (!student.getTeacherId().equals(teacherId)) {
            throw new IllegalArgumentException("无权限");
        }
        ShopItem item = shopItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("商品不存在"));
        if (!item.getTeacherId().equals(teacherId)) {
            throw new IllegalArgumentException("无权限");
        }
        int food = student.getFood() != null ? student.getFood() : 0;
        if (food < item.getPrice()) {
            throw new IllegalArgumentException("积分不足，需要 " + item.getPrice() + " 分");
        }
        student.setFood(food - item.getPrice());

        if ("pet_change_card".equals(item.getItemType()) || "petCard".equals(item.getItemType())) {
            int cards = student.getPetChangeCards() != null ? student.getPetChangeCards() : 0;
            student.setPetChangeCards(cards + 1);
            ScoreHistory history = new ScoreHistory(student.getId(), student.getName(),
                item.getIcon() + " 购买「" + item.getName() + "」",
                item.getIcon(), -item.getPrice(), teacherId);
            scoreHistoryRepository.save(history);
            studentRepository.save(student);
            return Map.of("success", true, "food", student.getFood(), "petChangeCards", student.getPetChangeCards());
        }

        // 精灵球：直接加数量（不创建兑换记录）
        if ("pokemon_ball".equals(item.getItemType()) || "POKEBALL".equals(item.getItemType())) {
            int balls = student.getPokemonBalls() != null ? student.getPokemonBalls() : 0;
            student.setPokemonBalls(balls + 1);
            ScoreHistory history = new ScoreHistory(student.getId(), student.getName(),
                item.getIcon() + " 购买「" + item.getName() + "」",
                item.getIcon(), -item.getPrice(), teacherId);
            scoreHistoryRepository.save(history);
            studentRepository.save(student);
            return Map.of("success", true, "food", student.getFood(), "pokemonBalls", student.getPokemonBalls());
        }

        // 进化道具：添加到学生库存
        if ("evolution_item".equals(item.getItemType())) {
            String itemKey = item.getEvolutionItemKey() != null ? item.getEvolutionItemKey() : item.getName();
            try {
                Map<String, Integer> items = objectMapper.readValue(
                    student.getEvolutionItems() != null ? student.getEvolutionItems() : "{}",
                    new TypeReference<Map<String, Integer>>() {});
                items.put(itemKey, items.getOrDefault(itemKey, 0) + 1);
                student.setEvolutionItems(objectMapper.writeValueAsString(items));
            } catch (Exception e) {
                student.setEvolutionItems("{\"" + itemKey + "\":1}");
            }
            ScoreHistory history = new ScoreHistory(student.getId(), student.getName(),
                item.getIcon() + " 购买「" + item.getName() + "」",
                item.getIcon(), -item.getPrice(), teacherId);
            scoreHistoryRepository.save(history);
            studentRepository.save(student);
            try {
                Map<String, Integer> updatedItems = objectMapper.readValue(student.getEvolutionItems(), new TypeReference<Map<String, Integer>>() {});
                return Map.of("success", true, "food", student.getFood(), "evolutionItems", updatedItems);
            } catch (Exception e) {
                return Map.of("success", true, "food", student.getFood());
            }
        }

        ExchangeRecord record = new ExchangeRecord();
        record.setStudentId(student.getId());
        record.setStudentName(student.getName());
        record.setItemId(item.getId());
        record.setItemName(item.getName());
        record.setItemIcon(item.getIcon());
        record.setFoodSpent(item.getPrice());
        record.setTeacherId(teacherId);
        exchangeRecordRepository.save(record);

        ScoreHistory history = new ScoreHistory(student.getId(), student.getName(),
            item.getIcon() + " 购买「" + item.getName() + "」",
            item.getIcon(), -item.getPrice(), teacherId);
        scoreHistoryRepository.save(history);
        studentRepository.save(student);
        return Map.of("success", true, "food", student.getFood());
    }

    // ==================== 宝可梦相关方法 ====================

    // ============== 进化工具方法 ==============

    // 从 condition 字符串中解析等级要求（如 "等级16以上" → 16）
    private int parseLevelFromCondition(String condition) {
        if (condition == null) return 0;
        // 匹配 "等级16以上" / "等级16" / "等级 16" 等格式
        Pattern p = Pattern.compile("等级\\s*(\\d+)");
        Matcher m = p.matcher(condition);
        if (m.find()) {
            return Integer.parseInt(m.group(1));
        }
        return 0; // 非等级条件（如道具/月石等）返回0，表示无条件
    }

    // ==================== 进化辅助方法 ====================
    
    // 计算某宝可梦到最终形态所需的最大食物（递归所有进化路径取最大值）
    private int calcMaxFoodToFinal(Integer pokedexId, Integer currentStage, int foodSoFar) {
        List<EvolutionRuleEntry> opts = getEvolutionOptions(pokedexId, currentStage);
        if (opts == null || opts.isEmpty()) {
            return foodSoFar + (currentStage == 0 ? 50 : (currentStage == 1 ? 150 : 300)); // 无进化选项，已是最终形态，按阶段设默认值
        }
        // 找等级进化选项（排除Mega/极巨化 + 排除道具/通讯交换）
        List<EvolutionRuleEntry> levelOpts = opts.stream()
                .filter(o -> o.getToFormName() == null)  // 排除 Mega/极巨化
                .filter(o -> isLevelBasedCondition(o.getCondition()))  // 只保留等级进化
                .toList();
        if (levelOpts.isEmpty()) {
            // 无等级进化选项，但可能有道具进化（如 Staryu 用水之石进化）
            // 道具有进化，给当前阶段设一个合理的最小食物上限
            int stageFoodCap = (currentStage == 0 ? 50 : (currentStage == 1 ? 150 : 300));
            return foodSoFar + stageFoodCap;
        }
        // 收集每个进化路径的最大食物
        int maxFood = foodSoFar;
        for (EvolutionRuleEntry opt : levelOpts) {
            int reqLevel = parseLevelFromCondition(opt.getCondition());
            int foodForThisPath = foodSoFar + (reqLevel > 0 ? reqLevel * 10 : 0);
            int nextStage = opt.getToStage() != null ? opt.getToStage() : currentStage + 1;
            maxFood = Math.max(maxFood, calcMaxFoodToFinal(opt.getToPokedexId(), nextStage, foodForThisPath));
        }
        return maxFood;
    }
    
    // 判断某宝可梦是否是最终形态（没有任何可用的进化路径）
    private boolean isFinalForm(Integer pokedexId, Integer currentStage) {
        List<EvolutionRuleEntry> opts = getEvolutionOptions(pokedexId, currentStage);
        if (opts == null || opts.isEmpty()) return true;
        // 过滤出已实现的进化选项（implemented != false）
        List<EvolutionRuleEntry> implementedOpts = opts.stream()
            .filter(o -> o.getImplemented() == null || !Boolean.FALSE.equals(o.getImplemented()))
            .toList();
        // 如果没有已实现的进化选项，就是最终形态
        if (implementedOpts.isEmpty()) return true;
        // 只要有一个 toFormName == null 的已实现进化选项，就不是最终形态
        return implementedOpts.stream().noneMatch(o -> o.getToFormName() == null);
    }
    
    // 判断条件是否为等级进化（而非道具/通讯交换等）
    private boolean isLevelBasedCondition(String condition) {
        if (condition == null) return false;
        return condition.contains("等级");
    }
    
    // 从进化条件中提取道具名称（如"使用水之石" -> "水之石"）
    private String extractItemName(String condition) {
        if (condition == null) return null;
        String c = condition.trim();
        if (c.startsWith("使用")) {
            return c.substring(2).trim();
        }
        return null;
    }
    
    // 随机选一个非Mega/极巨化的进化选项（用于展示和伊布等多路径场景）
    private EvolutionRuleEntry pickRandomOption(List<EvolutionRuleEntry> options) {
        List<EvolutionRuleEntry> levelOpts = options.stream()
                .filter(o -> o.getToFormName() == null)
                .toList();
        if (levelOpts.isEmpty()) return null;
        return levelOpts.get(new Random().nextInt(levelOpts.size()));
    }

    // 为单个宝可梦计算进化信息，返回带 canEvolve/nextEvolution/maxFood/isFinalForm 字段的 Map
    private Map<String, Object> enrichPokemonWithEvolution(StudentPokemon pokemon) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", pokemon.getId());
        map.put("studentId", pokemon.getStudentId());
        map.put("pokedexId", pokemon.getPokedexId());
        map.put("name", pokemon.getName());
        String img = pokemon.getImage();
        // 防护：确保 image 不重复拼前缀
        if (img != null && !img.isEmpty()) {
            if (img.startsWith("/pokemon/") || img.startsWith("pokemon/")) {
                map.put("image", img.startsWith("/") ? img : "/" + img);
            } else if (!img.startsWith("/")) {
                map.put("image", "/pokemon/" + img);
            } else {
                map.put("image", img);
            }
        } else {
            map.put("image", img);
        }
        map.put("types", pokemon.getTypes());
        map.put("food", pokemon.getFood());
        map.put("evolutionStage", pokemon.getEvolutionStage());
        map.put("formName", pokemon.getFormName());
        map.put("level", pokemon.getLevel());
        map.put("createdAt", pokemon.getCreatedAt());
        map.put("updatedAt", pokemon.getUpdatedAt());
        
        // 阶段食物上限（当前阶段的最大食物）：用于喂养限制和进度条显示
        int stageFoodCap = (pokemon.getEvolutionStage() == 0 ? 50 : (pokemon.getEvolutionStage() == 1 ? 150 : 300));
        int currentStageFoodCap = stageFoodCap; // 当前阶段的实际cap
        
        // 计算 maxFood（进化到最终形态所需食物上限）
        int currentFood = pokemon.getFood() != null ? pokemon.getFood() : 0;
        int maxFood = calcMaxFoodToFinal(pokemon.getPokedexId(), pokemon.getEvolutionStage(), 0);
        // 修复 maxFood=0 的情况（道具进化/无进化规则），用阶段默认值
        if (maxFood == 0) {
            maxFood = currentStageFoodCap;
        }
        
        map.put("stageFoodCap", currentStageFoodCap);
        map.put("maxFood", maxFood);
        map.put("food", currentFood);

        // 判断进化类型（必须在 calcMaxFoodToFinal 之后，因为后续代码依赖 evolutionType）
        List<EvolutionRuleEntry> optsForType = getEvolutionOptions(pokemon.getPokedexId(), pokemon.getEvolutionStage());
        String evolutionType;
        if (optsForType == null || optsForType.isEmpty()) {
            evolutionType = "none";
        } else if (optsForType.stream().anyMatch(o -> o.getToFormName() == null && isLevelBasedCondition(o.getCondition()))) {
            evolutionType = "level";
        } else {
            evolutionType = "item";
        }
        map.put("evolutionType", evolutionType);
        
        // 判断是否最终形态（用于前端显示）
        boolean finalForm = isFinalForm(pokemon.getPokedexId(), pokemon.getEvolutionStage());
        // 如果道具进化中至少有1个是已实现的，才不算最终形态
        if ("item".equals(evolutionType) && map.containsKey("itemEvolutions")) {
            Object rawItemEvos = map.get("itemEvolutions");
            if (rawItemEvos instanceof List) {
                List<?> itemEvos = (List<?>) rawItemEvos;
                boolean hasImplemented = itemEvos.stream().anyMatch(o -> {
                    if (o instanceof Map) {
                        Object imp = ((Map<?, ?>) o).get("implemented");
                        return imp == null || !Boolean.FALSE.equals(imp);
                    }
                    return false;
                });
                if (hasImplemented) finalForm = false;
            }
        }
        map.put("isFinalForm", finalForm);
        List<EvolutionRuleEntry> options = getEvolutionOptions(pokemon.getPokedexId(), pokemon.getEvolutionStage());
        // 标记：是否所有进化选项都未实现
        boolean allUnimplemented = options != null && !options.isEmpty()
            && options.stream().allMatch(o -> Boolean.FALSE.equals(o.getImplemented()));
        map.put("allEvolutionsUnimplemented", allUnimplemented);
        // 单阶段宝可梦（无任何进化规则）直接返回
        if ("none".equals(evolutionType)) {
            map.put("canEvolve", false);
            map.put("nextEvolution", null);
            map.put("evolutionHint", null);
            map.put("itemEvolutions", Collections.emptyList());
            return map;
        }
        // 道具进化宝可梦：不设置 nextEvolution，避免进度条显示 x/0
        // itemEvolutions 字段已设置，前端正进度条不显示
        if ("item".equals(evolutionType)) {
            map.put("nextEvolution", null);
            // 构建 itemEvolutions 显示给前端（包含所有非等级进化的选项，含Mega/极巨化形态）
            List<Map<String, Object>> itemEvoList = options.stream()
                    .filter(o -> !isLevelBasedCondition(o.getCondition()))
                    .map(o -> {
                        String itemKey = extractItemName(o.getCondition());
                        Map<String, Object> m = new HashMap<>();
                        m.put("toPokedexId", o.getToPokedexId());
                        // 优先用toFormName作显示名（如“超级水箭龟”），否则用toName
                        String displayName = (o.getToFormName() != null && !o.getToFormName().isEmpty()) ? o.getToFormName() : o.getToName();
                        m.put("toName", displayName);
                        m.put("toFormName", o.getToFormName());
                        m.put("condition", o.getCondition());
                        // 如果能提取出道具名就显示道具名，否则显示原始条件文本
                        List<String> reqItems = new ArrayList<>();
                        if (itemKey != null) {
                            reqItems.add(itemKey);
                        } else {
                            reqItems.add(o.getCondition());
                        }
                        m.put("requiredItems", reqItems);
                        m.put("implemented", o.getImplemented());
                        return m;
                    })
                    .toList();
            map.put("itemEvolutions", itemEvoList);
        } else {
            int currentLevel = pokemon.getLevel();
            // 找满足等级要求的进化选项（仅等级进化，排除道具/通讯交换等非食物进化）
            List<EvolutionRuleEntry> canEvolveOpts = options.stream()
                    .filter(o -> o.getToFormName() == null)
                    .filter(o -> {
                        int req = parseLevelFromCondition(o.getCondition());
                        return req > 0 && currentLevel >= req; // 必须有等级要求且当前等级满足
                    })
                    .toList();

            if (!canEvolveOpts.isEmpty()) {
                // 可进化，随机选一个作为 nextEvolution
                EvolutionRuleEntry chosen = canEvolveOpts.get(new Random().nextInt(canEvolveOpts.size()));
                Map<String, Object> species = findSpeciesEntry(chosen.getToPokedexId(), chosen.getToFormName());
                String toImage = buildToImage(species);

                Map<String, Object> nextEvo = new HashMap<>();
                nextEvo.put("name", chosen.getToName());
                nextEvo.put("image", toImage);
                nextEvo.put("foodRequired", currentStageFoodCap);
                nextEvo.put("progress", 0);
                nextEvo.put("toPokedexId", chosen.getToPokedexId());
                nextEvo.put("toFormName", chosen.getToFormName());

                map.put("canEvolve", true);
                map.put("nextEvolution", nextEvo);
            } else {
                // 不可进化但非最终形态
                EvolutionRuleEntry nextOpt = pickRandomOption(options);
                if (nextOpt != null) {
                    Map<String, Object> species = findSpeciesEntry(nextOpt.getToPokedexId(), nextOpt.getToFormName());
                    String toImage = buildToImage(species);

                    Map<String, Object> nextEvo = new HashMap<>();
                    nextEvo.put("name", nextOpt.getToName());
                    nextEvo.put("image", toImage);
                    nextEvo.put("condition", nextOpt.getCondition());
                    nextEvo.put("toPokedexId", nextOpt.getToPokedexId());
                    nextEvo.put("toFormName", nextOpt.getToFormName());

                    if (isLevelBasedCondition(nextOpt.getCondition())) {
                        // 等级进化：显示进度条
                        int reqLevel = parseLevelFromCondition(nextOpt.getCondition());
                        int foodRequired = reqLevel * 10;
                        int progress = foodRequired > 0 ? Math.min(100, currentFood * 100 / foodRequired) : 0;
                        nextEvo.put("foodRequired", foodRequired);
                        nextEvo.put("progress", progress);
                    } else {
                        // 道具有进化：设置 itemEvolutions 供前端显示进化道具按钮
                        nextEvo.put("foodRequired", 0);
                        nextEvo.put("progress", -1);
                        List<Map<String, Object>> itemEvoList = options.stream()
                                .filter(o -> !isLevelBasedCondition(o.getCondition()))
                                .map(o -> {
                                    String itemKey = extractItemName(o.getCondition());
                                    Map<String, Object> m = new HashMap<>();
                                    m.put("toPokedexId", o.getToPokedexId());
                                    // 优先用toFormName作显示名（如“超级水箭龟”），否则用toName
                                    String displayName = (o.getToFormName() != null && !o.getToFormName().isEmpty()) ? o.getToFormName() : o.getToName();
                                    m.put("toName", displayName);
                                    m.put("toFormName", o.getToFormName());
                                    m.put("condition", o.getCondition());
                                    // 如果能提取出道具名就显示道具名，否则显示原始条件文本
                                    List<String> reqItems = new ArrayList<>();
                                    if (itemKey != null) {
                                        reqItems.add(itemKey);
                                    } else {
                                        reqItems.add(o.getCondition());
                                    }
                                    m.put("requiredItems", reqItems);
                                    return m;
                                })
                                .toList();
                        map.put("itemEvolutions", itemEvoList);
                    }

                    map.put("canEvolve", false);
                    map.put("nextEvolution", nextEvo);
                } else {
                    map.put("canEvolve", false);
                    map.put("nextEvolution", null);
                }
            }
        }

        return map;
    }
    
    // 辅助：构建进化目标图片路径
    private String buildToImage(Map<String, Object> species) {
        if (species == null) return null;
        String sImg = species.get("image") != null ? species.get("image").toString() : null;
        return (sImg != null && !sImg.isEmpty()) ? "/pokemon/" + sImg : null;
    }

    // 获取学生的宝可梦列表（返回带 canEvolve/nextEvolution/livel 计算字段）
    public List<Map<String, Object>> getStudentPokemon(String studentId) {
        List<StudentPokemon> list = studentPokemonRepository.findByStudentId(studentId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (StudentPokemon p : list) {
            try {
                Map<String, Object> map = enrichPokemonWithEvolution(p);
                result.add(map);
            } catch (Exception e) {
                logger.error("Error processing pokemon: " + e.getMessage(), e);
            }
        }
        return result;
    }

    // 领养宝可梦（无上限，直接新增）
    @Transactional
    public StudentPokemon adoptPokemon(String studentId, Integer pokedexId, String name, String image, String types) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("学生不存在"));
        
        String img = image;
        if (img != null) {
            if (img.startsWith("/pokemon/")) img = img.substring("/pokemon/".length());
            else if (img.startsWith("pokemon/")) img = img.substring("pokemon/".length());
        }
        StudentPokemon pokemon = new StudentPokemon(studentId, pokedexId, name, img, types, null);
        return studentPokemonRepository.save(pokemon);
    }

    // 喂养宝可梦（给指定宝可梦加分，同时从学生账户扣除积分）
    @Transactional
    public StudentPokemon feedPokemon(String studentId, String pokemonId, int points) {
        // 1. 检查学生积分是否足够
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("学生不存在"));
        int studentFood = student.getFood() != null ? student.getFood() : 0;
        if (studentFood < points) {
            throw new IllegalArgumentException("积分不足，当前积分: " + studentFood + "，需要: " + points);
        }
        
        // 2. 验证宝可梦存在且属于该学生
        StudentPokemon pokemon = studentPokemonRepository.findById(pokemonId)
                .orElseThrow(() -> new IllegalArgumentException("宝可梦不存在"));
        if (!pokemon.getStudentId().equals(studentId)) {
            throw new IllegalArgumentException("无权限操作该宝可梦");
        }
        
        // 2.5 检查是否已达当前阶段食物上限（而非仅判断是否最终形态）
        Map<String, Object> enriched = enrichPokemonWithEvolution(pokemon);
        boolean isFinalForm = Boolean.TRUE.equals(enriched.get("isFinalForm"));
        int currentFood = pokemon.getFood() != null ? pokemon.getFood() : 0;
        // maxFood 可能为 null，加 null 安全检查
        Object maxFoodObj = enriched.get("maxFood");
        int maxFood = (maxFoodObj instanceof Number) ? ((Number) maxFoodObj).intValue() : 0;
        // 兜底：非最终形态但 maxFood=0（道具进化等无等级条件的进化）用阶段默认值
        if (!isFinalForm && maxFood == 0) {
            int stageFoodCap = (pokemon.getEvolutionStage() == 0 ? 50 : (pokemon.getEvolutionStage() == 1 ? 150 : 300));
            maxFood = stageFoodCap;
        }
        if (isFinalForm) {
            // 最终形态：cap = 前一进化阶段的最大食物（由 calcMaxFoodToFinal 递归计算得到）
            int prevMaxFood = calcMaxFoodToFinal(pokemon.getPokedexId(), pokemon.getEvolutionStage(), 0);
            if (prevMaxFood > 0 && currentFood >= prevMaxFood) {
                throw new IllegalArgumentException("该宝可梦已达当前阶段食物上限，无法继续投喂");
            }
        } else {
            // 中间形态：cap = calcMaxFoodToFinal 返回的总食物上限
            if (maxFood > 0 && currentFood >= maxFood) {
                throw new IllegalArgumentException("该宝可梦已达当前阶段食物上限，无法继续投喂");
            }
        }
        
        // 3. 从学生扣除积分
        student.setFood(studentFood - points);
        studentRepository.save(student);
        
        // 4. 给宝可梦增加食物/积分
        pokemon.setFood((pokemon.getFood() != null ? pokemon.getFood() : 0) + points);
        return studentPokemonRepository.save(pokemon);
    }

    // 获取学生宝可梦数量
    public long getPokemonCount(String studentId) {
        return studentPokemonRepository.findByStudentId(studentId).size();
    }
    
    // 获取宝可梦的进化选项列表
    public Map<String, Object> getEvolutionOptions(String studentId, String pokemonId) {
        StudentPokemon pokemon = studentPokemonRepository.findById(pokemonId)
                .orElseThrow(() -> new IllegalArgumentException("宝可梦不存在"));
        
        if (!pokemon.getStudentId().equals(studentId)) {
            throw new IllegalArgumentException("无权限操作该宝可梦");
        }
        
        List<EvolutionRuleEntry> options = getEvolutionOptions(pokemon.getPokedexId(), pokemon.getEvolutionStage());
        if (options == null || options.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("canEvolve", false);
            result.put("message", "该宝可梦已无法继续进化");
            result.put("options", new ArrayList<>());
            result.put("currentFood", pokemon.getFood());
            result.put("level", pokemon.getLevel());
            return result;
        }
        
        // 转换选项，补充图片和类型信息
        List<EvolutionOptionDto> optionDtos = new ArrayList<>();
        for (EvolutionRuleEntry entry : options) {
            Map<String, Object> species = findSpeciesEntry(entry.getToPokedexId(), entry.getToFormName());
            String toImage = null;
            String toTypes = null;
            if (species != null) {
                String img = species.get("image") != null ? species.get("image").toString() : null;
                toImage = img != null ? "/pokemon/" + img : null;
                Object typesObj = species.get("types");
                if (typesObj instanceof java.util.List) {
                    toTypes = typesObj.toString();
                }
            }
            
            optionDtos.add(new EvolutionOptionDto(
                    entry.getToPokedexId(),
                    entry.getToName(),
                    toImage,
                    toTypes,
                    entry.getCondition(),
                    entry.getToFormName(),
                    entry.getToStage()
            ));
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("canEvolve", true);
        result.put("options", optionDtos);
        result.put("optionCount", optionDtos.size());
        result.put("currentFood", pokemon.getFood());
        result.put("level", pokemon.getLevel());
        result.put("currentName", pokemon.getName());
        result.put("currentFormName", pokemon.getFormName());
        return result;
    }
    
    // 进化宝可梦（指定目标）
    @Transactional
    public StudentPokemon evolvePokemon(String studentId, String pokemonId, Integer toPokedexId, String toFormName) {
        StudentPokemon pokemon = studentPokemonRepository.findById(pokemonId)
                .orElseThrow(() -> new IllegalArgumentException("宝可梦不存在"));
        
        if (!pokemon.getStudentId().equals(studentId)) {
            throw new IllegalArgumentException("无权限操作该宝可梦");
        }
        
        // 获取可选的进化选项
        List<EvolutionRuleEntry> options = getEvolutionOptions(pokemon.getPokedexId(), pokemon.getEvolutionStage());
        if (options == null || options.isEmpty()) {
            throw new IllegalArgumentException("该宝可梦无法进化");
        }
        
        // 找到指定的进化目标
        EvolutionRuleEntry target = null;
        for (EvolutionRuleEntry opt : options) {
            if (opt.getToPokedexId().equals(toPokedexId)) {
                if (toFormName == null) {
                    if (opt.getToFormName() == null) {
                        target = opt;
                        break;
                    }
                } else if (toFormName.equals(opt.getToFormName())) {
                    target = opt;
                    break;
                }
            }
        }
        
        if (target == null) {
            throw new IllegalArgumentException("无效的进化目标");
        }
        
        // 获取进化后的物种数据
        Map<String, Object> species = findSpeciesEntry(target.getToPokedexId(), target.getToFormName());
        if (species == null) {
            throw new IllegalArgumentException("无法获取进化后的宝可梦数据");
        }
        
        // 执行进化
        pokemon.setPokedexId(target.getToPokedexId());
        pokemon.setName(target.getToName());
        String img = species.get("image") != null ? species.get("image").toString() : null;
        pokemon.setImage(img != null ? img : pokemon.getImage());
        Object typesObj = species.get("types");
        if (typesObj instanceof java.util.List) {
            pokemon.setTypes(typesObj.toString());
        }
        pokemon.setFormName(target.getToFormName());
        pokemon.setEvolutionStage(target.getToStage() != null ? target.getToStage() : pokemon.getEvolutionStage() + 1);
        
        logger.info("学生 {} 的宝可梦 {} 进化为 {} ({})", studentId, pokemon.getName(), target.getToName(), target.getToPokedexId());
        
        return studentPokemonRepository.save(pokemon);
    }
    
    // 使用进化道具让宝可梦进化
    @Transactional
    public Map<String, Object> useEvolutionItem(String studentId, String pokemonId, String itemKey) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("学生不存在"));
        StudentPokemon pokemon = studentPokemonRepository.findById(pokemonId)
                .orElseThrow(() -> new IllegalArgumentException("宝可梦不存在"));
        
        if (!pokemon.getStudentId().equals(studentId)) {
            throw new IllegalArgumentException("无权限操作该宝可梦");
        }
        
        // 检查学生是否拥有该道具
        Map<String, Integer> ownedItems;
        try {
            ownedItems = objectMapper.readValue(
                student.getEvolutionItems() != null ? student.getEvolutionItems() : "{}",
                new TypeReference<Map<String, Integer>>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("道具数据异常");
        }
        
        if (!ownedItems.containsKey(itemKey) || ownedItems.getOrDefault(itemKey, 0) <= 0) {
            throw new IllegalArgumentException("你没有「" + itemKey + "」，请先去小卖部购买");
        }
        
        // 检查食物是否达到当前阶段的上限
        int currentFood = pokemon.getFood() != null ? pokemon.getFood() : 0;
        int maxFood = calcMaxFoodToFinal(pokemon.getPokedexId(), pokemon.getEvolutionStage(), 0);
        if (currentFood < maxFood) {
            throw new IllegalArgumentException("宝可梦食物不足，需要达到 " + maxFood + " 才能进化，请先投喂食物");
        }
        
        // 获取进化选项，找到需要该道具的进化路径
        List<EvolutionRuleEntry> options = getEvolutionOptions(pokemon.getPokedexId(), pokemon.getEvolutionStage());
        if (options == null || options.isEmpty()) {
            throw new IllegalArgumentException("该宝可梦无法进化");
        }
        
        // 从 condition 字段提取道具名称，匹配 itemKey
        List<EvolutionRuleEntry> matchingOpts = options.stream()
                .filter(o -> o.getToFormName() == null) // 排除 Mega/极巨化
                .filter(o -> {
                    String cond = o.getCondition();
                    if (cond == null) return false;
                    // condition 格式如 "使用水之石" 或 "使用火之石" 或 "连接交换 ；使用联系绳"
                    return cond.contains(itemKey);
                })
                .toList();
        
        if (matchingOpts.isEmpty()) {
            throw new IllegalArgumentException("「" + itemKey + "」无法让 " + pokemon.getName() + " 进化");
        }
        
        // 如果有多个匹配（如伊布+水之石），随机选一个
        EvolutionRuleEntry target = matchingOpts.get(new Random().nextInt(matchingOpts.size()));
        
        // 获取进化后的物种数据
        Map<String, Object> species = findSpeciesEntry(target.getToPokedexId(), target.getToFormName());
        if (species == null) {
            throw new IllegalArgumentException("无法获取进化后的宝可梦数据");
        }
        
        // 消耗道具
        ownedItems.put(itemKey, ownedItems.get(itemKey) - 1);
        if (ownedItems.get(itemKey) <= 0) {
            ownedItems.remove(itemKey);
        }
        try {
            student.setEvolutionItems(objectMapper.writeValueAsString(ownedItems));
        } catch (Exception e) {
            student.setEvolutionItems("{}");
        }
        studentRepository.save(student);
        
        // 执行进化
        pokemon.setPokedexId(target.getToPokedexId());
        pokemon.setName(target.getToName());
        String img = species.get("image") != null ? species.get("image").toString() : null;
        pokemon.setImage(img != null ? img : pokemon.getImage());
        Object typesObj = species.get("types");
        if (typesObj instanceof java.util.List) {
            pokemon.setTypes(typesObj.toString());
        }
        pokemon.setFormName(target.getToFormName());
        pokemon.setEvolutionStage(target.getToStage() != null ? target.getToStage() : pokemon.getEvolutionStage() + 1);
        
        StudentPokemon saved = studentPokemonRepository.save(pokemon);
        logger.info("学生 {} 使用 {} 让 {} 进化为 {}", studentId, itemKey, saved.getName(), target.getToName());
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("pokemon", saved);
        result.put("evolutionItems", ownedItems);
        return result;
    }
    
    // 获取学生的进化道具库存
    public Map<String, Integer> getEvolutionItems(String studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("学生不存在"));
        try {
            return objectMapper.readValue(
                student.getEvolutionItems() != null ? student.getEvolutionItems() : "{}",
                new TypeReference<Map<String, Integer>>() {});
        } catch (Exception e) {
            return new HashMap<>();
        }
    }
    
    // 获取需要道具进化的宝可梦提示（返回所有道具进化条件映射）
    public Map<String, List<String>> getEvolutionItemHints(Integer pokedexId, Integer evolutionStage) {
        Map<String, List<String>> hints = new HashMap<>();
        List<EvolutionRuleEntry> options = getEvolutionOptions(pokedexId, evolutionStage);
        if (options == null || options.isEmpty()) return hints;
        
        for (EvolutionRuleEntry opt : options) {
            if (opt.getToFormName() != null) continue; // 跳过 Mega/极巨化
            String cond = opt.getCondition();
            if (cond == null || isLevelBasedCondition(cond)) continue; // 跳过等级进化
            
            // 提取道具名称
            // 格式: "使用水之石" → 水之石, "连接交换 ；使用联系绳" → 联系绳
            List<String> items = new ArrayList<>();
            Pattern p = Pattern.compile("使用(\\S+)");
            Matcher m = p.matcher(cond);
            while (m.find()) {
                String itemName = m.group(1);
                // 去除可能存在的括号注释
                itemName = itemName.split("（")[0].split("\\(")[0].trim();
                if (!itemName.isEmpty()) items.add(itemName);
            }
            if (!items.isEmpty()) {
                hints.put(opt.getToName(), items);
            }
        }
        return hints;
    }
    public Map<String, Object> getEvolutionInfo(String studentId, String pokemonId) {
        return getEvolutionOptions(studentId, pokemonId);
    }
    // ==================== 精灵球相关方法 ====================
    
    /**
     * 获取学生可用精灵球数量
     */
    public int getPokemonBalls(String studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("学生不存在"));
        return student.getPokemonBalls() != null ? student.getPokemonBalls() : 0;
    }
    
    /**
     * 使用精灵球领取宝可梦（从教师池中随机获得）
     */
    @Transactional
    public StudentPokemon usePokemonBall(String studentId) {
        // 1. 检查学生是否有精灵球
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("学生不存在"));
        int balls = student.getPokemonBalls() != null ? student.getPokemonBalls() : 0;
        if (balls <= 0) {
            throw new IllegalArgumentException("没有可用的精灵球，当前数量: " + balls);
        }
        
        // 2. 获取班级的宝可梦池
        ClassroomPokemonPool pool = classroomPokemonPoolRepository
                .findByClassroomId(student.getTeacherId())
                .orElse(null);
        
        // 2. 获取班级的宝可梦池（带权重的条目列表）
        List<Map<String, Object>> weightedEntries = new ArrayList<>();
        if (pool != null && pool.getPokedexEntries() != null && !pool.getPokedexEntries().isEmpty()) {
            try {
                weightedEntries = objectMapper.readValue(pool.getPokedexEntries(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));
            } catch (Exception e) {
                logger.error("解析宝可梦池JSON失败", e);
            }
        }
        
        // 如果池为空或解析失败，使用默认池（等权重）
        if (weightedEntries.isEmpty()) {
            for (int id : getDefaultPoolPokedexIds()) {
                Map<String, Object> entry = new HashMap<>();
                entry.put("pokedexId", id);
                entry.put("weight", 1.0);
                weightedEntries.add(entry);
            }
        }
        
        // 2.5 构建有效 ID 集合 + 加权条目列表
        Set<Integer> validTier1Ids = new HashSet<>();
        for (Map<String, Object> s : getAllSpecies()) {
            Integer sid = s.get("pokedexId") != null ? ((Number) s.get("pokedexId")).intValue() : 0;
            Integer tier = s.get("tier") != null ? ((Number) s.get("tier")).intValue() : 0;
            Object fForm = s.get("formName");
            if (tier == 1 && (fForm == null || fForm.toString().isEmpty())) {
                validTier1Ids.add(sid);
            }
        }
        
        // 过滤并收集有效条目
        List<Map<String, Object>> validEntries = new ArrayList<>();
        for (Map<String, Object> e : weightedEntries) {
            Integer pid = e.get("pokedexId") != null ? ((Number) e.get("pokedexId")).intValue() : 0;
            if (validTier1Ids.contains(pid)) {
                validEntries.add(e);
            }
        }
        
        if (validEntries.isEmpty()) {
            logger.warn("班级池中的所有图鉴编号在物种数据中均不存在，回退到默认池");
            for (int id : getDefaultPoolPokedexIds()) {
                Map<String, Object> entry = new HashMap<>();
                entry.put("pokedexId", id);
                entry.put("weight", 1.0);
                validEntries.add(entry);
            }
        }
        
        // 3. 加权随机选择（权重越高概率越大）
        double totalWeight = 0.0;
        for (Map<String, Object> e : validEntries) {
            Object w = e.get("weight");
            totalWeight += (w != null ? ((Number) w).doubleValue() : 1.0);
        }
        
        double rand = new Random().nextDouble() * totalWeight;
        double cumulative = 0.0;
        Integer selectedPokedexId = null;
        for (Map<String, Object> e : validEntries) {
            Object w = e.get("weight");
            cumulative += (w != null ? ((Number) w).doubleValue() : 1.0);
            if (rand <= cumulative) {
                selectedPokedexId = ((Number) e.get("pokedexId")).intValue();
                break;
            }
        }
        if (selectedPokedexId == null) {
            selectedPokedexId = ((Number) validEntries.get(validEntries.size() - 1).get("pokedexId")).intValue();
        }
        
        // 4. 获取详细信息（只选初始形态 tier=1）
        Map<String, Object> speciesData = null;
        for (Map<String, Object> s : getAllSpecies()) {
            Integer sid = s.get("pokedexId") != null ? ((Number) s.get("pokedexId")).intValue() : 0;
            Integer tier = s.get("tier") != null ? ((Number) s.get("tier")).intValue() : 0;
            Object fForm = s.get("formName");
            if (sid.equals(selectedPokedexId) && tier == 1 && (fForm == null || fForm.toString().isEmpty())) {
                speciesData = s;
                break;
            }
        }
        
        if (speciesData == null) {
            throw new IllegalArgumentException("无法获取宝可梦数据，图鉴编号: " + selectedPokedexId);
        }
        
        String name = speciesData.get("name") != null ? speciesData.get("name").toString() : "未知";
        String image = speciesData.get("image") != null ? speciesData.get("image").toString() : "";
        String types = speciesData.get("types") != null ? speciesData.get("types").toString() : "[]";
        
        // 5. 创建记录
        StudentPokemon pokemon = new StudentPokemon(studentId, selectedPokedexId, name, image, types, null);
        studentPokemonRepository.save(pokemon);
        
        // 6. 扣除精灵球
        student.setPokemonBalls(balls - 1);
        studentRepository.save(student);
        
        logger.info("学生 {} 使用精灵球获得了 {} ({})", studentId, name, selectedPokedexId);
        
        return pokemon;
    }
    
    /**
     * 获取默认池（Gen1全部初始形态）
     */
    private List<Integer> getDefaultPoolPokedexIds() {
        List<Integer> defaultIds = new ArrayList<>();
        int[] ids = {1, 4, 7, 10, 13, 16, 19, 21, 23, 27, 29, 32, 37, 41, 43, 46, 48, 50, 
                    52, 54, 56, 58, 60, 63, 66, 69, 72, 74, 77, 79, 81, 83, 84, 86, 88, 90, 
                    92, 95, 96, 98, 100, 102, 104, 108, 109, 111, 114, 115, 116, 118, 120, 
                    123, 127, 128, 129, 131, 132, 133, 137, 138, 140, 142, 144, 145, 146, 147, 150, 151};
        for (int id : ids) {
            defaultIds.add(id);
        }
        return defaultIds;
    }
    
    /**
     * 根据图鉴编号获取宝可梦数据（仅基础形态）
     */
    private Map<String, Object> getPokemonSpeciesByPokedexId(Integer pokedexId) {
        List<Map<String, Object>> all = getAllSpecies();
        for (Map<String, Object> s : all) {
            Integer sid = s.get("pokedexId") != null ? ((Number) s.get("pokedexId")).intValue() : 0;
            Integer tier = s.get("tier") != null ? ((Number) s.get("tier")).intValue() : 0;
            Object fForm = s.get("formName");
            if (sid.equals(pokedexId) && tier == 1 && (fForm == null || fForm.toString().isEmpty())) {
                return s;
            }
        }
        return null;
    }


    public Student getStudentById(String studentId) {
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("学生不存在"));
    }

    public Student setRepresentPokemon(String studentId, String pokemonId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("学生不存在"));
        StudentPokemon pokemon = studentPokemonRepository.findById(pokemonId)
                .orElseThrow(() -> new IllegalArgumentException("宝可梦不存在"));
        if (!pokemon.getStudentId().equals(studentId)) {
            throw new IllegalArgumentException("该宝可梦不属于该学生");
        }
        if (pokemonId.equals(student.getRepresentPokemonId())) {
            return student;
        }
        int cards = student.getPetChangeCards() != null ? student.getPetChangeCards() : 0;
        if (cards <= 0) {
            throw new IllegalArgumentException("没有宠物更换卡，请先到小卖部购买");
        }
        student.setPetChangeCards(cards - 1);
        student.setRepresentPokemonId(pokemonId);

        // 同步更新学生形象（petIcon）为代表宝可梦的图片
        String pokemonImage = pokemon.getImage();
        String image = null;
        if (pokemonImage != null && !pokemonImage.isEmpty()) {
            image = pokemonImage.startsWith("/pokemon/") ? pokemonImage : "/pokemon/" + pokemonImage;
        }
        student.setPetIcon(image);
        student.setPetName(pokemon.getName());
        return studentRepository.save(student);
    }
}
