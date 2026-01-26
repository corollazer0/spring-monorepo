package com.batchflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * BatchFlow 온보딩 프로젝트 메인 애플리케이션
 *
 * Spring Batch를 학습하기 위한 온보딩 프로젝트입니다.
 * 50개의 Step을 통해 Batch 처리의 기초부터 실전까지 학습합니다.
 */
@SpringBootApplication
public class BatchFlowApplication {
    public static void main(String[] args) {
        SpringApplication.run(BatchFlowApplication.class, args);
    }
}
