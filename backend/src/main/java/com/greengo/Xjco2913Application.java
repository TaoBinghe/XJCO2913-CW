package com.greengo;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.greengo.mapper")
@EnableCaching
@EnableScheduling
public class Xjco2913Application {

    public static void main(String[] args) {
        SpringApplication.run(Xjco2913Application.class, args);
    }

}

