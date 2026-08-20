package com.classpet.service;

import com.classpet.dto.ShopDto.ShopItemRequest;
import com.classpet.dto.ShopDto.ShopItemResponse;
import com.classpet.dto.ShopDto.ExchangeRequest;
import com.classpet.dto.ShopDto.GiftRequest;
import com.classpet.dto.ShopDto.ExchangeRecordResponse;
import com.classpet.entity.ShopItem;
import com.classpet.entity.ExchangeRecord;
import com.classpet.entity.ScoreHistory;
import com.classpet.entity.Student;
import com.classpet.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@Service
public class ShopService {

    private final ShopItemRepository shopItemRepo;
    private final ExchangeRecordRepository exchangeRecordRepo;
    private final StudentRepository studentRepo;
    private final ScoreHistoryRepository scoreHistoryRepo;

    public ShopService(ShopItemRepository shopItemRepo, ExchangeRecordRepository exchangeRecordRepo, StudentRepository studentRepo, ScoreHistoryRepository scoreHistoryRepo) {
        this.shopItemRepo = shopItemRepo;
        this.exchangeRecordRepo = exchangeRecordRepo;
        this.studentRepo = studentRepo;
        this.scoreHistoryRepo = scoreHistoryRepo;
    }

    // ============== 商店商品管理 ==============
    public List<ShopItemResponse> getItems(String teacherId) {
        return shopItemRepo.findByTeacherIdOrderByCreatedAtDesc(teacherId)
                .stream().map(ShopItemResponse::from).collect(Collectors.toList());
    }

    public ShopItemResponse addItem(ShopItemRequest req, String teacherId) {
        ShopItem item = new ShopItem();
        item.setName(req.getName());
        item.setIcon(req.getIcon());
        item.setPrice(req.getPrice());
        item.setDescription(req.getDescription());
        item.setStock(req.getStock());
        item.setItemType(req.getItemType() != null ? req.getItemType() : "decoration");
        item.setEvolutionItemKey(req.getEvolutionItemKey());
        item.setTeacherId(teacherId);
        return ShopItemResponse.from(shopItemRepo.save(item));
    }

    public ShopItemResponse updateItem(String itemId, ShopItemRequest req, String teacherId) {
        ShopItem item = shopItemRepo.findById(itemId)
                .orElseThrow(() -> new RuntimeException("商品不存在"));
        if (!item.getTeacherId().equals(teacherId)) throw new RuntimeException("无权限");
        item.setName(req.getName());
        item.setIcon(req.getIcon());
        item.setPrice(req.getPrice());
        item.setDescription(req.getDescription());
        item.setStock(req.getStock());
        if (req.getItemType() != null) item.setItemType(req.getItemType());
        item.setEvolutionItemKey(req.getEvolutionItemKey());
        return ShopItemResponse.from(shopItemRepo.save(item));
    }

    public void deleteItem(String itemId, String teacherId) {
        ShopItem item = shopItemRepo.findById(itemId)
                .orElseThrow(() -> new RuntimeException("商品不存在"));
        if (!item.getTeacherId().equals(teacherId)) throw new RuntimeException("无权限");
        shopItemRepo.delete(item);
    }

    // ============== 兑换功能 ==============
    @Transactional
    public ExchangeRecordResponse exchange(ExchangeRequest req, String teacherId) {
        // 查找学生
        Student student = studentRepo.findById(req.getStudentId())
                .orElseThrow(() -> new RuntimeException("学生不存在"));
        if (!student.getTeacherId().equals(teacherId)) throw new RuntimeException("无权限");

        // 查找商品
        ShopItem item = shopItemRepo.findById(req.getItemId())
                .orElseThrow(() -> new RuntimeException("商品不存在"));
        if (!item.getTeacherId().equals(teacherId)) throw new RuntimeException("无权限");

        // 检查库存
        if (item.getStock() <= 0) throw new RuntimeException("商品已售罄");

        // 检查积分
        if (student.getFood() < item.getPrice()) throw new RuntimeException("粮食不足");

        // 扣积分并写入历史
        int spent = item.getPrice();
        student.setFood(student.getFood() - spent);
        studentRepo.save(student);

        // 写入积分历史记录（reason显示商品名称，不重复 emoji 因为 scoreItemIcon 已有）
        ScoreHistory history = new ScoreHistory();
        history.setStudentId(student.getId());
        history.setStudentName(student.getName());
        history.setScoreItemName("购买「" + item.getName() + "」");
        history.setScoreItemIcon(item.getIcon());
        history.setPoint(-spent);
        history.setTeacherId(teacherId);
        scoreHistoryRepo.save(history);

        // 特殊商品处理：宠物更换卡（购买时不创建兑换记录）
        if ("petCard".equals(item.getItemType()) || "pet_change_card".equals(item.getItemType())) {
            int cards = student.getPetChangeCards() != null ? student.getPetChangeCards() : 0;
            student.setPetChangeCards(cards + 1);
            studentRepo.save(student);
            // 宠物更换卡不创建兑换记录，只需积分历史（已在上方写入）
            return null;
        }

        // 特殊商品处理：精灵球（购买时不创建兑换记录）
        if ("pokemon_ball".equals(item.getItemType()) || "POKEBALL".equals(item.getItemType())) {
            int balls = student.getPokemonBalls() != null ? student.getPokemonBalls() : 0;
            student.setPokemonBalls(balls + 1);
            studentRepo.save(student);
            // 精灵球不创建兑换记录，只需积分历史（已在上方写入）
            return null;
        }

        // 扣库存
        item.setStock(item.getStock() - 1);
        shopItemRepo.save(item);

        // 记录兑换
        ExchangeRecord record = new ExchangeRecord();
        record.setStudentId(student.getId());
        record.setStudentName(student.getName());
        record.setItemId(item.getId());
        record.setItemName(item.getName());
        record.setItemIcon(item.getIcon());
        record.setFoodSpent(item.getPrice());
        record.setTeacherId(teacherId);
        record.setActionType(ExchangeRecord.ActionType.PURCHASE.name());
        ExchangeRecord saved = exchangeRecordRepo.save(record);

        // 进化道具：+1 到学生库存（以 evolutionItemKey 为 key，没有则用 name）
        if ("evolution_item".equals(item.getItemType())) {
            String itemKey = item.getEvolutionItemKey() != null && !item.getEvolutionItemKey().isEmpty()
                    ? item.getEvolutionItemKey() : item.getName();
            addEvolutionItem(student, itemKey, 1);
            studentRepo.save(student);
        }

        return ExchangeRecordResponse.from(saved);
    }

    // ============== 兑换记录 ==============
    public List<ExchangeRecordResponse> getRecords(String teacherId) {
        return exchangeRecordRepo.findByTeacherIdOrderByCreatedAtDesc(teacherId)
                    .stream().map(ExchangeRecordResponse::from).collect(Collectors.toList());
    }

    /**
     * 兑换记录游标分页：响应 {items, hasMore, nextCursor}
     */
    public Map<String, Object> getRecordsPage(String teacherId,
                                               java.time.LocalDateTime cursorTime, String cursorId, int limit) {
        int safeLimit = Math.max(1, Math.min(100, limit));
        java.util.List<ExchangeRecord> rows = exchangeRecordRepo.findTeacherPage(
                teacherId, cursorTime, cursorId,
                org.springframework.data.domain.PageRequest.of(0, safeLimit + 1));
        boolean hasMore = rows.size() > safeLimit;
        if (hasMore) rows = rows.subList(0, safeLimit);

        java.util.List<ExchangeRecordResponse> items = rows.stream()
                .map(ExchangeRecordResponse::from).collect(Collectors.toList());
        Map<String, Object> resp = new HashMap<>();
        resp.put("items", items);
        resp.put("hasMore", hasMore);
        if (hasMore && !rows.isEmpty()) {
            ExchangeRecord tail = rows.get(rows.size() - 1);
            Map<String, Object> cursor = new HashMap<>();
            cursor.put("createdAt", tail.getCreatedAt());
            cursor.put("id", tail.getId());
            resp.put("nextCursor", cursor);
        } else {
            resp.put("nextCursor", null);
        }
        return resp;
    }

    // ============== 道具赠送 ==============
    @Transactional
    public ExchangeRecordResponse giftItem(GiftRequest req, String teacherId) {
        // 查找赠送者
        Student fromStudent = studentRepo.findById(req.getFromStudentId())
                .orElseThrow(() -> new RuntimeException("赠送者不存在"));
        if (!fromStudent.getTeacherId().equals(teacherId)) throw new RuntimeException("无权限");

        // 查找接收者
        Student toStudent = studentRepo.findById(req.getToStudentId())
                .orElseThrow(() -> new RuntimeException("接收者不存在"));
        if (!toStudent.getTeacherId().equals(teacherId)) throw new RuntimeException("无权限");

        // 不能赠送给自己
        if (req.getFromStudentId().equals(req.getToStudentId())) {
            throw new RuntimeException("不能赠送给自己");
        }

        // 查找兑换记录
        ExchangeRecord record = exchangeRecordRepo.findById(req.getRecordId())
                .orElseThrow(() -> new RuntimeException("兑换记录不存在"));
        if (!record.getStudentId().equals(req.getFromStudentId())) {
            throw new RuntimeException("该道具不属于你");
        }
        // 关键防护：已赠出的 record 不能重复赠送
        String at = record.getActionType();
        if (at != null && ExchangeRecord.ActionType.GIFT_OUT.name().equals(at)) {
            throw new RuntimeException("该道具已赠出，不能重复赠送");
        }

        // 检查是否已装备
        String equippedJson = fromStudent.getEquippedItems();
        if (equippedJson != null && !equippedJson.isEmpty()) {
            try {
                java.util.List<String> equipped = new com.fasterxml.jackson.databind.ObjectMapper()
                        .readValue(equippedJson, new com.fasterxml.jackson.core.type.TypeReference<java.util.List<String>>() {});
                if (equipped.contains(record.getItemId())) {
                    throw new RuntimeException("已装备的道具需先卸下才能赠送");
                }
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                // JSON解析失败，忽略
            } catch (RuntimeException e) {
                throw e;
            }
        }

        // 保留原 record 为 GIFT_OUT 赠出侧（studentId=赠出方不变）
        record.setGiftTo(toStudent.getId());
        record.setGiftToName(toStudent.getName());
        // 关键修复：清除原 giftFrom（如果之前是 GIFT_IN 接收记录）
        record.setGiftFrom(null);
        record.setGiftFromName(null);
        record.setActionType(ExchangeRecord.ActionType.GIFT_OUT.name());
        exchangeRecordRepo.save(record);

        // 为接收方新建一条 GIFT_IN 记录
        ExchangeRecord inRecord = new ExchangeRecord();
        inRecord.setStudentId(toStudent.getId());
        inRecord.setStudentName(toStudent.getName());
        inRecord.setItemId(record.getItemId());
        inRecord.setItemName(record.getItemName());
        inRecord.setItemIcon(record.getItemIcon());
        inRecord.setFoodSpent(0);
        inRecord.setTeacherId(teacherId);
        inRecord.setGiftFrom(fromStudent.getId());
        inRecord.setGiftFromName(fromStudent.getName());
        inRecord.setActionType(ExchangeRecord.ActionType.GIFT_IN.name());
        ExchangeRecord savedIn = exchangeRecordRepo.save(inRecord);

        // 进化道具：赠出方 -1，接收方 +1
        ShopItem item = shopItemRepo.findById(record.getItemId()).orElse(null);
        if (item != null && "evolution_item".equals(item.getItemType())) {
            String itemKey = item.getEvolutionItemKey() != null && !item.getEvolutionItemKey().isEmpty()
                    ? item.getEvolutionItemKey() : item.getName();
            addEvolutionItem(fromStudent, itemKey, -1);
            studentRepo.save(fromStudent);
            addEvolutionItem(toStudent, itemKey, 1);
            studentRepo.save(toStudent);
        }

        return ExchangeRecordResponse.from(record);
    }

    // ============== 进化道具数据 ==============
    private static final Object[][] EVOLUTION_ITEMS_DATA = {
        // 普通进化石
        {"💧", "水之石", 30, "让特定宝可梦进化的神秘石头", 99, "水之石"},
        {"🔥", "火之石", 30, "让特定宝可梦进化的神秘石头", 99, "火之石"},
        {"🍃", "叶之石", 30, "让特定宝可梦进化的神秘石头", 99, "叶之石"},
        {"🌙", "月之石", 30, "让特定宝可梦进化的神秘石头", 99, "月之石"},
        {"⚡", "雷之石", 30, "让特定宝可梦进化的神秘石头", 99, "雷之石"},
        {"❄️", "冰之石", 30, "让冰系宝可梦进化的神秘石头", 99, "冰之石"},
        {"☀️", "日之石", 30, "让特定宝可梦进化的神秘石头", 99, "日之石"},
        {"🔮", "黑奇石", 30, "让特定宝可梦进化的神秘石头", 99, "黑奇石"},
        // 连接交换进化道具
        {"🔗", "联系绳", 30, "让特定宝可梦进化的神秘绳索", 99, "联系绳"},
        {"📿", "王者之证", 30, "让特定宝可梦进化的神秘道具", 99, "王者之证"},
        {"⚙️", "金属膜", 30, "让特定宝可梦进化的金属膜", 99, "金属膜"},
        {"🛡️", "护具", 30, "让特定宝可梦进化的护具", 99, "护具"},
        {"🐉", "龙之鳞片", 30, "让特定宝可梦进化的龙鳞", 99, "龙之鳞片"},
        {"💾", "升级数据", 30, "让特定宝可梦进化的数据道具", 99, "升级数据"},
        {"💉", "可疑补丁", 30, "让特定宝可梦进化的可疑道具", 99, "可疑补丁"},
        // 亲密度进化（亲密度/友好度/特定招式条件统一为一种道具）
        {"💜", "亲密度进化石", 50, "让亲密度足够的宝可梦进化（太阳伊布/月亮伊布/仙子伊布等）", 99, "亲密度进化石"},
        // 通讯交换进化道具（覆盖 18 条通讯交换规则）
        {"🔄", "通讯交换石", 50, "通过通讯交换进化（如肯泰罗/瓦斯弹等）", 99, "通讯交换石"},
        // 携带道具进化（原"通讯交换时携带 X"降级为直接使用道具）
        {"🌊", "深海之牙", 30, "让玛瑙水母进化（北海狮→玛瑙水母分支）", 99, "深海之牙"},
        {"🦈", "深海鳞片", 30, "让刺龙王进化（北海狮→刺龙王分支）", 99, "深海鳞片"},
        {"🔌", "电气珠", 30, "让电飞龙进化（鸭嘴炎龙携带道具）", 99, "电气珠"},
        {"🌋", "熔岩器具", 30, "让鸭嘴爆龙进化（鸭嘴炎龙携带道具）", 99, "熔岩器具"},
        {"👻", "灵界之布", 30, "让彷徨夜灵进化（梦妖魔携带道具）", 99, "灵界之布"},
        {"🌸", "香袋", 30, "让风妖精进化（樱花儿携带道具）", 99, "香袋"},
        {"🍰", "奶油蛋糕", 30, "让胖甜妮进化（甜竹竹携带道具）", 99, "奶油蛋糕"},
        // 时段+携带进化道具（时段约束通过 condition 字段保留提示）
        {"🦷", "锐利之牙", 30, "让玛狃拉进化（大狼犬携带道具）", 99, "锐利之牙"},
        {"🦅", "锐利之爪", 30, "让狙射树枭/炽焰咆哮虎进化（飞天螳螂携带道具）", 99, "锐利之爪"},
        {"⚪", "圆之石", 30, "让天然雀进化（天然鸟携带道具）", 99, "圆之石"}
    };

    // ============== 进化道具迁移（旧账号兼容，逐个检查补充） ==============
    public void migrateEvolutionItems(String teacherId) {
        List<ShopItem> existing = shopItemRepo.findByTeacherIdOrderByCreatedAtDesc(teacherId);
        // 提取已有进化道具的名称集合
        java.util.Set<String> existingNames = existing.stream()
                .filter(i -> "evolution_item".equals(i.getItemType()))
                .map(ShopItem::getName)
                .collect(Collectors.toSet());
        // 逐个检查，缺失的才创建
        for (Object[] d : EVOLUTION_ITEMS_DATA) {
            String itemName = (String) d[5];  // evolutionItemKey
            if (!existingNames.contains(itemName)) {
                ShopItem item = new ShopItem();
                item.setIcon((String) d[0]);
                item.setName((String) d[1]);
                item.setPrice((Integer) d[2]);
                item.setDescription((String) d[3]);
                item.setStock((Integer) d[4]);
                item.setTeacherId(teacherId);
                item.setItemType("evolution_item");
                item.setEvolutionItemKey(itemName);
                shopItemRepo.save(item);
            }
        }
    }

    // 创建进化道具商品
    private void createEvolutionItems(String teacherId) {
        for (Object[] d : EVOLUTION_ITEMS_DATA) {
            ShopItem item = new ShopItem();
            item.setIcon((String) d[0]);
            item.setName((String) d[1]);
            item.setPrice((Integer) d[2]);
            item.setDescription((String) d[3]);
            item.setStock((Integer) d[4]);
            item.setTeacherId(teacherId);
            item.setItemType("evolution_item");
            item.setEvolutionItemKey((String) d[5]);
            shopItemRepo.save(item);
        }
    }

    // ============== 默认商品初始化 ==============
    public void initializeDefaults(String teacherId) {
        List<ShopItem> existing = shopItemRepo.findByTeacherIdOrderByCreatedAtDesc(teacherId);

        // 首次初始化：装饰道具 + 宠物更换卡（仅在商品表为空时）
        if (existing.isEmpty()) {
            Object[][] defaults = {
                // 头饰类
                {"🎀", "蝴蝶结", 8, "可爱蝴蝶结，装扮宠物头顶", 99},
                {"👑", "金色王冠", 25, "尊贵王冠，传说中的装饰", 30},
                {"🎩", "魔术礼帽", 15, "神秘魔术师的礼帽", 50},
                {"🌸", "樱花头饰", 12, "春日限定的樱花发饰", 40},
                {"🎓", "毕业帽", 20, "学霸专属毕业帽", 35},
                // 衣饰类
                {"🧣", "温暖围巾", 10, "柔软温暖的围巾", 60},
                {"👕", "潮流T恤", 12, "时尚潮流的宠物T恤", 50},
                {"🧥", "魔法斗篷", 30, "神秘魔法斗篷，传说级", 20},
                {"👗", "公主裙", 18, "梦幻公主裙", 35},
                {"🦺", "冒险背心", 14, "勇敢冒险者的背心", 45},
                // 配饰类
                {"🕶️", "墨镜", 10, "酷酷的宠物墨镜", 55},
                {"💍", "闪亮戒指", 22, "闪闪发光的戒指", 25},
                {"⌚", "智能手表", 16, "高科技宠物手表", 40},
                {"🔔", "铃铛项链", 8, "叮叮当的铃铛项链", 65},
                {"🎒", "小书包", 14, "迷你书包，上学必备", 45},
                // 特效类
                {"✨", "星光特效", 20, "全身闪烁星光", 30},
                {"🌈", "彩虹光环", 28, "头上浮现彩虹光环", 25},
                {"🔥", "烈焰光环", 35, "传说级烈焰光环", 15},
                {"💫", "旋转星星", 18, "身边旋转的小星星", 35},
                {"🛡️", "守护盾牌", 24, "闪耀的守护之盾", 30}
            };
            for (Object[] d : defaults) {
                ShopItem item = new ShopItem();
                item.setIcon((String) d[0]);
                item.setName((String) d[1]);
                item.setPrice((Integer) d[2]);
                item.setDescription((String) d[3]);
                item.setStock((Integer) d[4]);
                item.setTeacherId(teacherId);
                item.setItemType("decoration");
                shopItemRepo.save(item);
            }

            // 添加宠物更换卡
            ShopItem petCard = new ShopItem();
            petCard.setIcon("🎫");
            petCard.setName("宠物更换卡");
            petCard.setPrice(15);
            petCard.setDescription("使用后可更换宠物");
            petCard.setStock(99);
            petCard.setTeacherId(teacherId);
            petCard.setItemType("pet_change_card");
            shopItemRepo.save(petCard);

            // 添加精灵球（首次初始化时）
            ShopItem pokeball = new ShopItem();
            pokeball.setIcon("⚪");
            pokeball.setName("精灵球");
            pokeball.setPrice(100);
            pokeball.setDescription("使用后可领取随机宝可梦");
            pokeball.setStock(999);
            pokeball.setTeacherId(teacherId);
            pokeball.setItemType("pokemon_ball");
            shopItemRepo.save(pokeball);

            // 重新获取列表（包含刚添加的商品）
            existing = shopItemRepo.findByTeacherIdOrderByCreatedAtDesc(teacherId);
        }

        // 始终检查：补充缺失的进化道具（兼容旧账号及新添加的道具）
        migrateEvolutionItems(teacherId);
    }

    /**
     * 调整学生 evolutionItems 库存（delta 可为 -1 / +1）
     * 委托给 util 实现，避免重复代码。
     */
    private void addEvolutionItem(Student student, String itemKey, int delta) {
        com.classpet.util.EvolutionItemUtil.adjust(student, itemKey, delta);
    }

    /**
     * 撤销兑换（教师权限）
     * - 仅可撤销 actionType=PURCHASE 的 record
     * - 回退 foodSpent 粮食给学生
     * - 如果是 evolution_item 类型，扣除该学生 1 个 evolutionItems 库存
     * - 标记 record.actionType=REVOKED（保留原字段便于查看）
     */
    @Transactional
    public ExchangeRecordResponse revokeExchange(String recordId, String teacherId) {
        ExchangeRecord record = exchangeRecordRepo.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("兑换记录不存在"));
        if (!record.getTeacherId().equals(teacherId)) {
            throw new IllegalArgumentException("无权操作该记录");
        }
        String currentAction = record.getActionType();
        if (currentAction == null || "PURCHASE".equals(currentAction)) {
            // OK 可以撤销
        } else if ("REVOKED".equals(currentAction)) {
            throw new IllegalArgumentException("该记录已被撤销");
        } else {
            throw new IllegalArgumentException("只能撤销兑换记录（GIFT_OUT/GIFT_IN 不能撤销）");
        }

        // 关键防护：该道具一旦赠送出去过，不能撤销（接收方仍持有，兑分会不一致）
        if (record.getGiftTo() != null || record.getGiftToName() != null) {
            throw new IllegalArgumentException("该道具已赠送给「" + record.getGiftToName() + "」，无法撤销（请先联系接收方归还后再撤销）");
        }

        // 退还 foodSpent 粮食给学生
        Student student = studentRepo.findById(record.getStudentId())
                .orElseThrow(() -> new IllegalArgumentException("学生不存在"));
        int refund = record.getFoodSpent() == null ? 0 : record.getFoodSpent();
        student.setFood((student.getFood() == null ? 0 : student.getFood()) + refund);
        studentRepo.save(student);

        // 进化道具：扣 1 个库存
        ShopItem item = shopItemRepo.findById(record.getItemId()).orElse(null);
        if (item != null && "evolution_item".equals(item.getItemType())) {
            String itemKey = item.getEvolutionItemKey() != null && !item.getEvolutionItemKey().isEmpty()
                    ? item.getEvolutionItemKey() : item.getName();
            addEvolutionItem(student, itemKey, -1);
            studentRepo.save(student);
        }

        // 退还商品库存（学生购买时 -1，撤销 +1）
        if (item != null && item.getStock() != null) {
            item.setStock(item.getStock() + 1);
            shopItemRepo.save(item);
        }

        // 标记为 REVOKED
        record.setActionType(ExchangeRecord.ActionType.REVOKED.name());
        exchangeRecordRepo.save(record);

        return ExchangeRecordResponse.from(record);
    }
}