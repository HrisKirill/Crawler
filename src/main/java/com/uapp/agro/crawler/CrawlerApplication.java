package com.uapp.agro.crawler;

import com.uapp.agro.crawler.config.ApplicationProperties;
import com.uapp.agro.crawler.scraper.service.ImageScraperService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableConfigurationProperties(ApplicationProperties.class)
public class CrawlerApplication {

    public static void main(String[] args) {
        SpringApplication.run(CrawlerApplication.class, args);
    }

    @Bean
    ApplicationRunner init(ImageScraperService imageScraperService) {
        return args -> {
            var url = "https://books.toscrape.com/";
            imageScraperService.startScraping(url);
        };
    }
}
