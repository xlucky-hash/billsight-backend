package com.ldj.service;

import java.util.Map;
import java.util.Optional;

public interface BayesService {
    public void load();

    public void train(String category, String text);

    public Optional<Map.Entry<String, Double>> predict(String text);
}
