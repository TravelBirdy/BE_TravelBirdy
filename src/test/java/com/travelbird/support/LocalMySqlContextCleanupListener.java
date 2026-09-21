package com.travelbird.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

/** Close the whole context (including scheduled workers and pools) after each suite. */
public final class LocalMySqlContextCleanupListener extends AbstractTestExecutionListener {
    @Override public void afterTestClass(TestContext testContext) {
        if (AnnotatedElementUtils.hasAnnotation(testContext.getTestClass(), SpringBootTest.class)
                && testContext.hasApplicationContext()) {
            var database = testContext.getApplicationContext().getBean(LocalMySqlDatabase.class);
            testContext.markApplicationContextDirty(HierarchyMode.EXHAUSTIVE);
            // Spring logs destroy-callback errors; an explicit retry makes persistent cleanup failure fail the suite.
            database.destroy();
        }
    }
}
