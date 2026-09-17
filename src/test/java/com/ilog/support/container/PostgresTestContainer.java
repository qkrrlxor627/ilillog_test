package com.ilog.support.container;

import org.testcontainers.postgresql.PostgreSQLContainer;

/** 모든 테스트 클래스가 재사용하는 싱글턴 PostgreSQL 컨테이너. JVM 당 한 번만 기동한다. (종료는 Ryuk 가 담당) */
public final class PostgresTestContainer {

    public static final PostgreSQLContainer INSTANCE = start();

    private PostgresTestContainer() {}

    private static PostgreSQLContainer start() {
        PostgreSQLContainer container = new PostgreSQLContainer("postgres:17-alpine");
        container.start();
        return container;
    }
}
