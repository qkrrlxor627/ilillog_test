package com.ilog.auth.service;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * prod 용 자리표시자. 전달 수단이 정해지기 전까지는 예외를 던져 트랜잭션을 롤백한다. (전달되지 않은 임시 비밀번호로 회원 비밀번호가 바뀌는 것을 막음)
 *
 * <p>TODO: 메일 발송 등 실제 전달 수단 확정 시 교체. 평문 비밀번호를 로그에 남기지 않는다.
 */
@Component
@Profile("prod")
public class UnsupportedTemporaryPasswordSender implements TemporaryPasswordSender {

    @Override
    public void send(String email, String temporaryPassword) {
        throw new IllegalStateException("임시 비밀번호 전달 수단이 아직 구성되지 않았습니다.");
    }
}
