package com.materialstudio;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.materialstudio.mapper")
public class MaterialStudioApplication {

    public static void main(String[] args) {
        SpringApplication.run(MaterialStudioApplication.class, args);
    }
}
