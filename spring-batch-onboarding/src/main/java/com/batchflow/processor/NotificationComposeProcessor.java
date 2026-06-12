package com.batchflow.processor;

import com.batchflow.domain.Member;
import com.batchflow.domain.Notification;
import org.springframework.batch.item.ItemProcessor;

/**
 * 휴면 알림 조립 Processor (심화 Step 16).
 *
 * 외부 발송 API 호출을 흉내내는 고정 지연(LATENCY_MS)을 갖는다 —
 * "Processor가 느린 배치"의 표준 시나리오. 읽기/쓰기는 빠른데
 * 가공(외부 호출·암복호화·이미지 처리)이 병목인 경우, 비동기 처리의 무대가 된다.
 *
 * 동기 Job과 비동기 Job이 "같은 Processor"를 쓰는 것이 비교 실험의 전제다 —
 * 변인은 오직 실행 방식 하나여야 한다.
 */
public class NotificationComposeProcessor implements ItemProcessor<Member, Notification> {

    /** 건당 의사 지연 — 외부 알림 API의 왕복 시간 흉내 */
    public static final long LATENCY_MS = 20L;

    @Override
    public Notification process(Member member) throws Exception {
        Thread.sleep(LATENCY_MS);   // 외부 발송 API 호출 흉내 (테스트 결정성을 위한 고정값)
        return Notification.dormantNotice(member);
    }
}
