package com.ilog.auth.service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;

/** {@link com.ilog.global.policy.MemberPolicy} 비밀번호 규칙을 만족하는 임시 비밀번호를 SecureRandom 으로 생성한다. */
@Component
public class TemporaryPasswordGenerator {

    static final int LENGTH = 12;

    private static final String LETTERS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";
    private static final String DIGITS = "23456789";
    private static final String SPECIALS = "!@#$%^&*";
    private static final String ALL = LETTERS + DIGITS + SPECIALS;

    private final SecureRandom random = new SecureRandom();

    public String generate() {
        List<Character> chars = new ArrayList<>(LENGTH);
        chars.add(pick(LETTERS));
        chars.add(pick(DIGITS));
        chars.add(pick(SPECIALS));
        while (chars.size() < LENGTH) {
            chars.add(pick(ALL));
        }
        Collections.shuffle(chars, random);

        StringBuilder password = new StringBuilder(LENGTH);
        chars.forEach(password::append);
        return password.toString();
    }

    private char pick(String source) {
        return source.charAt(random.nextInt(source.length()));
    }
}
