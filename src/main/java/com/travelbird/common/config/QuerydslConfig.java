package com.travelbird.common.config;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 최초 QueryDSL 도입(공통협의 부록 "복합 동적 조회"). 이 코드베이스의 다른 도메인은
 * 전부 JPQL/native {@code @Query}로 충분해서 지금까지 미사용이었고, 커뮤니티 검색(§3.9.3)의
 * 여러 optional 필터 + 다단계 가중치 정렬이 QueryDSL이 필요한 첫 사례다.
 */
@Configuration
public class QuerydslConfig {

    @PersistenceContext
    private EntityManager entityManager;

    @Bean
    public JPAQueryFactory jpaQueryFactory() {
        return new JPAQueryFactory(entityManager);
    }
}
