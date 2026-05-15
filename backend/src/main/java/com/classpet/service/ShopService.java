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

        // 写入积分历史记录（reason显示商品名称）
        ScoreHistory history = new ScoreHistory();
        history.setStudentId(student.getId());
        history.setStudentName(student.getName());
        history.setScoreItemName(item.getIcon() + " 购买「" + item.getName() + "」");
        history.setScoreItemIcon(item.getIcon());
        history.setPoint(-spent);
        history.setTeacherId(teacherId);
        scoreHistoryRepo.save(history);

        // 特殊商品处理：宠物更换卡（购买时不创建兑换记录）
        if ("petCard".equals(item.getItemType())) {
            int cards = student.getPetChangeCards() != null ? student.getPetChangeCards() : 0;
            student.setPetChangeCards(cards + 1);
            studentRepo.save(student);
            // 宠物更换卡不创建兑换记录，只需积分历史（已在上方写入）
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
        return ExchangeRecordResponse.from(exchangeRecordRepo.save(record));
    }

    // ============== 兑换记录 ==============
    public List<ExchangeRecordResponse> getRecords(String teacherId) {
        return exchangeRecordRepo.findByTeacherIdOrderByCreatedAtDesc(teacherId)
                    .stream().map(ExchangeRecordResponse::from).collect(Collectors.toList());
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

        // 转移道具：修改兑换记录的归属
        record.setStudentId(toStudent.getId());
        record.setStudentName(toStudent.getName());
        record.setGiftFrom(fromStudent.getId());
        record.setGiftFromName(fromStudent.getName());
        exchangeRecordRepo.save(record);

        return ExchangeRecordResponse.from(record);
    }

    // ============== 进化道具数据 ==============
    private static final Object[][] EVOLUTION_ITEMS_DATA = {
        {"💧", "水之石", 30, "让特定宝可梦进化的神秘石头", 99, "水之石"},
        {"🔥", "火之石", 30, "让特定宝可梦进化的神秘石头", 99, "火之石"},
        {"🍃", "叶之石", 30, "让特定宝可梦进化的神秘石头", 99, "叶之石"},
        {"🌙", "月之石", 30, "让特定宝可梦进化的神秘石头", 99, "月之石"},
        {"⚡", "雷之石", 30, "让特定宝可梦进化的神秘石头", 99, "雷之石"},
        {"🔗", "联系绳", 30, "让特定宝可梦进化的神秘绳索", 99, "联系绳"}
    };

    // ============== 进化道具迁移（旧账号兼容） ==============
    public void migrateEvolutionItems(String teacherId) {
        List<ShopItem> existing = shopItemRepo.findByTeacherIdOrderByCreatedAtDesc(teacherId);
        boolean hasEvolutionItem = existing.stream().anyMatch(i -> "evolution_item".equals(i.getItemType()));
        if (!hasEvolutionItem) {
            createEvolutionItems(teacherId);
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

        // 始终检查：补充缺失的进化道具（兼容旧账号）
        boolean hasEvolutionItem = existing.stream().anyMatch(i -> "evolution_item".equals(i.getItemType()));
        if (!hasEvolutionItem) {
            createEvolutionItems(teacherId);
        }
    }
}