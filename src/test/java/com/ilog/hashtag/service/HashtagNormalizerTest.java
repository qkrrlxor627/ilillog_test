package com.ilog.hashtag.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ilog.global.error.BusinessException;
import com.ilog.global.error.ErrorCode;
import com.ilog.global.error.ErrorResponse;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class HashtagNormalizerTest {

    @ParameterizedTest
    @CsvSource({"JWT,JWT", "#JWT,JWT", "'  #Spring ',Spring", "##JPA,JPA", "'# 공백 ',공백", "jwt,jwt"})
    @DisplayName("앞뒤 공백과 앞의 # 을 제거하고 대소문자는 그대로 둔다")
    void normalizeName_stripsHashAndWhitespace(String raw, String expected) {
        // when & then
        assertThat(HashtagNormalizer.normalizeName(raw)).isEqualTo(expected);
    }

    @Test
    @DisplayName("등록용 정규화는 요청 안의 중복을 입력 순서대로 하나로 합친다")
    void normalizeForRegistration_deduplicatesKeepingOrder() {
        // when
        List<String> names =
                HashtagNormalizer.normalizeForRegistration(
                        List.of("Spring", "#JWT", "Spring", "jwt"), "names");

        // then
        assertThat(names).containsExactly("Spring", "JWT", "jwt");
    }

    @Test
    @DisplayName("등록용 정규화에 null 이 오면 빈 목록이다")
    void normalizeForRegistration_null_returnsEmpty() {
        // when & then
        assertThat(HashtagNormalizer.normalizeForRegistration(null, "names")).isEmpty();
    }

    @Test
    @DisplayName("정규화 후 비어 있으면 지정한 필드명으로 INVALID_INPUT 예외가 발생한다")
    void normalizeForRegistration_blankAfterNormalize_throws() {
        // when & then
        assertThatThrownBy(
                        () -> HashtagNormalizer.normalizeForRegistration(List.of("# "), "hashtags"))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        e -> {
                            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT);
                            assertThat(e.getFieldErrors())
                                    .extracting(ErrorResponse.FieldError::field)
                                    .containsExactly("hashtags");
                        });
    }

    @Test
    @DisplayName("null 원소도 비어 있는 태그로 보고 거절한다")
    void normalizeForRegistration_nullElement_throws() {
        // when & then
        assertThatThrownBy(
                        () ->
                                HashtagNormalizer.normalizeForRegistration(
                                        Arrays.asList("JWT", null), "names"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("정규화 후 50자를 넘으면 INVALID_INPUT 예외가 발생한다")
    void normalizeForRegistration_tooLong_throws() {
        // given
        String longName = "#" + "a".repeat(51);

        // when & then
        assertThatThrownBy(
                        () ->
                                HashtagNormalizer.normalizeForRegistration(
                                        List.of(longName), "names"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    @DisplayName("검색용 정규화는 비어 있는 값을 조용히 무시한다")
    void normalizeForSearch_ignoresBlank() {
        // when
        List<String> names =
                HashtagNormalizer.normalizeForSearch(Arrays.asList("#JWT", "", "#", null, "JWT"));

        // then
        assertThat(names).containsExactly("JWT");
        assertThat(HashtagNormalizer.normalizeForSearch(null)).isEmpty();
    }
}
