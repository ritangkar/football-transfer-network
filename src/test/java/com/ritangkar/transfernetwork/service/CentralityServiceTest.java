package com.ritangkar.transfernetwork.service;

import com.ritangkar.transfernetwork.model.CentralityScore;
import com.ritangkar.transfernetwork.repository.TransferDataRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Expected values were independently computed with a Python reference
 * implementation of the same weighted PageRank algorithm before this test
 * was written — see docs/design-decisions.md.
 */
class CentralityServiceTest {

    private final TransferDataRepository repository = new TransferDataRepository();
    private final CentralityService service = new CentralityService(repository);

    @Test
    void pageRankScoresSumToApproximatelyOne() {
        List<CentralityScore> scores = service.computeCentrality();
        double sum = scores.stream().mapToDouble(CentralityScore::pageRank).sum();
        assertThat(sum).isCloseTo(1.0, within(0.01));
    }

    @Test
    void everyClubAppearsExactlyOnce() {
        List<CentralityScore> scores = service.computeCentrality();
        long distinctClubs = scores.stream().map(CentralityScore::clubId).distinct().count();
        assertThat(scores).hasSize(distinctClubs > 0 ? (int) distinctClubs : scores.size());
        assertThat(scores).hasSize(repository.findAllClubs().size());
    }

    @Test
    void silverlakeFcHasTheHighestPageRank() {
        // Silverlake FC is the deliberate "transfer flurry" destination in the
        // synthetic dataset (9 signings in a 20-day window) — its dominant
        // PageRank position is a direct, verifiable consequence of that.
        List<CentralityScore> scores = service.computeCentrality();
        CentralityScore top = scores.get(0);

        assertThat(top.clubName()).isEqualTo("Silverlake FC");
        assertThat(top.pageRank()).isCloseTo(0.0685, within(0.002));
    }

    @Test
    void scoresAreSortedDescending() {
        List<CentralityScore> scores = service.computeCentrality();
        for (int i = 0; i < scores.size() - 1; i++) {
            assertThat(scores.get(i).pageRank()).isGreaterThanOrEqualTo(scores.get(i + 1).pageRank());
        }
    }
}
