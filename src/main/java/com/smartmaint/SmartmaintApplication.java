package com.smartmaint;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication(scanBasePackages = "com.smartmaint")
@EnableScheduling
public class SmartmaintApplication {

    private static final Logger logger = LoggerFactory.getLogger(SmartmaintApplication.class);

    public static void main(String[] args) {
        logger.info("Iniciando Smartmaint backend");

        SpringApplication.run(SmartmaintApplication.class, args);
        logger.info("Backend Smartmaint activo y escuchando");
    }

    @Bean
    public CommandLineRunner passwordEncoderDiagnostics(ApplicationContext context) {
        return new CommandLineRunner() {
            @Override
            public void run(String... args) throws Exception {
                String[] encoderBeans = context.getBeanNamesForType(PasswordEncoder.class);
                logger.debug("PasswordEncoder beans detectados: {}", encoderBeans.length);
                for (String beanName : encoderBeans) {
                    Object bean = context.getBean(beanName);
                    logger.debug("- {} -> {}", beanName, bean.getClass().getName());
                }
            }
        };
    }
}