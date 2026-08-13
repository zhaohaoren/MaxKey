package org.dromara.maxkey;

import org.dromara.maxkey.web.InitializeContext;
import org.dromara.maxkey.web.ProductEnvironment;
import org.dromara.maxkey.web.WebContext;
import org.joda.time.DateTime;
import org.mybatis.spring.annotation.MapperScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.web.context.request.RequestContextListener;

@SpringBootApplication
@MapperScan("org.dromara.maxkey.persistence.mapper")
public class MaxKeyUnifiedApplication {


    static final Logger _logger = LoggerFactory.getLogger(MaxKeyUnifiedApplication.class);

    /**
     * 绑定当前 HTTP 请求，供 WebContext 在过滤器和服务层读取 Request/Session。
     * 原 WAR 部署通过 web.xml 注册，内嵌 Tomcat 需要在启动类中显式注册。
     */
    @Bean
    public RequestContextListener requestContextListener() {
        return new RequestContextListener();
    }

    public static void main(String[] args) {
        _logger.info("Start MaxKey Application ...");
        ProductEnvironment.listEnvVars();

        SpringApplication application = new SpringApplication(MaxKeyUnifiedApplication.class);
        application.setApplicationStartup(new BufferingApplicationStartup(2048));
        ConfigurableApplicationContext applicationContext = application.run(args);
        initializeContext(applicationContext);

        _logger.info("MaxKey at {}", new DateTime());
        _logger.info("MaxKey Server Port {}", WebContext.getServerPort());
        _logger.info("MaxKey started.");
    }

    static void initializeContext(ConfigurableApplicationContext applicationContext) {
        new InitializeContext(applicationContext).init();
    }
}
