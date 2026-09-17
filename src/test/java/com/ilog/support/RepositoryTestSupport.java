package com.ilog.support;

import com.ilog.global.config.ClockConfig;
import com.ilog.global.config.JpaAuditingConfig;
import com.ilog.support.container.PostgresTestContainer;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** 리포지토리 테스트 공통 설정. 실제 PostgreSQL + Flyway 마이그레이션 + ddl validate. 각 테스트는 롤백된다. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaAuditingConfig.class, ClockConfig.class})
@ActiveProfiles("test")
public abstract class RepositoryTestSupport {

    @ServiceConnection static final PostgreSQLContainer POSTGRES = PostgresTestContainer.INSTANCE;
}
