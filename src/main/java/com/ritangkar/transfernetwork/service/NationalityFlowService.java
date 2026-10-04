package com.ritangkar.transfernetwork.service;

import com.ritangkar.transfernetwork.model.NationalityFlow;
import com.ritangkar.transfernetwork.model.Player;
import com.ritangkar.transfernetwork.model.Transfer;
import com.ritangkar.transfernetwork.repository.TransferDataRepository;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Aggregates transfers by the moving player's nationality and the
 * destination club's league — which countries' players are flowing into
 * which leagues.
 */
@Service
public class NationalityFlowService {

    private final TransferDataRepository repository;

    public NationalityFlowService(TransferDataRepository repository) {
        this.repository = repository;
    }

    public List<NationalityFlow> topFlows(int limit) {
        record Key(String nationality, String league) {}
        Map<Key, Integer> counts = new LinkedHashMap<>();

        for (Transfer t : repository.findAllTransfers()) {
            Player player = repository.findPlayerById(t.playerId()).orElse(null);
            String league = repository.findClubById(t.toClubId()).map(c -> c.league()).orElse("Unknown");
            if (player == null) continue;
            Key key = new Key(player.nationality(), league);
            counts.merge(key, 1, Integer::sum);
        }

        return counts.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .limit(limit)
                .map(e -> new NationalityFlow(e.getKey().nationality(), e.getKey().league(), e.getValue()))
                .toList();
    }

    /** Every league's incoming nationality mix, for a "where does this league's talent come from" view. */
    public Map<String, List<NationalityFlow>> flowsByLeague() {
        Map<String, List<NationalityFlow>> result = new LinkedHashMap<>();
        for (NationalityFlow f : topFlows(Integer.MAX_VALUE)) {
            result.computeIfAbsent(f.destinationLeague(), k -> new ArrayList<>()).add(f);
        }
        return result;
    }
}
