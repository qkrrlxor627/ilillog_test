package com.ilog.auth.service;

/**
 * 임시 비밀번호 전달 수단.
 *
 * <p>TODO: 전달 방식(메일 발송 vs 화면 노출)은 명세에 없음. 확정되면 구현체를 추가한다.
 */
public interface TemporaryPasswordSender {

    void send(String email, String temporaryPassword);
}
