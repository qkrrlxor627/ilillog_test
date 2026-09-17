package com.ilog.post.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

class PostSearchRequestTest {

    private static final ValidatorFactory VALIDATOR_FACTORY =
            Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = VALIDATOR_FACTORY.getValidator();

    @AfterAll
    static void closeValidator() {
        VALIDATOR_FACTORY.close();
    }

    @Test
    @DisplayName("파라미터가 없으면 0페이지, 10개, 작성일 내림차순이다")
    void toPageable_defaults() {
        // when
        Pageable pageable = PostSearchRequest.empty().toPageable();

        // then
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Test
    @DisplayName("정렬 방향은 대소문자를 구분하지 않는다")
    void toPageable_sortDirectionIgnoreCase() {
        // given
        PostSearchRequest request =
                new PostSearchRequest(null, null, null, null, null, 2, 20, "title,ASC");

        // when
        Pageable pageable = request.toPageable();

        // then
        assertThat(pageable.getPageNumber()).isEqualTo(2);
        assertThat(pageable.getPageSize()).isEqualTo(20);
        assertThat(pageable.getSort()).isEqualTo(Sort.by(Sort.Direction.ASC, "title"));
        assertThat(VALIDATOR.validate(request)).isEmpty();
    }

    @Test
    @DisplayName("방향 없이 필드만 주면 내림차순이다")
    void toPageable_sortWithoutDirection_desc() {
        // given
        PostSearchRequest request =
                new PostSearchRequest(null, null, null, null, null, null, null, "title");

        // when & then
        assertThat(request.toPageable().getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "title"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"id,desc", "content", "createdAt,up", "createdAt;drop table"})
    @DisplayName("허용하지 않은 정렬은 검증에 실패한다")
    void validate_unsupportedSort_fails(String sort) {
        // given
        PostSearchRequest request =
                new PostSearchRequest(null, null, null, null, null, null, null, sort);

        // when
        Set<ConstraintViolation<PostSearchRequest>> violations = VALIDATOR.validate(request);

        // then
        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .containsExactly("sort");
    }

    @Test
    @DisplayName("페이지 크기가 최대값을 넘거나 페이지가 음수면 검증에 실패한다")
    void validate_invalidPaging_fails() {
        // given
        PostSearchRequest request =
                new PostSearchRequest(null, null, null, null, null, -1, 51, null);

        // when
        Set<ConstraintViolation<PostSearchRequest>> violations = VALIDATOR.validate(request);

        // then
        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .containsExactlyInAnyOrder("page", "size");
    }
}
