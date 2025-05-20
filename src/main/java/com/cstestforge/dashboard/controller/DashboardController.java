package com.cstestforge.dashboard.controller;

import com.cstestforge.dashboard.model.*;
import com.cstestforge.dashboard.service.DashboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * REST controller for dashboard API endpoints
 */
@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);
    
    private final DashboardService dashboardService;
    
    @Autowired
    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }
    
    /**
     * Get dashboard statistics
     * 
     * @param projectId Optional project ID
     * @return Dashboard statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<DashboardStats> getStats(@RequestParam(required = false) String projectId) {
        logger.debug("GET /api/v1/dashboard/stats - projectId: {}", projectId);
        return ResponseEntity.ok(dashboardService.getStats(projectId));
    }
    
    /**
     * Get recent test executions
     * 
     * @param limit Maximum number of tests to return
     * @param projectId Optional project ID
     * @return List of recent test executions
     */
    @GetMapping("/recent-tests")
    public ResponseEntity<List<RecentTest>> getRecentTests(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String projectId) {
        logger.debug("GET /api/v1/dashboard/recent-tests - limit: {}, projectId: {}", limit, projectId);
        return ResponseEntity.ok(dashboardService.getRecentTests(limit, projectId));
    }
    
    /**
     * Get environment status
     * 
     * @return List of environment statuses
     */
    @GetMapping("/environment")
    public ResponseEntity<List<EnvironmentStatus>> getEnvironmentStatus() {
        logger.debug("GET /api/v1/dashboard/environment");
        return ResponseEntity.ok(dashboardService.getEnvironmentStatus());
    }
    
    /**
     * Get metrics data
     * 
     * @param type Metric type (execution, performance, errors)
     * @param period Period (day, week, month)
     * @param projectId Optional project ID
     * @return Metrics data
     */
    @GetMapping("/metrics")
    public ResponseEntity<MetricsData> getMetrics(
            @RequestParam(defaultValue = "execution") String type,
            @RequestParam(defaultValue = "week") String period,
            @RequestParam(required = false) String projectId) {
        logger.debug("GET /api/v1/dashboard/metrics - type: {}, period: {}, projectId: {}", type, period, projectId);
        return ResponseEntity.ok(dashboardService.getMetrics(type, period, projectId));
    }
    
    /**
     * Get test execution timeline
     * 
     * @param days Number of days to retrieve data for
     * @param projectId Optional project ID
     * @return Test timeline data
     */
    @GetMapping("/timeline")
    public ResponseEntity<TestTimelineData> getTestTimeline(
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(required = false) String projectId) {
        logger.debug("GET /api/v1/dashboard/timeline - days: {}, projectId: {}", days, projectId);
        return ResponseEntity.ok(dashboardService.getTestTimeline(days, projectId));
    }
    
    /**
     * Get failure analysis data
     * 
     * @param days Number of days to analyze failures for
     * @param projectId Optional project ID
     * @return Failure analysis data
     */
    @GetMapping("/failures")
    public ResponseEntity<FailureAnalysis> getFailureAnalysis(
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(required = false) String projectId) {
        logger.debug("GET /api/v1/dashboard/failures - days: {}, projectId: {}", days, projectId);
        return ResponseEntity.ok(dashboardService.getFailureAnalysis(days, projectId));
    }
    
    /**
     * Get daily test counts
     * 
     * @param days Number of days to retrieve data for
     * @param projectId Optional project ID
     * @return List of daily test counts
     */
    @GetMapping("/daily-counts")
    public ResponseEntity<List<DailyTestCount>> getDailyTestCounts(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(required = false) String projectId) {
        logger.debug("GET /api/v1/dashboard/daily-counts - days: {}, projectId: {}", days, projectId);
        return ResponseEntity.ok(dashboardService.getDailyTestCounts(days, projectId));
    }
    
    /**
     * Export dashboard data as a report
     * 
     * @param format Format of the report (pdf, csv, excel)
     * @param projectId Optional project ID
     * @return Report file download
     */
    @GetMapping("/stats/export")
    public ResponseEntity<Resource> exportReport(
            @RequestParam(defaultValue = "pdf") String format,
            @RequestParam(required = false) String projectId) {
        logger.debug("GET /api/v1/dashboard/stats/export - format: {}, projectId: {}", format, projectId);
        
        // Validate format
        if (!isValidExportFormat(format)) {
            return ResponseEntity.badRequest().build();
        }
        
        // Generate report
        byte[] reportData = dashboardService.exportDashboardReport(format, projectId);
        ByteArrayResource resource = new ByteArrayResource(reportData);
        
        // Set appropriate content type and filename
        String filename = generateReportFilename(format);
        MediaType contentType = getContentTypeForFormat(format);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(contentType)
                .contentLength(reportData.length)
                .body(resource);
    }
    
    /**
     * Check if export format is valid
     * 
     * @param format Export format
     * @return true if format is valid
     */
    private boolean isValidExportFormat(String format) {
        return "pdf".equalsIgnoreCase(format) || 
               "csv".equalsIgnoreCase(format) || 
               "excel".equalsIgnoreCase(format);
    }
    
    /**
     * Generate report filename
     * 
     * @param format Export format
     * @return Filename
     */
    private String generateReportFilename(String format) {
        LocalDate now = LocalDate.now();
        String dateStr = now.format(DateTimeFormatter.ISO_LOCAL_DATE);
        String extension = "pdf".equalsIgnoreCase(format) ? "pdf" : 
                           "excel".equalsIgnoreCase(format) ? "xlsx" : "csv";
        
        return "dashboard-report-" + dateStr + "." + extension;
    }
    
    /**
     * Get content type for export format
     * 
     * @param format Export format
     * @return MediaType
     */
    private MediaType getContentTypeForFormat(String format) {
        if ("pdf".equalsIgnoreCase(format)) {
            return MediaType.APPLICATION_PDF;
        } else if ("excel".equalsIgnoreCase(format)) {
            return MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        } else {
            return MediaType.TEXT_PLAIN;
        }
    }
} 