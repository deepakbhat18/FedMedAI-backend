package com.fedlearn.medical.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableAsync
public class SchedulerConfig {
    // Enables @Scheduled and @Async annotations
    // @EnableScheduling → TrainingSchedulerService.scheduledTraining()
    // @EnableAsync      → for future async email sending
}