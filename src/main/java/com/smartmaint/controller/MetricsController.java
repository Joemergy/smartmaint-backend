package com.smartmaint.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.time.Instant;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class MetricsController {

    private static final long START_TIME = System.currentTimeMillis();

    @GetMapping("/api/metrics")
    public Map<String, Object> metrics() {
        Map<String, Object> metrics = new LinkedHashMap<>();
        
        // Uptime
        long uptime = System.currentTimeMillis() - START_TIME;
        metrics.put("uptime", formatUptime(uptime));
        metrics.put("uptimeMs", uptime);
        
        // Memory
        Runtime runtime = Runtime.getRuntime();
        long memoryUsed = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
        long memoryMax = runtime.maxMemory() / 1024 / 1024;
        metrics.put("memoryUsedMB", memoryUsed);
        metrics.put("memoryMaxMB", memoryMax);
        metrics.put("memoryPercentage", (double) memoryUsed / memoryMax * 100);
        
        // CPU
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        metrics.put("cpuLoadAverage", osBean.getSystemLoadAverage());
        metrics.put("availableProcessors", osBean.getAvailableProcessors());
        
        // Thread info
        metrics.put("threadCount", Thread.activeCount());
        
        // Timestamp
        metrics.put("timestamp", Instant.now().toString());
        metrics.put("status", "OK");
        
        return metrics;
    }

    private String formatUptime(long uptime) {
        Duration duration = Duration.ofMillis(uptime);
        long days = duration.toDays();
        long hours = duration.toHours() % 24;
        long minutes = duration.toMinutes() % 60;
        long seconds = duration.getSeconds() % 60;
        
        return String.format("%d days %d hours %d minutes %d seconds", days, hours, minutes, seconds);
    }
}
