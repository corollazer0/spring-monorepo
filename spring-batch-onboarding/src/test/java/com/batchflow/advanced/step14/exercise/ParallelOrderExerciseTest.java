package com.batchflow.advanced.step14.exercise;

import com.batchflow.config.TestBatchConfig;
import com.batchflow.job.parallel.MultiThreadScanJobConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * [심화 Step 14 — exercise] 병렬 테스트에서 "단정할 수 있는 것"을 가려내세요
 *
 * 진행 방법:
 * 1. 클래스 위의 @Disabled 를 지운다
 * 2. TODO를 채운다 (example 두 클래스를 참고)
 * 3. .\gradlew :spring-batch-onboarding:test 로 통과를 확인한다
 *
 * 핵심 질문: 병렬 실행에서 결정적인 것(카운트 합계)과
 * 비결정적인 것(처리 순서, 스레드 배정)을 구분할 수 있는가?
 * 비결정적인 것을 단정(assert)하면 — 가끔만 실패하는 최악의 테스트가 된다.
 */
@Disabled("과제: docs/batch/education/FOR-BatchFlow-Step14.md 참고 후 @Disabled를 제거하고 완성하세요")
@SpringBatchTest
@SpringBootTest(classes = {MultiThreadScanJobConfig.class, TestBatchConfig.class})
@DisplayName("병렬과 결정성 (연습문제)")
class ParallelOrderExerciseTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    @BeforeEach
    void setUp() {
        jobRepositoryTestUtils.removeJobExecutions();
        MultiThreadScanJobConfig.SEEN_THREADS.clear();
    }

    @Test
    @DisplayName("두 번 실행해도 카운트는 항상 같다 (결정적인 것)")
    void multiThread_반복실행_카운트결정적() throws Exception {
        // when : Job을 두 번 실행하고 (unique 파라미터!) 각 READ_COUNT를 비교하세요
        // TODO 1

        // then : 두 실행 모두 READ_COUNT가 50으로 같은지 검증하세요
        //        (스레드 배정은 매번 달라도, 합계는 흔들리면 안 된다)
        // TODO 2
    }

    @Test
    @DisplayName("스레드 사용 개수는 상한(4) 이내다 (단정 가능한 경계)")
    void multiThread_스레드수_상한이내() throws Exception {
        // when : Job을 실행하세요
        // TODO 3

        // then : SEEN_THREADS 크기가 1 이상 4 이하인지 검증하세요
        //        ("정확히 4"라고 단정하면 안 되는 이유를 주석으로 적으세요!)
        // TODO 4
    }
}
