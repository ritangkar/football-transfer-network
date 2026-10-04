package com.ritangkar.transfernetwork.service;

import com.ritangkar.transfernetwork.model.Anomaly;
import com.ritangkar.transfernetwork.model.Player;
import com.ritangkar.transfernetwork.model.Transfer;
import com.ritangkar.transfernetwork.repository.TransferDataRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Three independent, deterministic anomaly checks over the transfer list.
 * Every finding cites the exact transfer id(s) behind it — nothing here is
 * a vague "this looks unusual"; each type has an explicit, documented rule.
 */
@Service
public class AnomalyDetectionService {

    private static final double IQR_MULTIPLIER = 1.5;
    private static final int FLURRY_WINDOW_DAYS = 30;
    private static final int FLURRY_MIN_COUNT = 5;

    private final TransferDataRepository repository;

    public AnomalyDetectionService(TransferDataRepository repository) {
        this.repository = repository;
    }

    public List<Anomaly> detectAll() {
        List<Anomaly> all = new ArrayList<>();
        all.addAll(detectFeeOutliers());
        all.addAll(detectTransferFlurries());
        all.addAll(detectBoomerangTransfers());
        return all;
    }

    /** Fees more than 1.5x the interquartile range above the 75th percentile — the standard IQR rule. */
    public List<Anomaly> detectFeeOutliers() {
        List<Double> fees = repository.findAllTransfers().stream()
                .map(Transfer::feeMillions)
                .filter(f -> f > 0)
                .sorted()
                .toList();
        if (fees.size() < 4) return List.of();

        double q1 = percentile(fees, 25);
        double q3 = percentile(fees, 75);
        double iqr = q3 - q1;
        double upperBound = q3 + IQR_MULTIPLIER * iqr;

        List<Anomaly> anomalies = new ArrayList<>();
        for (Transfer t : repository.findAllTransfers()) {
            if (t.feeMillions() > upperBound) {
                String playerName = repository.findPlayerById(t.playerId()).map(Player::name).orElse(t.playerId());
                anomalies.add(new Anomaly("FEE_OUTLIER",
                        String.format("%s: \u20AC%.1fM (%s \u2192 %s)", playerName, t.feeMillions(),
                                repository.clubName(t.fromClubId()), repository.clubName(t.toClubId())),
                        String.format("Fee is \u20AC%.1fM, above the \u20AC%.2fM upper bound (Q3=%.2f + 1.5\u00d7IQR=%.2f).",
                                t.feeMillions(), upperBound, q3, iqr),
                        List.of(t.id())));
            }
        }
        anomalies.sort((a, b) -> b.description().compareTo(a.description()));
        return anomalies;
    }

    /**
     * For each club, finds the earliest 30-day window (anchored at an actual
     * incoming-transfer date) containing at least 5 arrivals, and reports it.
     * Reports at most one flurry per club — the first qualifying window.
     */
    public List<Anomaly> detectTransferFlurries() {
        Map<String, List<LocalDate>> arrivalsByClub = new HashMap<>();
        for (Transfer t : repository.findAllTransfers()) {
            arrivalsByClub.computeIfAbsent(t.toClubId(), k -> new ArrayList<>()).add(t.transferDate());
        }

        List<Anomaly> anomalies = new ArrayList<>();
        for (var entry : arrivalsByClub.entrySet()) {
            List<LocalDate> dates = new ArrayList<>(entry.getValue());
            Collections.sort(dates);

            for (LocalDate windowStart : dates) {
                long count = dates.stream()
                        .filter(d -> {
                            long days = ChronoUnit.DAYS.between(windowStart, d);
                            return days >= 0 && days <= FLURRY_WINDOW_DAYS;
                        })
                        .count();
                if (count >= FLURRY_MIN_COUNT) {
                    String clubName = repository.clubName(entry.getKey());
                    anomalies.add(new Anomaly("TRANSFER_FLURRY",
                            clubName + " signed " + count + " players within 30 days",
                            clubName + " made " + count + " incoming signings between " + windowStart
                                    + " and " + windowStart.plusDays(FLURRY_WINDOW_DAYS) + ".",
                            transferIdsInWindow(entry.getKey(), windowStart)));
                    break; // only the first qualifying window per club
                }
            }
        }
        anomalies.sort((a, b) -> b.title().compareTo(a.title()));
        return anomalies;
    }

    private List<String> transferIdsInWindow(String clubId, LocalDate windowStart) {
        return repository.findAllTransfers().stream()
                .filter(t -> t.toClubId().equals(clubId))
                .filter(t -> {
                    long days = ChronoUnit.DAYS.between(windowStart, t.transferDate());
                    return days >= 0 && days <= FLURRY_WINDOW_DAYS;
                })
                .map(Transfer::id)
                .toList();
    }

    /**
     * A player returning to a club they were previously affiliated with
     * (either the club they started at, or any club they transferred to
     * earlier in their history).
     */
    public List<Anomaly> detectBoomerangTransfers() {
        Map<String, List<Transfer>> byPlayer = new LinkedHashMap<>();
        repository.findAllTransfers().stream()
                .sorted(Comparator.comparing(Transfer::transferDate))
                .forEach(t -> byPlayer.computeIfAbsent(t.playerId(), k -> new ArrayList<>()).add(t));

        List<Anomaly> anomalies = new ArrayList<>();
        for (var entry : byPlayer.entrySet()) {
            List<Transfer> history = entry.getValue();
            if (history.isEmpty()) continue;

            Set<String> visited = new HashSet<>();
            visited.add(history.get(0).fromClubId());

            for (Transfer t : history) {
                if (visited.contains(t.toClubId())) {
                    String playerName = repository.findPlayerById(t.playerId()).map(Player::name).orElse(t.playerId());
                    String clubName = repository.clubName(t.toClubId());
                    anomalies.add(new Anomaly("BOOMERANG",
                            playerName + " returned to " + clubName,
                            playerName + " transferred back to " + clubName + " on " + t.transferDate()
                                    + ", a club they were previously affiliated with.",
                            List.of(t.id())));
                }
                visited.add(t.toClubId());
            }
        }
        return anomalies;
    }

    private double percentile(List<Double> sorted, double pct) {
        double k = (sorted.size() - 1) * pct / 100.0;
        int f = (int) k;
        int c = Math.min(f + 1, sorted.size() - 1);
        if (f == c) return sorted.get(f);
        return sorted.get(f) + (sorted.get(c) - sorted.get(f)) * (k - f);
    }
}
