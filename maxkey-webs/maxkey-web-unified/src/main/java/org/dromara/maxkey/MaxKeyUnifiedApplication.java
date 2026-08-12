package org.dromara.maxkey;

import org.dromara.maxkey.web.ProductEnvironment;
import org.dromara.maxkey.web.WebContext;
import org.joda.time.DateTime;
import org.mybatis.spring.annotation.MapperScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;

@SpringBootApplication
@MapperScan("org.dromara.maxkey.persistence.mapper")
public class MaxKeyUnifiedApplication {


    static final Logger _logger = LoggerFactory.getLogger(MaxKeyUnifiedApplication.class);

    public static void main(String[] args) {
        _logger.info("Start MaxKey Application ...");
        ProductEnvironment.listEnvVars();

        SpringApplication application = new SpringApplication(MaxKeyUnifiedApplication.class);
        application.setApplicationStartup(new BufferingApplicationStartup(2048));
        application.run(args);

        _logger.info("MaxKey at {}", new DateTime());
        _logger.info("MaxKey Server Port {}", WebContext.getServerPort());
        _logger.info("MaxKey started.");
    }
}
