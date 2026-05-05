package com.classpet.service;

import com.classpet.entity.Classroom;
import com.classpet.repository.ClassroomRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ClassroomService {

    @Autowired
    private ClassroomRepository classroomRepository;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // 默认唐诗宋词数据（52首）— 含朝代
    private static final List<Map<String, String>> DEFAULT_POEMS = Arrays.asList(
        Map.of("title","静夜思","author","李白","dynasty","唐","content","床前明月光，疑是地上霜。\n举头望明月，低头思故乡。"),
        Map.of("title","春晓","author","孟浩然","dynasty","唐","content","春眠不觉晓，处处闻啼鸟。\n夜来风雨声，花落知多少。"),
        Map.of("title","登鹳雀楼","author","王之涣","dynasty","唐","content","白日依山尽，黄河入海流。\n欲穷千里目，更上一层楼。"),
        Map.of("title","相思","author","王维","dynasty","唐","content","红豆生南国，春来发几枝。\n愿君多采撷，此物最相思。"),
        Map.of("title","咏鹅","author","骆宾王","dynasty","唐","content","鹅，鹅，鹅，曲项向天歌。\n白毛浮绿水，红掌拨清波。"),
        Map.of("title","悯农（其二）","author","李绅","dynasty","唐","content","锄禾日当午，汗滴禾下土。\n谁知盘中餐，粒粒皆辛苦。"),
        Map.of("title","悯农（其一）","author","李绅","dynasty","唐","content","春种一粒粟，秋收万颗子。\n四海无闲田，农夫犹饿死。"),
        Map.of("title","江雪","author","柳宗元","dynasty","唐","content","千山鸟飞绝，万径人踪灭。\n孤舟蓑笠翁，独钓寒江雪。"),
        Map.of("title","鹿柴","author","王维","dynasty","唐","content","空山不见人，但闻人语响。\n返景入深林，复照青苔上。"),
        Map.of("title","竹里馆","author","王维","dynasty","唐","content","独坐幽篁里，弹琴复长啸。\n深林人不知，明月来相照。"),
        Map.of("title","送别","author","王维","dynasty","唐","content","山中相送罢，日暮掩柴扉。\n春草明年绿，王孙归不归？"),
        Map.of("title","宿建德江","author","孟浩然","dynasty","唐","content","移舟泊烟渚，日暮客愁新。\n野旷天低树，江清月近人。"),
        Map.of("title","鸟鸣涧","author","王维","dynasty","唐","content","人闲桂花落，夜静春山空。\n月出惊山鸟，时鸣春涧中。"),
        Map.of("title","逢雪宿芙蓉山主人","author","刘长卿","dynasty","唐","content","日暮苍山远，天寒白屋贫。\n柴门闻犬吠，风雪夜归人。"),
        Map.of("title","问刘十九","author","白居易","dynasty","唐","content","绿蚁新醅酒，红泥小火炉。\n晚来天欲雪，能饮一杯无？"),
        Map.of("title","夜宿山寺","author","李白","dynasty","唐","content","危楼高百尺，手可摘星辰。\n不敢高声语，恐惊天上人。"),
        Map.of("title","望庐山瀑布","author","李白","dynasty","唐","content","日照香炉生紫烟，遥看瀑布挂前川。\n飞流直下三千尺，疑是银河落九天。"),
        Map.of("title","绝句","author","杜甫","dynasty","唐","content","两个黄鹂鸣翠柳，一行白鹭上青天。\n窗含西岭千秋雪，门泊东吴万里船。"),
        Map.of("title","枫桥夜泊","author","张继","dynasty","唐","content","月落乌啼霜满天，江枫渔火对愁眠。\n姑苏城外寒山寺，夜半钟声到客船。"),
        Map.of("title","清明","author","杜牧","dynasty","唐","content","清明时节雨纷纷，路上行人欲断魂。\n借问酒家何处有，牧童遥指杏花村。"),
        Map.of("title","黄鹤楼送孟浩然之广陵","author","李白","dynasty","唐","content","故人西辞黄鹤楼，烟花三月下扬州。\n孤帆远影碧空尽，唯见长江天际流。"),
        Map.of("title","回乡偶书","author","贺知章","dynasty","唐","content","少小离家老大回，乡音无改鬓毛衰。\n儿童相见不相识，笑问客从何处来。"),
        Map.of("title","出塞","author","王昌龄","dynasty","唐","content","秦时明月汉时关，万里长征人未还。\n但使龙城飞将在，不教胡马度阴山。"),
        Map.of("title","凉州词","author","王翰","dynasty","唐","content","葡萄美酒夜光杯，欲饮琵琶马上催。\n醉卧沙场君莫笑，古来征战几人回？"),
        Map.of("title","别董大","author","高适","dynasty","唐","content","千里黄云白日曛，北风吹雁雪纷纷。\n莫愁前路无知己，天下谁人不识君。"),
        Map.of("title","望天门山","author","李白","dynasty","唐","content","天门中断楚江开，碧水东流至此回。\n两岸青山相对出，孤帆一片日边来。"),
        Map.of("title","早发白帝城","author","李白","dynasty","唐","content","朝辞白帝彩云间，千里江陵一日还。\n两岸猿声啼不住，轻舟已过万重山。"),
        Map.of("title","滁州西涧","author","韦应物","dynasty","唐","content","独怜幽草涧边生，上有黄鹂深树鸣。\n春潮带雨晚来急，野渡无人舟自横。"),
        Map.of("title","江南逢李龟年","author","杜甫","dynasty","唐","content","岐王宅里寻常见，崔九堂前几度闻。\n正是江南好风景，落花时节又逢君。"),
        Map.of("title","九月九日忆山东兄弟","author","王维","dynasty","唐","content","独在异乡为异客，每逢佳节倍思亲。\n遥知兄弟登高处，遍插茱萸少一人。"),
        Map.of("title","送元二使安西","author","王维","dynasty","唐","content","渭城朝雨浥轻尘，客舍青青柳色新。\n劝君更尽一杯酒，西出阳关无故人。"),
        Map.of("title","望月怀远","author","张九龄","dynasty","唐","content","海上生明月，天涯共此时。\n情人怨遥夜，竟夕起相思。"),
        Map.of("title","题西林壁","author","苏轼","dynasty","宋","content","横看成岭侧成峰，远近高低各不同。\n不识庐山真面目，只缘身在此山中。"),
        Map.of("title","春望","author","杜甫","dynasty","唐","content","国破山河在，城春草木深。\n感时花溅泪，恨别鸟惊心。\n烽火连三月，家书抵万金。\n白头搔更短，浑欲不胜簪。"),
        Map.of("title","赋得古原草送别","author","白居易","dynasty","唐","content","离离原上草，一岁一枯荣。\n野火烧不尽，春风吹又生。\n远芳侵古道，晴翠接荒城。\n又送王孙去，萋萋满别情。"),
        Map.of("title","黄鹤楼","author","崔颢","dynasty","唐","content","昔人已乘黄鹤去，此地空余黄鹤楼。\n黄鹤一去不复返，白云千载空悠悠。\n晴川历历汉阳树，芳草萋萋鹦鹉洲。\n日暮乡关何处是？烟波江上使人愁。"),
        Map.of("title","游子吟","author","孟郊","dynasty","唐","content","慈母手中线，游子身上衣。\n临行密密缝，意恐迟迟归。\n谁言寸草心，报得三春晖。"),
        Map.of("title","将进酒","author","李白","dynasty","唐","content","君不见黄河之水天上来，奔流到海不复回。\n君不见高堂明镜悲白发，朝如青丝暮成雪。\n人生得意须尽欢，莫使金樽空对月。\n天生我材必有用，千金散尽还复来。"),
        Map.of("title","登高","author","杜甫","dynasty","唐","content","风急天高猿啸哀，渚清沙白鸟飞回。\n无边落木萧萧下，不尽长江滚滚来。\n万里悲秋常作客，百年多病独台。\n艰难苦恨繁霜鬓，潦倒新停浊酒杯。"),
        Map.of("title","水调歌头","author","苏轼","dynasty","宋","content","明月几时有？把酒问青天。\n不知天上宫阙，今夕是何年。\n我欲乘风归去，又恐琼楼玉宇，高处不胜寒。\n起舞弄清影，何似在人间。"),
        Map.of("title","念奴娇·赤壁怀古","author","苏轼","dynasty","宋","content","大江东去，浪淘尽，千古风流人物。\n故垒西边，人道是，三国周郎赤壁。\n乱石穿空，惊涛拍岸，卷起千堆雪。\n江山如画，一时多少豪杰。"),
        Map.of("title","如梦令","author","李清照","dynasty","宋","content","常记溪亭日暮，沉醉不知归路。\n兴尽晚回舟，误入藕花深处。\n争渡，争渡，惊起一滩鸥鹭。"),
        Map.of("title","声声慢","author","李清照","dynasty","宋","content","寻寻觅觅，冷冷清清，凄凄惨惨戚戚。\n乍暖还寒时候，最难将息。\n三杯两盏淡酒，怎敌他、晚来风急！\n雁过也，正伤心，却是旧时相识。"),
        Map.of("title","破阵子","author","辛弃疾","dynasty","宋","content","醉里挑灯看剑，梦回吹角连营。\n八百里分麾下炙，五十弦翻塞外声。\n沙场秋点兵。"),
        Map.of("title","青玉案·元夕","author","辛弃疾","dynasty","宋","content","东风夜放花千树，更吹落、星如雨。\n宝马雕车香满路。\n凤箫声动，玉壶光转，一夜鱼龙舞。\n众里寻他千百度。\n蓦然回首，那人却在，灯火阑珊处。"),
        Map.of("title","虞美人","author","李煜","dynasty","南唐","content","春花秋月何时了？往事知多少。\n小楼昨夜又东风，故国不堪回首月明中。\n问君能有几多愁？恰似一江春水向东流。"),
        Map.of("title","浣溪沙","author","晏殊","dynasty","宋","content","一曲新词酒一杯，去年天气旧亭台。\n夕阳西下几时回？\n无可奈何花落去，似曾相识燕归来。\n小园香径独徘徊。"),
        Map.of("title","卜算子·咏梅","author","陆游","dynasty","宋","content","驿外断桥边，寂寞开无主。\n已是黄昏独自愁，更著风和雨。\n无意苦争春，一任群芳妒。\n零落成泥碾作尘，只有香如故。"),
        Map.of("title","渔家傲·秋思","author","范仲淹","dynasty","宋","content","塞下秋来风景异，衡阳雁去无留意。\n四面边声连角起，千嶂里，长烟落日孤城闭。\n浊酒一杯家万里，燕然未勒归无计。\n羌管悠悠霜满地，人不寐，将军白发征夫泪。"),
        Map.of("title","满江红","author","岳飞","dynasty","宋","content","怒发冲冠，凭栏处、潇潇雨歇。\n抬望眼、仰天长啸，壮怀激烈。\n三十功名尘与土，八千里路云和月。\n莫等闲、白了少年头，空悲切。"),
        Map.of("title","江城子·密州出猎","author","苏轼","dynasty","宋","content","老夫聊发少年狂，左牵黄，右擎苍。\n锦帽貂裘，千骑卷平冈。\n为报倾城随太守，亲射虎，看孙郎。"),
        Map.of("title","相见欢","author","李煜","dynasty","南唐","content","无言独上西楼，月如钩。\n寂寞梧桐深院锁清秋。\n剪不断，理还乱，是离愁。\n别是一般滋味在心头。")
    );

    public List<Classroom> getClassrooms(String teacherId) {
        return classroomRepository.findByTeacherIdOrderByCreatedAtDesc(teacherId);
    }

    public Classroom createClassroom(String teacherId, String name, String type, String configJson) {
        Classroom c = new Classroom();
        c.setId(UUID.randomUUID().toString());
        c.setTeacherId(teacherId);
        c.setName(name);
        c.setType(type);
        c.setConfig(configJson);
        c.setCreatedAt(java.time.LocalDateTime.now());
        return classroomRepository.save(c);
    }

    // ============== 默认教室初始化（与 ShopService 逻辑一致）==============
    public void initializeDefaults(String teacherId) {
        List<Classroom> existing = classroomRepository.findByTeacherIdOrderByCreatedAtDesc(teacherId);
        if (!existing.isEmpty()) return; // 已有数据，跳过

        List<Classroom> list = new ArrayList<>();

        // 语文
        Classroom chinese = new Classroom();
        chinese.setId(UUID.randomUUID().toString());
        chinese.setTeacherId(teacherId);
        chinese.setName("语文小课堂");
        chinese.setType("CHINESE");
        chinese.setConfig(toJson(Map.of("poems", DEFAULT_POEMS)));
        chinese.setCreatedAt(java.time.LocalDateTime.now());
        list.add(chinese);

        // 数学
        Classroom math = new Classroom();
        math.setId(UUID.randomUUID().toString());
        math.setTeacherId(teacherId);
        math.setName("数学小课堂");
        math.setType("MATH");
        math.setConfig(toJson(Map.of("operations", Arrays.asList("+","-","×","÷"), "maxNum", 100)));
        math.setCreatedAt(java.time.LocalDateTime.now());
        list.add(math);

        // 英语
        Classroom english = new Classroom();
        english.setId(UUID.randomUUID().toString());
        english.setTeacherId(teacherId);
        english.setName("英语小课堂");
        english.setType("ENGLISH");
        english.setConfig(toJson(Map.of()));
        english.setCreatedAt(java.time.LocalDateTime.now());
        list.add(english);

        classroomRepository.saveAll(list);
    }

    public Classroom createDefaultClassrooms(String teacherId) {
        initializeDefaults(teacherId);
        List<Classroom> list = getClassrooms(teacherId);
        return list.isEmpty() ? null : list.get(0);
    }

    public Classroom updateClassroom(String teacherId, String id, String name, String configJson) {
        Classroom c = classroomRepository.findById(id)
            .filter(x -> x.getTeacherId().equals(teacherId))
            .orElseThrow(() -> new IllegalArgumentException("教室不存在或无权限"));
        if (name != null) c.setName(name);
        if (configJson != null) c.setConfig(configJson);
        return classroomRepository.save(c);
    }

    public void deleteClassroom(String teacherId, String id) {
        Classroom c = classroomRepository.findById(id)
            .filter(x -> x.getTeacherId().equals(teacherId))
            .orElseThrow(() -> new IllegalArgumentException("教室不存在或无权限"));
        classroomRepository.delete(c);
    }

    // ============== 诗词管理 ==============
    @SuppressWarnings("unchecked")
    public Classroom addPoem(String teacherId, String classroomId, Map<String, String> poem) {
        Classroom c = classroomRepository.findById(classroomId)
            .filter(x -> x.getTeacherId().equals(teacherId))
            .orElseThrow(() -> new IllegalArgumentException("教室不存在或无权限"));
        if (!"CHINESE".equals(c.getType())) {
            throw new IllegalArgumentException("只有语文教室可以添加诗词");
        }
        try {
            Map<String, Object> config = MAPPER.readValue(c.getConfig(), Map.class);
            List<Map<String, String>> poems = (List<Map<String, String>>) config.getOrDefault("poems", new ArrayList<>());
            // 简单去重：标题相同则替换
            poems.removeIf(p -> p.get("title").equals(poem.get("title")));
            poems.add(poem);
            config.put("poems", poems);
            c.setConfig(MAPPER.writeValueAsString(config));
            return classroomRepository.save(c);
        } catch (Exception e) {
            throw new RuntimeException("添加诗词失败: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public Classroom removePoem(String teacherId, String classroomId, String title) {
        Classroom c = classroomRepository.findById(classroomId)
            .filter(x -> x.getTeacherId().equals(teacherId))
            .orElseThrow(() -> new IllegalArgumentException("教室不存在或无权限"));
        if (!"CHINESE".equals(c.getType())) {
            throw new IllegalArgumentException("只有语文教室可以删除诗词");
        }
        try {
            Map<String, Object> config = MAPPER.readValue(c.getConfig(), Map.class);
            List<Map<String, String>> poems = (List<Map<String, String>>) config.getOrDefault("poems", new ArrayList<>());
            int before = poems.size();
            poems.removeIf(p -> p.get("title").equals(title));
            if (poems.size() == before) {
                throw new IllegalArgumentException("未找到该诗词");
            }
            config.put("poems", poems);
            c.setConfig(MAPPER.writeValueAsString(config));
            return classroomRepository.save(c);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("删除诗词失败: " + e.getMessage());
        }
    }

    private String toJson(Object obj) {
        try { return MAPPER.writeValueAsString(obj); }
        catch (Exception e) { return "{}"; }
    }
}