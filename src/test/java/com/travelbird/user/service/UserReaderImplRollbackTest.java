package com.travelbird.user.service;

import static org.assertj.core.api.Assertions.assertThatCode;

import com.travelbird.global.error.BusinessException;
import com.travelbird.user.api.UserReader;
import com.travelbird.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.SmartTransactionObject;

/**
 * {@link UserReaderImpl#getUserSummary}가 던진 {@link BusinessException}을 호출부가
 * catch로 삼켜도, 바깥 트랜잭션이 rollback-only로 남지 않는지 검증한다(PR#21 리뷰에서
 * 재현한 문제 — {@code getUserSummary}가 일반 {@code @Transactional}이면, 프록시를 통해
 * 호출되는 이상 예외를 삼켜도 트랜잭션 매니저가 rollback-only를 표시해서 커밋 시점에
 * {@code UnexpectedRollbackException}이 난다). 실제 DB 없이 트랜잭션 전파 규칙만으로
 * 재현하기 위해 최소한의 가짜 {@link PlatformTransactionManager}를 쓴다.
 */
@SpringJUnitConfig(UserReaderImplRollbackTest.Config.class)
class UserReaderImplRollbackTest {

    static class Tx implements SmartTransactionObject {
        boolean rollbackOnly;

        @Override
        public boolean isRollbackOnly() {
            return rollbackOnly;
        }

        @Override
        public void flush() {
        }
    }

    static class FakeTransactionManager extends AbstractPlatformTransactionManager {
        private static final ThreadLocal<Tx> CURRENT = new ThreadLocal<>();

        @Override
        protected Object doGetTransaction() {
            Tx tx = CURRENT.get();
            return tx != null ? tx : new Tx();
        }

        @Override
        protected boolean isExistingTransaction(Object transaction) {
            return CURRENT.get() == transaction;
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
            CURRENT.set((Tx) transaction);
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
            CURRENT.remove();
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
            CURRENT.remove();
        }

        @Override
        protected void doSetRollbackOnly(DefaultTransactionStatus status) {
            ((Tx) status.getTransaction()).rollbackOnly = true;
        }
    }

    static class Caller {
        private final UserReader userReader;

        Caller(UserReader userReader) {
            this.userReader = userReader;
        }

        @Transactional
        String callAndSwallow() {
            try {
                userReader.getUserSummary(999L);
            } catch (BusinessException e) {
                // 커뮤니티/저장경로 카드 조립부가 실제로 하는 것과 동일하게, 이 항목만
                // placeholder로 빼고 나머지는 정상 처리한다는 가정.
            }
            return "ok";
        }
    }

    @Configuration
    @EnableTransactionManagement
    static class Config {

        @Bean
        PlatformTransactionManager transactionManager() {
            return new FakeTransactionManager();
        }

        @Bean
        UserRepository userRepository() {
            UserRepository repo = Mockito.mock(UserRepository.class);
            Mockito.when(repo.findById(999L)).thenReturn(java.util.Optional.empty());
            return repo;
        }

        @Bean
        UserReader userReader(UserRepository userRepository) {
            return new UserReaderImpl(userRepository);
        }

        @Bean
        Caller caller(UserReader userReader) {
            return new Caller(userReader);
        }
    }

    @Autowired
    private Caller caller;

    @Test
    void 삼킨_예외가_바깥_트랜잭션을_rollback_only로_만들지_않는다() {
        assertThatCode(() -> caller.callAndSwallow()).doesNotThrowAnyException();
    }
}
