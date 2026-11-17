package com.ldj;

import com.ldj.service.impl.MatchMethod;
import com.ldj.common.utlis.FileStorage;
import com.ldj.common.utlis.impl.CsvBillParseImpl;
import com.ldj.common.utlis.impl.ExcelBillParseImpl;
import com.ldj.mapper.TxnMapper;
import com.ldj.pojo.MatchResult;
import com.ldj.pojo.Txn;
import com.ldj.service.*;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class FinanceWebApplicationTests {

    @Autowired
    private MatchService matchService;
    @Autowired
    private BayesService bayesService;

    @Autowired
    private LlmService llmService;

    @Autowired
    private MatchMethod matchMethod;
    @Autowired
    private CsvBillParseImpl csvBillParseImpl;

    @Test
    public void MatchServiceTest(){
        Optional<MatchResult> result = matchService.match(1, "午餐海南鸡饭");
        System.out.println(result.isPresent() ? result.get() : "没有匹配上");
    }



    @Test
    void testBayesFlow() {
        /*System.out.println("================ 1. 初始预测阶段 ================");

        // 此时如果你的数据库里已经插了初始化数据，这里应该能预测出餐饮
        // 如果你数据库是空的，这里应该返回空（因为 totalDoc == 0）
        printResult(bayesService.predict("星巴克,拿铁"));
        printResult(bayesService.predict("滴滴,打车"));

        System.out.println("\n================ 2. 模拟用户修改（反哺）阶段 ================");

        // 假设用户传了一条新账单，系统不认识，然后用户手动把它改成了“餐饮”和“交通”
        // 我们调 train 方法把新词教给系统
        bayesService.train("餐饮", "汉堡,薯条,炸鸡");
        bayesService.train("交通", "高铁,火车,公交");

        // 模拟第二次反哺，强化记忆
        bayesService.train("餐饮", "麦当劳,汉堡");

        System.out.println("\n================ 3. 反哺后再次预测阶段 ================");

        // 此时模型已经学到了新词，应该能精准预测
        printResult(bayesService.predict("想吃 汉堡 和 薯条")); // 预期: 餐饮
        printResult(bayesService.predict("坐 高铁 去 北京"));    // 预期: 交通
        printResult(bayesService.predict("麦当劳"));            // 预期: 餐饮
        printResult(bayesService.predict("星巴克"));            // 预期: 餐饮*/

//        bayesService.train("生活缴费", "保洁");
        // 测试一个完全没见过的词，看看拉普拉斯平滑能不能兜住
        System.out.println("未登录词测试：");
        printResult(bayesService.predict("保洁"));
    }



    @Test
    void testLlmClassify() {
        System.out.println("\n================ 1. 测试常规生僻账单（考察泛化分类能力） ================");
        testAndPrint("赛博朋克机械键盘", "买了个带RGB灯效的外设"); // 预期: 购物
        testAndPrint("塔斯汀", "中式汉堡");                     // 预期: 餐饮
        testAndPrint("滴滴专车", "去机场");                     // 预期: 交通

        System.out.println("\n================ 2. 测试大模型强约束（考察防废话机制） ================");
        // 故意给一些容易让模型长篇大论的文本
        testAndPrint("游戏充值", "原神抽卡648");                 // 预期: 娱乐

        System.out.println("\n================ 3. 测试降级兜底（考察系统健壮性） ================");
        // 我们传 null 或空字符串，观察模型是否依然能给出“其他”分类
        testAndPrint(null, null);                                // 预期: 其他

        System.out.println("================ 测试结束 ================\n");
    }

    /**
     * 辅助打印方法
     */
    private void testAndPrint(String merchant, String summary) {
        String m = merchant == null ? "null" : merchant;
        String s = summary == null ? "null" : summary;

        // 记录开始时间，用于测试超时逻辑
        long startTime = System.currentTimeMillis();

        Optional<String> result = llmService.classify(merchant, summary);

        long endTime = System.currentTimeMillis();

        if (result.isPresent()) {
            System.out.printf("✅ 成功！商户:[%s] -> 分类:[%s] (耗时: %d ms)%n",
                    m, result.get(), (endTime - startTime));
        } else {
            // 如果代码里的超时或解析失败被触发，就会走到这里
            System.out.printf("❌ 降级！商户:[%s] -> 触发兜底机制，返回空 (耗时: %d ms)%n",
                    m, (endTime - startTime));
        }
    }

    /**
     * 辅助打印方法
     */
    private void printResult(Optional<Map.Entry<String, Double>> result) {
        if (result.isPresent()) {
            String category = result.get().getKey();
            Double score = result.get().getValue();
            // 由于概率连乘可能会变得非常小（小数），我们保留科学计数法打印
            System.out.printf("✅ 预测成功！最可能的分类是: [%s], 得分: %e%n", category, score);
        } else {
            System.out.println("❌ 预测失败（未命中任何分类，或者没有数据）");
        }
    }


    @Resource
    private ClassificationService classificationService;

    @Test
    void testClassify() {
//        System.out.println("\n================ 场景 1：命中规则 (RULE) ================");
//        // 注意：运行前确保你数据库 user_rule 里有 '麦当劳' -> 分类ID 1
//        Txn txn1 = buildTxn(1, "滴滴", "打车");
//        classificationService.classify(txn1);
//        System.out.println("结果: " + txn1.getClassifySource() + " | 分类ID: " + txn1.getCategoryId() + " | 置信度: " + txn1.getConfidence());
//
//        System.out.println("\n================ 场景 2：命中贝叶斯 (NB) ================");
//        // 注意：确保你数据库的贝叶斯词库里有 '星巴克' -> 分类ID 1
//        Txn txn2 = buildTxn(1, "保洁", null);
//        classificationService.classify(txn2);
//        System.out.println("结果: " + txn2.getClassifySource() + " | 分类ID: " + txn2.getCategoryId() + " | 置信度: " + txn2.getConfidence());
//
//        System.out.println("\n================ 场景 3：大模型兜底 (LLM) ================");
//        // 完全生僻的账单，前两级肯定没命中，必定调用大模型
//        Txn txn3 = buildTxn(1, "购买了一张 赛博朋克 机械键盘", "带RGB灯效");
//        classificationService.classify(txn3);
//        System.out.println("结果: " + txn3.getClassifySource() + " | 分类ID: " + txn3.getCategoryId() + " | 置信度: " + txn3.getConfidence());

        System.out.println("\n================ 场景 4：大模型超时（降级验证） ================");
        // 为了验证降级，你可以去 application.yml 里把 llm.api-url 改成一个错误的地址，或者把 api-key 填错。
        // 这样大模型必定报错，触发降级。
        // 假设贝叶斯能算出一个极低分：
        Txn txn4 = buildTxn(1, "塔斯汀", null);
        classificationService.classify(txn4);
        System.out.println("结果: " + txn4.getClassifySource() + " | 分类ID: " + txn4.getCategoryId() + " | 置信度: " + txn4.getConfidence());

        System.out.println("\n================ 测试结束 ================");
    }

    @Test
    public void testTrainUserRule()
    {
        matchMethod.trainUserRuleWeight(1, "海南鸡饭", 1);
    }

    /**
     * 辅助方法：快速构建账单实体
     */
    private Txn buildTxn(Integer userId, String merchant, String summary) {
        Txn txn = new Txn();
        txn.setUserId(userId);
        txn.setCounterpartyEnc(merchant);
        txn.setSummary(summary);
        return txn;
    }


    @Resource
    private FileStorage fileStorage;

    @Test
    void testSaveAndLoad() throws Exception {
        System.out.println("\n================ 场景 1：正常上传与读取 ================");
        Integer userId = 1;
        String content = "这是一份正常的账单数据";

        // 模拟前端上传的文件
        MultipartFile file = new MockMultipartFile(
                "file",
                "bill.csv",
                "text/csv",
                content.getBytes(StandardCharsets.UTF_8)
        );

        // 1. 保存文件
        String objectKey = fileStorage.save(userId, file);
        System.out.println("文件保存成功，返回 objectKey: " + objectKey);
        assertNotNull(objectKey);
        assertTrue(objectKey.startsWith("1/"));

        // 2. 读取文件
        try (InputStream is = fileStorage.load(userId, objectKey)) {
            String readContent = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            System.out.println("文件读取成功，内容: " + readContent);
            assertEquals(content, readContent);
        }
    }

    @Test
    void testSecurity() throws Exception {
        System.out.println("\n================ 场景 2：模拟黑客越权攻击 ================");
        Integer normalUser = 1;
        Integer hackerUser = 2;

        // 1. 攻击1：跨用户读取（用户2试图读取用户1的文件）
        System.out.println("👉 攻击1：跨用户越权读取...");
        String hackerKey = "1/202311/fake.csv";
        Exception ex1 = assertThrows(RuntimeException.class, () -> {
            fileStorage.load(hackerUser, hackerKey);
        });
        System.out.println("✅ 成功拦截跨用户访问，异常信息: " + ex1.getMessage());

        // 2. 攻击2：路径穿越（试图跳出存储目录）
        System.out.println("👉 攻击2：路径穿越攻击 (../../etc/passwd)...");
        String traversalKey = "2/../../../etc/passwd";
        Exception ex2 = assertThrows(RuntimeException.class, () -> {
            fileStorage.load(hackerUser, traversalKey);
        });
        System.out.println("✅ 成功拦截路径穿越，异常信息: " + ex2.getMessage());

        // 3. 攻击3：双重伪装穿越（带有合法的 userId 前缀，但依然试图往上跳）
        System.out.println("👉 攻击3：高级伪装路径穿越...");
        String disguiseKey = "2/../../tmp/hacked.csv";
        Exception ex3 = assertThrows(RuntimeException.class, () -> {
            fileStorage.load(hackerUser, disguiseKey);
        });
        System.out.println("✅ 成功拦截高级伪装，异常信息: " + ex3.getMessage());
    }


    @Autowired
    ExcelBillParseImpl excelBillParse;

    @Test
    void testCsv()
    {
        InputStream load = fileStorage.load(1, "1/202611/test.xlsx");
        List<Txn> parse = excelBillParse.parse(load);
        parse.get(1).setUserId(1);
        System.out.println(parse.getLast());
        classificationService.classify(parse.getLast());
    }


    @Resource
    private BillImportService billImportService;

    @Resource
    private TxnMapper txnMapper;

    @Test
    void testProcessImport() {
        System.out.println("\n================ 开始测试：模拟文件导入与并发分类 ================");

        // 1. 模拟一段 CSV 文件内容（包含表头和数据）
        // 注意：这里我特意加了“麦当劳”和“星巴克”，以测试你的规则和贝叶斯引擎
        String csvContent =
                "交易时间,交易对方,商品说明,金额,收支\n" +
                        "2023-11-12 12:00:00,麦当劳(科技园店),午餐,35.00,支出\n" +
                        "2023-11-12 14:00:00,星巴克,拿铁,28.00,支出\n" +
                        "2023-11-12 18:00:00,滴滴出行,打车回家,45.50,支出\n" +
                        "2023-11-12 20:00:00,淘宝,买了一件衣服,159.00,支出\n" +
                        "2023-11-12 21:00:00,外星飞船维修中心,更换曲速引擎,99999.00,支出"; // 这条会逼出大模型兜底

        // 2. 构造一个模拟的前端上传文件对象
        MultipartFile file = new MockMultipartFile(
                "file",
                "test_bill.csv",
                "text/csv",
                csvContent.getBytes(StandardCharsets.UTF_8)
        );

        Integer testUserId = 1;

        // 3. 调用你的导入服务，并断言不会抛出异常
        assertDoesNotThrow(() -> {
            billImportService.processImport(testUserId, file, null);
        });

        System.out.println("================ 导入流程执行完毕 ================\n");

        // 4. （可选）去数据库里验证一下是否真的存进去了
        // 假设你的 TxnMapper 里有个查所有的方法，或者你可以直接去 Navicat 里看
        System.out.println("请前往数据库查看 txn 表和 raw_record 表的最新数据！");
    }

    @Resource
    private OcrService ocrService;

    /**
     * 场景一：纯本地测试 JSON 解析逻辑（不花钱、不调接口）
     */
    @Test
    void testParseJsonToTxn() {
        System.out.println("\n================ 场景1：本地 JSON 解析测试 ================");

        // 模拟阿里云返回的真实 JSON 数据（就是你刚才发我的那段）
        String jsonData = """
                {
                  "data": {
                    "recipientName": "中国铁路网络有限公司",
                    "totalAmount": "-198.00",
                    "paymentMethod": "花呗",
                    "description": "火车票",
                    "paymentTime": "2019-07-21 12:45",
                    "orderNumber": "2019072122001457521050262559"
                  }
                }
                """;

        // 调用你的解析方法
        Txn txn = ocrService.parseJsonToTxn(jsonData);

        // 断言校验
        assertNotNull(txn, "解析结果不能为空");
        System.out.println("解析结果: " + txn);

        assertEquals("中国铁路网络有限公司", txn.getCounterpartyEnc(), "商户名解析错误");
        assertEquals("198.00", txn.getAmount().toString(), "金额解析错误");
        assertEquals("EXPENSE", txn.getDirection(), "收支方向解析错误");
        assertEquals("火车票", txn.getSummary(), "摘要解析错误");
        assertEquals(2019, txn.getTxnTime().getYear(), "时间年份解析错误");
        assertEquals(45, txn.getTxnTime().getMinute(), "时间分钟解析错误");

        System.out.println("✅ 场景1通过：JSON解析完全正确！");
    }

    /**
     * 场景二：真实调用阿里云 OCR 识别（消耗免费额度）
     */
    @Test
    void testRealOcrRecognize() throws Exception {
        System.out.println("\n================ 场景2：真实图片识别测试 ================");

        // ★ 这里需要你填一张本地真实账单截图的路径（比如微信或支付宝的支付成功截图）
        String imagePath = "C:/Users/28894/Desktop/1.jpg";

        try (InputStream inputStream = new FileInputStream(imagePath)) {
            // 调用你写的 recognizeImage 方法
            Txn txn = ocrService.recognizeImage(inputStream);

            assertNotNull(txn, "OCR 识别结果不能为空");
            System.out.println("真实 OCR 识别结果: " + txn);

            // 打印出核心字段看看对不对
            System.out.println("提取的商户: " + txn.getCounterpartyEnc());
            System.out.println("提取的金额: " + txn.getAmount());
            System.out.println("提取的时间: " + txn.getTxnTime());

            System.out.println("✅ 场景2通过：阿里云 API 调用成功！");
        }
    }


    @Resource
    private TxnService txnService;


    @Test
    void testUpdateCategoryAndFeedback() {
        System.out.println("\n================ 开始测试：用户修改分类与闭环反哺 ================");

        // 1. 前提准备：假设数据库里已经有一条待校对的账单（你可以先手动去数据库看一眼 userId=1 的最新记录）
        Integer testUserId = 1;
        Integer targetCategoryId = 1; // 假设我们要把它改到分类ID 2 (交通)

        // 查一下当前这条账单的状态，方便对比
        Txn txnBefore = txnMapper.selectById(testUserId);
        if (txnBefore == null) {
            System.out.println("⚠️ 数据库中找不到 userId=" + testUserId + " 的账单，请先导入一条测试数据！");
            return;
        }

        System.out.println("修改前 - 账单ID: " + txnBefore.getId() +
                " | 商户: " + txnBefore.getCounterpartyEnc() +
                " | 当前分类ID: " + txnBefore.getCategoryId());

        // 2. 执行修改与反哺操作
        assertDoesNotThrow(() -> {
            // 这里调用的就是你写的核心方法
            txnService.updateCategoryById(testUserId, targetCategoryId);
        });

        // 3. 验证结果：去数据库里查，主表是否更新，反哺是否生效
        Txn txnAfter = txnMapper.selectById(testUserId);
        assertNotNull(txnAfter);

        System.out.println("修改后 - 账单ID: " + txnAfter.getId() +
                " | 新分类ID: " + txnAfter.getCategoryId() +
                " | 修改来源: " + txnAfter.getClassifySource() +
                " | 状态: " + txnAfter.getStatus());

        // 简单断言一下状态是否变成了 MANUAL
        if ("MANUAL".equals(txnAfter.getClassifySource()) && "CONFIRMED".equals(txnAfter.getStatus())) {
            System.out.println("✅ 测试通过：账单主表状态更新成功！");
        } else {
            System.out.println("❌ 测试失败：主表状态未正确更新！");
        }

        System.out.println("\n================ 提示 ================");
        System.out.println("请立即前往数据库查看以下两张表：");
        System.out.println("1. user_rule 表：看看特征词对应的分类是否更新，或者是否新增了规则。");
        System.out.println("2. bayes_word_freq 表：看看对应词汇的频次(freq_count)是否 +1。");
    }
}
