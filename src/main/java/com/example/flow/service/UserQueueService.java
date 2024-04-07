package com.example.flow.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class UserQueueService {
    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    // 대기열 등록 API
    public Mono<Boolean> registerWaitQueue(final Long userId) {
        // userId, unix timestamp
        var unixTimestamp = Instant.now().getEpochSecond();
        // redis sortedset -> ZSet
        return reactiveRedisTemplate.opsForZSet().add("user-queue", userId.toString(), unixTimestamp);
    }
}
