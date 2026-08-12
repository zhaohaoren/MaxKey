package org.dromara.maxkey;

import org.dromara.maxkey.web.WebContext;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MaxKeyUnifiedApplicationTest {

    @Test
    void initializesSharedWebContextAfterSpringStarts() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().getPropertySources().addFirst(
                    new MapPropertySource("test", Map.of("server.port", "19527")));
            context.registerBean(PropertySourcesPlaceholderConfigurer.class);
            context.refresh();

            MaxKeyUnifiedApplication.initializeContext(context);

            assertEquals("19527", WebContext.getServerPort());
        }
    }
}
