package com.unifiedmedia.cms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class UnifiedMediaCmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(UnifiedMediaCmsApplication.class, args);
    }
}
