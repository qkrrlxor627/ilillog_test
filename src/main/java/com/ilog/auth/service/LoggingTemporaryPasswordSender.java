package com.ilog.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** local/dev/test 전용: 임시 비밀번호를 로그로 출력한다. prod 에서는 절대 활성화되지 않는다. */
@Slf4j
@Component
@Profile("!prod")
public class LoggingTemporaryPasswordSender implements TemporaryPasswordSender {

    @Override
    public void send(String email, String temporaryPassword) {
        log.info(
                "[임시 비밀번호 발급 - 비운영 환경 전용] email={}, temporaryPassword={}",
                email,
                temporaryPassword);
    }
}
