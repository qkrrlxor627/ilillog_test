package com.ilog.auth.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TemporaryPasswordSenderTest {

    @Test
    @DisplayName("비운영 환경 구현체는 예외 없이 로그로 전달한다")
    void loggingSender_send_doesNotThrow() {
        // when & then
        assertThatCode(() -> new LoggingTemporaryPasswordSender().send("a@b.com", "Temp1234!"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("운영 환경 자리표시자는 전달 수단이 없으므로 예외를 던져 트랜잭션을 롤백시킨다")
    void unsupportedSender_send_throws() {
        // when & then
        assertThatThrownBy(
                        () -> new UnsupportedTemporaryPasswordSender().send("a@b.com", "Temp1234!"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageNotContaining("Temp1234!");
    }
}
