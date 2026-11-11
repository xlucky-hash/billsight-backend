package com.ldj.service;

import java.util.Optional;

public interface LlmService {

    public Optional<String> classify(String merchant, String summary);

    public String parseContentFromJson(String json);
}
