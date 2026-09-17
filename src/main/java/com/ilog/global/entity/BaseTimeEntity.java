package com.ilog.global.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 생성·수정 시각 공통 매핑.
 *
 * <p>{@code createdAt} 은 JPA Auditing 이 기록하고, {@code updatedAt} 은 수정 도메인 메서드가 서비스에서 받은 시각으로 직접
 * 갱신한다. (자식 컬렉션만 바뀌는 수정에서도 확실히 갱신되고, 생성 직후에는 null 로 남는다.)
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseTimeEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected void markUpdated(Instant now) {
        this.updatedAt = now;
    }
}
