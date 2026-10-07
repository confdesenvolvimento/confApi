package com.confApi.wooba.sales;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "wooba.sales.polling.issued")
public class WoobaIssuedPollingProperties {
    private Job tickets = new Job();
    private Job reservations = new Job();
    private int maxItemsPerCycle = 50;
    private int overlapMinutes = 5;
    private int queryWindowMinutes = 60;
    private int completedRetentionDays = 7;

    @Data
    public static class Job {
        private boolean enabled;
        private int initialLookbackMinutes = 60;
    }
}
