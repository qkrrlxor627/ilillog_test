package com.ilog.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ilog.global.policy.MemberPolicy;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

class TemporaryPasswordGeneratorTest {

    private final TemporaryPasswordGenerator generator = new TemporaryPasswordGenerator();

    @RepeatedTest(200)
    @DisplayName("생성한 임시 비밀번호는 항상 비밀번호 정책을 만족한다")
    void generate_always_satisfiesPasswordPolicy() {
        // when
        String password = generator.generate();

        // then
        assertThat(password).hasSize(TemporaryPasswordGenerator.LENGTH);
        assertThat(MemberPolicy.isValidPassword(password)).isTrue();
    }

    @Test
    @DisplayName("생성할 때마다 다른 값이 나온다")
    void generate_repeatedly_returnsDistinctValues() {
        // given
        Set<String> passwords = new HashSet<>();

        // when
        for (int i = 0; i < 100; i++) {
            passwords.add(generator.generate());
        }

        // then
        assertThat(passwords).hasSize(100);
    }
}
