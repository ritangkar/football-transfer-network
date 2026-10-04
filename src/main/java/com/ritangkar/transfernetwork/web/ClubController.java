package com.ritangkar.transfernetwork.web;

import com.ritangkar.transfernetwork.model.Club;
import com.ritangkar.transfernetwork.model.Corridor;
import com.ritangkar.transfernetwork.model.TransferFlow;
import com.ritangkar.transfernetwork.repository.TransferDataRepository;
import com.ritangkar.transfernetwork.service.TransferFlowService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ClubController {

    private final TransferDataRepository repository;
    private final TransferFlowService flowService;

    public ClubController(TransferDataRepository repository, TransferFlowService flowService) {
        this.repository = repository;
        this.flowService = flowService;
    }

    @GetMapping("/api/clubs")
    public List<Club> clubs() {
        return repository.findAllClubs();
    }

    @GetMapping("/api/clubs/{id}/flow")
    public TransferFlow clubFlow(@PathVariable String id) {
        return flowService.flowForClub(id);
    }

    @GetMapping("/api/flows")
    public List<TransferFlow> allFlows() {
        return flowService.allClubFlows();
    }

    @GetMapping("/api/corridors")
    public List<Corridor> corridors(@RequestParam(defaultValue = "10") int limit) {
        return flowService.topCorridors(Math.max(1, Math.min(limit, 50)));
    }
}
