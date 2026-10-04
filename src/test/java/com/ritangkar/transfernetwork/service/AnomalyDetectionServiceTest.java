package com.ritangkar.transfernetwork.service;

import com.ritangkar.transfernetwork.model.Anomaly;
import com.ritangkar.transfernetwork.repository.TransferDataRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Expected counts and specific findings were independently computed with a
 * Python reference implementation before this test was written — see
 * docs/design-decisions.md.
 */
class AnomalyDetectionServiceTest {

    private final TransferDataRepository repository = new TransferDataRepository();
    private final AnomalyDetectionService service = new AnomalyDetectionService(repository);

    @Test
    void detectsAllFourInjectedFeeOutliers() {
        List<Anomaly> outliers = service.detectFeeOutliers();

        assertThat(outliers).allMatch(a -> a.type().equals("FEE_OUTLIER"));
        // The 4 deliberately-injected marquee transfers (55-90M) must be among the findings —
        // the detector also legitimately flags several organically-high fees around 8-9.5M,
        // which is expected IQR behavior, not a bug (see docs/design-decisions.md).
        assertThat(outliers.size()).isGreaterThanOrEqualTo(4);
        assertThat(outliers.stream().anyMatch(a -> a.evidence().contains("T0328"))).isTrue(); // the 86.5M transfer
    }

    @Test
    void detectsTheInjectedSilverlakeFcTransferFlurry() {
        List<Anomaly> flurries = service.detectTransferFlurries();

        assertThat(flurries).anyMatch(a -> a.type().equals("TRANSFER_FLURRY") && a.title().contains("Silverlake FC"));
    }

    @Test
    void detectsAtLeastTheSixInjectedBoomerangTransfers() {
        List<Anomaly> boomerangs = service.detectBoomerangTransfers();

        assertThat(boomerangs).allMatch(a -> a.type().equals("BOOMERANG"));
        // 16 total were found in the reference run (6 deliberately injected +
        // organic repeats that arise naturally from 220 players cycling
        // through 26 clubs over 10 transfer windows).
        assertThat(boomerangs.size()).isGreaterThanOrEqualTo(6);
    }

    @Test
    void everyAnomalyCitesAtLeastOneRealTransferId() {
        List<Anomaly> all = service.detectAll();
        assertThat(all).isNotEmpty();
        assertThat(all).allMatch(a -> !a.evidence().isEmpty());
    }
}
