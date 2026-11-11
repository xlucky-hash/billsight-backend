package com.ldj.service.impl;

import com.ldj.service.LlmService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class LlmServiceImpl implements LlmService {

    @Value("${llm.api-key}")
    private String apiKey;

    @Value("${llm.api-url}")
    private String apiUrl;

    @Value("${llm.model-name}")
    private String modelName;

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public LlmServiceImpl()
    {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(8));

        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .build();
    }

    @Override
    public Optional<String> classify(String merchant, String summary) {
        String text = "商户: " + (merchant == null ? "" : merchant) + ", 摘要: " + (summary == null ? "" : summary);
        log.info("触发大模型兜底，待分类文本: {}", text);

        String prompt = String.format(
                "你是一个账单分类助手。请根据以下交易信息，从[餐饮, 交通, 购物, 娱乐, 生活缴费, 其他]中选择一个最合适的分类。\n" +
                        "要求：只返回一个分类名称，不要返回任何标点符号或其他废话。\n" +
                        "交易信息：%s", text);

        try {
            Map<String, Object> requestBody = Map.of(
                    "model", modelName,
                    "messages", List.of(Map.of("role", "user", "content", prompt)),
                    "temperature", 0.1
            );

            String response = restClient.post()
                    .uri(apiUrl)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            // 3. ★ 修复：如果解析失败会抛出异常，由外层 catch 统一捕获转为 empty
            return Optional.of(parseContentFromJson(response));

        } catch (Exception e) {
            log.warn("大模型调用超时或失败，触发降级: {}", e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public String parseContentFromJson(String json) {
        try {
            JsonNode rootNode = objectMapper.readTree(json);
            JsonNode choicesNode = rootNode.path("choices");

            // 4. ★ 修复：严格校验 choices 是否是数组且不为空，防止 NPE
            if (!choicesNode.isArray() || choicesNode.isEmpty()) {
                throw new RuntimeException("大模型返回的 JSON 中缺少有效的 choices 数组");
            }

            // 安全提取 content（哪怕 message 缺失也不会 NPE，会提取到空字符串）
            String rawContent = choicesNode.get(0).path("message").path("content").asText().trim();
            log.info("大模型原始返回内容: [{}]", rawContent);

            if (rawContent.isEmpty()) {
                throw new RuntimeException("大模型返回的 content 为空");
            }

            // 3. ★ 修复：数据清洗（先去标点空格，精确匹配，匹配不到再降级为 Other）
            // 去除所有标点符号和空白字符
            String cleaned = rawContent.replaceAll("[\\p{P}\\p{S}\\s]", "");

            // 优先精确匹配（处理带空格、带标点的情况）
            List<String> validCategories = List.of("餐饮", "交通", "购物", "娱乐", "生活缴费", "其他");
            if (validCategories.contains(cleaned)) {
                return cleaned;
            }

            // 精确匹配失败，退回包含匹配（仅用于容错，比如"分类是餐饮"）
            for (String validCat : validCategories) {
                if (cleaned.contains(validCat)) {
                    return validCat;
                }
            }

            // 模型胡说八道，返回了未知分类，我们把它当作格式错误抛出异常，交给外层兜底
            throw new RuntimeException("大模型返回了非法分类词: " + rawContent);

        } catch (Exception e) {
            // 将解析异常向上抛出，让 classify 的 catch 捕获并转为 Optional.empty()
            log.error("解析大模型 JSON 失败: {}", e.getMessage());
            throw new RuntimeException("大模型解析异常", e);
        }
    }
}
