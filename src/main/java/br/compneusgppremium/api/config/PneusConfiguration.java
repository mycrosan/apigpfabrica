package br.compneusgppremium.api.config;

import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@org.springframework.scheduling.annotation.EnableScheduling
@EnableConfigurationProperties({PoliticaPneuProperties.class, OcrLocalProperties.class})
public class PneusConfiguration {
    @Bean
    public Clock relogioPneus(final PoliticaPneuProperties politica) {
        return Clock.system(politica.fuso());
    }
}
