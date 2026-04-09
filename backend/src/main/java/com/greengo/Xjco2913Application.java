package com.greengo;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.greengo.mapper")
public class Xjco2913Application {

    public static void main(String[] args) {
        SpringApplication.run(Xjco2913Application.class, args);
    }

}

