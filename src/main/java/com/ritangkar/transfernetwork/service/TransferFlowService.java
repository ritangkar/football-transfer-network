package com.ritangkar.transfernetwork.service;

import com.ritangkar.transfernetwork.model.*;
import com.ritangkar.transfernetwork.repository.TransferDataRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Turns the raw transfer list into per-club spend/receive summaries and
 * frequently-used club-to-club "corridors".
 */
@Service
public class TransferFlowService {

    private final TransferDataRepository repository;

    public TransferFlowService(TransferDataRepository repository) {
        this.repository = repository;
    }

    public List<TransferFlow> allClubFlows() {
        List<Transfer> transfers = repository.findAllTransfers();
        Map<String, Double> spent = new HashMap<>();
        Map<String, Double> received = new HashMap<>();
        Map<String, Integer> transfersIn = new HashMap<>();
        Map<String, Integer> transfersOut = new HashMap<>();

        for (Transfer t : transfers) {
            spent.merge(t.toClubId(), t.feeMillions(), Double::sum);
            received.merge(t.fromClubId(), t.feeMillions(), Double::sum);
            transfersIn.merge(t.toClubId(), 1, Integer::sum);
            transfersOut.merge(t.fromClubId(), 1, Integer::sum);
        }

        List<TransferFlow> flows = new ArrayList<>();
        for (Club club : repository.findAllClubs()) {
            double s = spent.getOrDefault(club.id(), 0.0);
            double r = received.getOrDefault(club.id(), 0.0);
            flows.add(new TransferFlow(club.id(), club.name(), round1(s), round1(r), round1(s - r),
                    transfersIn.getOrDefault(club.id(), 0), transfersOut.getOrDefault(club.id(), 0)));
        }
        flows.sort(Comparator.comparingDouble(TransferFlow::totalSpent).reversed());
        return flows;
    }

    public TransferFlow flowForClub(String clubId) {
        return allClubFlows().stream()
                .filter(f -> f.clubId().equals(clubId))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Unknown club id: " + clubId));
    }

    /** The most frequently used directed club-to-club transfer routes, ranked by transfer count. */
    public List<Corridor> topCorridors(int limit) {
        List<Transfer> transfers = repository.findAllTransfers();
        record Pair(String from, String to) {}

        Map<Pair, Integer> counts = new LinkedHashMap<>();
        Map<Pair, Double> fees = new LinkedHashMap<>();
        for (Transfer t : transfers) {
            Pair p = new Pair(t.fromClubId(), t.toClubId());
            counts.merge(p, 1, Integer::sum);
            fees.merge(p, t.feeMillions(), Double::sum);
        }

        return counts.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .limit(limit)
                .map(e -> new Corridor(e.getKey().from(), repository.clubName(e.getKey().from()),
                        e.getKey().to(), repository.clubName(e.getKey().to()),
                        e.getValue(), round1(fees.get(e.getKey()))))
                .collect(Collectors.toList());
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
