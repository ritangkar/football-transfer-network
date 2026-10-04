package com.ritangkar.transfernetwork.service;

import com.ritangkar.transfernetwork.model.CentralityScore;
import com.ritangkar.transfernetwork.model.Club;
import com.ritangkar.transfernetwork.model.Transfer;
import com.ritangkar.transfernetwork.repository.TransferDataRepository;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Weighted PageRank over the transfer graph: clubs are nodes, each transfer
 * is a directed edge from its source club to its destination club, and an
 * edge's weight is the number of transfers along that exact route (not fee
 * — a club that trades frequently with another is "central" here even if
 * the fees are modest).
 *
 * Standard power-iteration PageRank with damping 0.85, dangling-node mass
 * redistributed evenly (a club with no outgoing transfers in the dataset
 * doesn't just leak its rank out of the system). Convergence and behavior
 * were verified against an independent Python reference implementation
 * before this class was written — see docs/design-decisions.md.
 */
@Service
public class CentralityService {

    private static final double DAMPING = 0.85;
    private static final int MAX_ITERATIONS = 200;
    private static final double CONVERGENCE_THRESHOLD = 1e-9;

    private final TransferDataRepository repository;

    public CentralityService(TransferDataRepository repository) {
        this.repository = repository;
    }

    public List<CentralityScore> computeCentrality() {
        List<Club> clubs = repository.findAllClubs();
        List<Transfer> transfers = repository.findAllTransfers();
        int n = clubs.size();
        List<String> clubIds = clubs.stream().map(Club::id).toList();

        // edge weight = transfer count along (from, to)
        Map<String, Map<String, Integer>> edgeWeights = new HashMap<>();
        Map<String, Integer> outWeightTotal = new HashMap<>();
        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, Integer> outDegree = new HashMap<>();

        for (Transfer t : transfers) {
            edgeWeights.computeIfAbsent(t.fromClubId(), k -> new HashMap<>())
                    .merge(t.toClubId(), 1, Integer::sum);
            outWeightTotal.merge(t.fromClubId(), 1, Integer::sum);
            inDegree.merge(t.toClubId(), 1, Integer::sum);
            outDegree.merge(t.fromClubId(), 1, Integer::sum);
        }

        Map<String, Double> pr = new HashMap<>();
        for (String id : clubIds) pr.put(id, 1.0 / n);

        for (int iter = 0; iter < MAX_ITERATIONS; iter++) {
            Map<String, Double> next = new HashMap<>();
            for (String id : clubIds) next.put(id, (1 - DAMPING) / n);

            for (String from : clubIds) {
                Map<String, Integer> outEdges = edgeWeights.get(from);
                int totalOut = outWeightTotal.getOrDefault(from, 0);
                if (outEdges == null || totalOut == 0) continue;
                for (var e : outEdges.entrySet()) {
                    double share = DAMPING * pr.get(from) * (e.getValue() / (double) totalOut);
                    next.merge(e.getKey(), share, Double::sum);
                }
            }

            // Redistribute dangling nodes' rank (clubs with no outgoing transfers) evenly.
            double danglingSum = 0;
            for (String id : clubIds) {
                if (outWeightTotal.getOrDefault(id, 0) == 0) danglingSum += pr.get(id);
            }
            double danglingShare = DAMPING * danglingSum / n;
            for (String id : clubIds) next.merge(id, danglingShare, Double::sum);

            double diff = 0;
            for (String id : clubIds) diff += Math.abs(next.get(id) - pr.get(id));
            pr = next;
            if (diff < CONVERGENCE_THRESHOLD) break;
        }

        List<CentralityScore> scores = new ArrayList<>();
        for (Club club : clubs) {
            scores.add(new CentralityScore(club.id(), club.name(), round4(pr.get(club.id())),
                    inDegree.getOrDefault(club.id(), 0), outDegree.getOrDefault(club.id(), 0)));
        }
        scores.sort(Comparator.comparingDouble(CentralityScore::pageRank).reversed());
        return scores;
    }

    private static double round4(double v) {
        return Math.round(v * 10000.0) / 10000.0;
    }
}
