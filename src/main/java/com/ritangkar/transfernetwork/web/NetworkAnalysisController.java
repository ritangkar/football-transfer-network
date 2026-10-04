package com.ritangkar.transfernetwork.web;

import com.ritangkar.transfernetwork.model.Anomaly;
import com.ritangkar.transfernetwork.model.CentralityScore;
import com.ritangkar.transfernetwork.model.NationalityFlow;
import com.ritangkar.transfernetwork.service.AnomalyDetectionService;
import com.ritangkar.transfernetwork.service.CentralityService;
import com.ritangkar.transfernetwork.service.NationalityFlowService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class NetworkAnalysisController {

    private final NationalityFlowService nationalityFlowService;
    private final CentralityService centralityService;
    private final AnomalyDetectionService anomalyDetectionService;

    public NetworkAnalysisController(NationalityFlowService nationalityFlowService,
                                      CentralityService centralityService,
                                      AnomalyDetectionService anomalyDetectionService) {
        this.nationalityFlowService = nationalityFlowService;
        this.centralityService = centralityService;
        this.anomalyDetectionService = anomalyDetectionService;
    }

    @GetMapping("/api/nationality-flows")
    public List<NationalityFlow> nationalityFlows(@RequestParam(defaultValue = "20") int limit) {
        return nationalityFlowService.topFlows(Math.max(1, Math.min(limit, 100)));
    }

    @GetMapping("/api/centrality")
    public List<CentralityScore> centrality() {
        return centralityService.computeCentrality();
    }

    @GetMapping("/api/anomalies")
    public List<Anomaly> anomalies() {
        return anomalyDetectionService.detectAll();
    }
}
