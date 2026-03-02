package org.icc.pecesatierra.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EntityScan(basePackages = "org.icc.pecesatierra.entities")
@ComponentScan(basePackages = {"org.icc.pecesatierra"})
@EnableJpaRepositories(basePackages = "org.icc.pecesatierra.repositories")
@EnableScheduling
public class PecesATierraApplication {

    public static void main(String[] args) {
        SpringApplication.run(PecesATierraApplication.class, args);
    }

}
