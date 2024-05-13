package com.fastcampust.flow.service;

import com.fastcampust.flow.EmbeddedRedis;
import com.fastcampust.flow.exception.ApplicationException;
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
@Import({EmbeddedRedis.class})
@ActiveProfiles("test")
class UserQueueServiceTest {
    @Autowired
    private UserQueueService userQueueService;

    @Autowired
    private ReactiveRedisTemplate reactiveRedisTemplate;

    @BeforeEach
    public void beforeEach() {
        ReactiveRedisConnection reactiveRedisConnection = reactiveRedisTemplate.getConnectionFactory().getReactiveConnection();
        reactiveRedisConnection.serverCommands().flushAll().subscribe();
    }

    @Test
    void registerWaitQueue() {
        StepVerifier.create(userQueueService.registerWaitQueue("default",100L))
                .expectNext(1L)
                .verifyComplete();

        StepVerifier.create(userQueueService.registerWaitQueue("default",101L))
                .expectNext(2L)
                .verifyComplete();

        StepVerifier.create(userQueueService.registerWaitQueue("default",102L))
                .expectNext(3L)
                .verifyComplete();
    }

    @Test
    void alreadyRegisterWaitQueue() {
        StepVerifier.create(userQueueService.registerWaitQueue("default",102L))
                .expectNext(1L)
                .verifyComplete();

        StepVerifier.create(userQueueService.registerWaitQueue("default",102L))
                .expectError(ApplicationException.class)
                .verify();
    }

    @Test
    void emptyAllowUser() {
        StepVerifier.create(userQueueService.allowUser("default",3L))
                .expectNext(1L)
                .verifyComplete();
    }

    @Test
    void allowUser() {
        StepVerifier.create(userQueueService.registerWaitQueue("default",1L)
                    .then(userQueueService.registerWaitQueue("default",2L))
                    .then(userQueueService.registerWaitQueue("default",3L))
                    .then(userQueueService.allowUser("default",3L)))
                .expectNext(3L)
                .verifyComplete();
    }

    @Test
    void isAllowed() {
        StepVerifier.create(userQueueService.registerWaitQueue("default",1L)
                        .then(userQueueService.registerWaitQueue("default",2L))
                        .then(userQueueService.registerWaitQueue("default",3L))
                        .then(userQueueService.allowUser("default",3L))
                        .then(userQueueService.isAllowed("default",1L)))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void getRank() {
        StepVerifier.create(userQueueService.registerWaitQueue("default",1L)
                        .then(userQueueService.getRank("default",1L)))
                .expectNext(1L)
                .verifyComplete();
    }

    @Test
    void generateToken() {
        StepVerifier.create(userQueueService.generateToken("default",100L))
                .expectNext("d333a5d4eb24f3f5cdd767d79b8c01aad3cd73d3537c70dec430455d37afe4b8")
                .verifyComplete();
    }

    @Test
    void isAllowedByToken() {
        StepVerifier.create(userQueueService.isAllowedByToken("default",100L, "d333a5d4eb24f3f5cdd767d79b8c01aad3cd73d3537c70dec430455d37afe4b8"))
                .expectNext(true)
                .verifyComplete();
    }
}