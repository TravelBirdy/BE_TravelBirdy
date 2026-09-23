package com.travelbird.ai.config;

import com.travelbird.ai.client.AiRecommendationClient;
import com.travelbird.global.config.InternalAiProperties;
import java.time.Clock;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

class Part2ConfigurationTest {
    private final ApplicationContextRunner clientContext = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PropertyPlaceholderAutoConfiguration.class))
            .withUserConfiguration(AiClientConfig.class)
            .withBean(RestClient.Builder.class, RestClient::builder);

    @Test
    void sharedClockUsesUtcForStoredAiTimestamps() {
        new ApplicationContextRunner().withUserConfiguration(GlobalConfiguration.class)
                .withPropertyValues("aws.s3.region=ap-northeast-2")
                .run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(Clock.class);
                    assertThat(context.getBean(Clock.class).getZone()).isEqualTo(ZoneOffset.UTC);
                });
    }

    @Test
    void clientConfigurationRegistersValidatedInternalAiProperties() {
        clientContext.withPropertyValues("app.ai.internal-key=test-only-key",
                        "app.ai.server-base-url=http://localhost:19090")
                .run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(InternalAiProperties.class)
                            .hasSingleBean(AiRecommendationClient.class);
                    assertThat(context.getBean(InternalAiProperties.class).internalKey()).isEqualTo("test-only-key");
                });
    }

    @Test
    void applicationPropertiesBindAiEnvironmentVariables() {
        clientContext.withInitializer(context -> {
                    try {
                        context.getEnvironment().getPropertySources().addLast(
                                new ResourcePropertySource(new ClassPathResource("application.properties")));
                    } catch (java.io.IOException e) {
                        throw new java.io.UncheckedIOException(e);
                    }
                })
                .withPropertyValues("AI_SERVER_BASE_URL=http://localhost:19090",
                        "TRAVELBIRD_AI_CALLBACK_KEY=test-only-key")
                .run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(AiRecommendationClient.class);
                    assertThat(context.getEnvironment().getProperty("app.ai.server-base-url"))
                            .isEqualTo("http://localhost:19090");
                    assertThat(context.getBean(InternalAiProperties.class).internalKey()).isEqualTo("test-only-key");
                });
    }

    @Test
    void blankInternalKeyStillPreventsStartup() {
        clientContext.withPropertyValues("app.ai.internal-key=",
                        "app.ai.server-base-url=http://localhost:19090")
                .run(context -> assertThat(context).hasFailed()
                        .getFailure().hasRootCauseInstanceOf(IllegalArgumentException.class));
    }

    @Test
    void missingAiServerUrlPreventsStartup() {
        clientContext.withPropertyValues("app.ai.internal-key=test-only-key")
                .run(context -> assertThat(context).hasFailed()
                        .getFailure().hasStackTraceContaining("app.ai.server-base-url"));
    }
    @Test
    void missingInternalKeyStillPreventsStartup() {
        clientContext.withPropertyValues("app.ai.server-base-url=http://localhost:19090")
                .run(context -> assertThat(context).hasFailed()
                        .getFailure().hasRootCauseInstanceOf(IllegalArgumentException.class));
    }
    @Configuration(proxyBeanMethods = false)
    @ComponentScan("com.travelbird.global.config")
    static class GlobalConfiguration {}
}
