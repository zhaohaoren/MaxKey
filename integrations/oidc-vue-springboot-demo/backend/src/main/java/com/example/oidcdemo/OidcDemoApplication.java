package com.example.oidcdemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Demo 后端入口。Spring Boot 自动扫描 config 和 controller 包，初始化
 * OIDC Client、SecurityFilterChain 与提供给 Vue 的会话接口。
 */
@SpringBootApplication
public class OidcDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(OidcDemoApplication.class, args);
    }
}
