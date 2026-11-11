package com.ldj.service;


import com.ldj.pojo.MatchResult;

import java.util.Optional;

public interface MatchService {
    public Optional<MatchResult> match(Integer userId, String text);
}
