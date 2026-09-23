package com.travelbird.support;

import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.MergedContextConfiguration;

/** Test class identity is part of the cache key: every Spring integration suite gets its own DB. */
public final class LocalMySqlContextCustomizerFactory implements ContextCustomizerFactory {
    @Override public ContextCustomizer createContextCustomizer(Class<?> testClass,
            List<ContextConfigurationAttributes> configAttributes) {
        return AnnotatedElementUtils.hasAnnotation(testClass, SpringBootTest.class)
                ? new LocalMySqlCustomizer(testClass) : null;
    }

    private record LocalMySqlCustomizer(Class<?> testClass) implements ContextCustomizer {
        @Override public void customizeContext(ConfigurableApplicationContext context,
                MergedContextConfiguration mergedConfig) {
            var settings = LocalMySqlSettings.from(System.getenv());
            var database = new LocalMySqlDatabase(settings);
            var cleanup = new Thread(() -> {
                try { database.destroy(); }
                catch (IllegalStateException exception) { System.err.println(exception.getMessage()); }
            }, "local-mysql-test-cleanup");
            Runtime.getRuntime().addShutdownHook(cleanup);
            try {
                var factory = (DefaultListableBeanFactory) context.getBeanFactory();
                factory.registerSingleton("localMySqlDatabase", database);
                factory.registerDisposableBean("localMySqlDatabase", () -> {
                    database.destroy();
                    try { Runtime.getRuntime().removeShutdownHook(cleanup); }
                    catch (IllegalStateException ignored) { /* JVM shutdown already in progress. */ }
                });
                for (String dependent : List.of("dataSource", "entityManagerFactory", "flyway", "taskScheduler")) {
                    factory.registerDependentBean("localMySqlDatabase", dependent);
                }
                TestPropertyValues.of(
                        "spring.datasource.url=" + database.jdbcUrl(),
                        "spring.datasource.username=" + settings.username(),
                        "spring.datasource.password=" + settings.password(),
                        "spring.datasource.hikari.maximum-pool-size=3",
                        "spring.flyway.url=" + database.jdbcUrl(),
                        "spring.flyway.user=" + settings.username(),
                        "spring.flyway.password=" + settings.password(),
                        "spring.flyway.schemas=" + database.schema(),
                        "spring.flyway.default-schema=" + database.schema(),
                        "spring.flyway.locations=classpath:db/migration",
                        "spring.flyway.enabled=true",
                        "spring.flyway.clean-disabled=true",
                        "spring.flyway.validate-on-migrate=true",
                        "spring.flyway.baseline-on-migrate=false",
                        "spring.jpa.hibernate.ddl-auto=validate",
                        "spring.sql.init.mode=never",
                        "app.ai.server-base-url=http://127.0.0.1:19090",
                        "app.ai.internal-key=" + UUID.randomUUID(),
                        "logging.level.root=WARN"
                ).applyTo(context);
                System.out.println("LOCAL_MYSQL_CREATED " + database.schema() + " suite=" + testClass.getSimpleName());
            } catch (RuntimeException exception) {
                database.destroy();
                Runtime.getRuntime().removeShutdownHook(cleanup);
                throw exception;
            }
        }
    }
}
