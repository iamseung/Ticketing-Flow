package com.example.flow.service;

import com.example.flow.EmbeddedRedis;
import com.example.flow.exception.ApplicationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.ReactiveRedisConnection;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(EmbeddedRedis.class)
@ActiveProfiles("test")
class UserQueueServiceTest {
    @Autowired
    private UserQueueService userQueueService;

    @Autowired
    private ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    // 테스트 직전에 데이터 정리
    @BeforeEach
    public void beforeEach() {
        ReactiveRedisConnection redisConnection = reactiveRedisTemplate.getConnectionFactory().getReactiveConnection();
        redisConnection.serverCommands().flushAll().subscribe();
    }

    @Test
    void registerWaitQueue() {
        StepVerifier.create(userQueueService.registerWaitQueue("default", 100L))
                .expectNext(1L)
                .verifyComplete();

        StepVerifier.create(userQueueService.registerWaitQueue("default", 101L))
                .expectNext(2L)
                .verifyComplete();

        StepVerifier.create(userQueueService.registerWaitQueue("default", 102L))
                .expectNext(3L)
                .verifyComplete();
    }

    @Test
    void alreadyRegisterWaitQueue() {
        StepVerifier.create(userQueueService.registerWaitQueue("default", 100L))
                .expectNext(1L)
                .verifyComplete();

        // 중복된 유저가 대기열 신청 시, 에러 발생
        StepVerifier.create(userQueueService.registerWaitQueue("default", 100L))
                .expectError(ApplicationException.class)
                .verify();
    }

    @Test
    void emptyAllowUser() {
        // 등록된 사용자가 없기에 0 return
        StepVerifier.create(userQueueService.allowUser("default", 3L))
                .expectNext(0L)
                .verifyComplete();
    }

    @Test
    void allowUser() {
        StepVerifier.create(userQueueService.registerWaitQueue("default", 100L)
                .then(userQueueService.registerWaitQueue("default", 101L))
                .then(userQueueService.registerWaitQueue("default", 102L))
                .then(userQueueService.allowUser("default", 2L)))
                .expectNext(2L)
                .verifyComplete();
    }

    @Test
    void allowUser2() {
        // 3명의 등록, 5명 허용 시 3명 전원 허용됨
        StepVerifier.create(userQueueService.registerWaitQueue("default", 100L)
                        .then(userQueueService.registerWaitQueue("default", 101L))
                        .then(userQueueService.registerWaitQueue("default", 102L))
                        .then(userQueueService.allowUser("default", 5L)))
                .expectNext(3L)
                .verifyComplete();
    }

    @Test
    void allowUserAfterRegisterWaitQueue() {
        // 3명 등록, 3명 허용, 다시 한명 등록 시 1을 return
        StepVerifier.create(userQueueService.registerWaitQueue("default", 100L)
                        .then(userQueueService.registerWaitQueue("default", 101L))
                        .then(userQueueService.registerWaitQueue("default", 102L))
                        .then(userQueueService.allowUser("default", 3L))
                        .then(userQueueService.registerWaitQueue("default", 200L)))
                .expectNext(1L)
                .verifyComplete();
    }

    @Test
    void isNotAllowed() {
        // 대기열이 빈 상태에서 허용시 false 를 return
        StepVerifier.create(userQueueService.isAllowed("default", 100L))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void isNotAllowed2() {
        // 100번 허용, 101번 진입이 가능한지 조회했을 경우 false 를 return
        StepVerifier.create(userQueueService.registerWaitQueue("default", 100L)
                .then(userQueueService.allowUser("default", 3L))
                .then(userQueueService.isAllowed("default", 101L)))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void isNotAllowed3() {
        // 100번 허용, 101번 진입이 가능한지 조회했을 경우 false 를 return
        StepVerifier.create(userQueueService.registerWaitQueue("default", 100L)
                        .then(userQueueService.allowUser("default", 3L))
                        .then(userQueueService.isAllowed("default", 100L)))
                .expectNext(true)
                .verifyComplete();
    }
}