package com.legend.springbootredisdemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class SpringbootredisdemoApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext ctx = SpringApplication.run(SpringbootredisdemoApplication.class, args);
        TestRedis testRedis = ctx.getBean(TestRedis.class);
        //testRedis.testValueOps();
        //testRedis.testRedisConnection();
        //testRedis.testHashOps();
        //testRedis.testMyTemplate();
        testRedis.testPS();
    }

}
