package com.ritangkar.transfernetwork.service;

import com.ritangkar.transfernetwork.model.Corridor;
import com.ritangkar.transfernetwork.model.TransferFlow;
import com.ritangkar.transfernetwork.repository.TransferDataRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class TransferFlowServiceTest {

    private final TransferDataRepository repository = new TransferDataRepository();
    private final TransferFlowService service = new TransferFlowService(repository);

    @Test
    void everyClubHasAFlowEntry() {
        List<TransferFlow> flows = service.allClubFlows();
        assertThat(flows).hasSize(repository.findAllClubs().size());
    }

    @Test
    void netSpendEqualsSpentMinusReceivedForEveryClub() {
        for (TransferFlow flow : service.allClubFlows()) {
            assertThat(flow.netSpend()).isCloseTo(flow.totalSpent() - flow.totalReceived(), within(0.01));
        }
    }

    @Test
    void flowsAreSortedByTotalSpentDescending() {
        List<TransferFlow> flows = service.allClubFlows();
        for (int i = 0; i < flows.size() - 1; i++) {
            assertThat(flows.get(i).totalSpent()).isGreaterThanOrEqualTo(flows.get(i + 1).totalSpent());
        }
    }

    @Test
    void totalTransfersInAcrossAllClubsEqualsTotalTransferCount() {
        int totalIn = service.allClubFlows().stream().mapToInt(TransferFlow::transfersIn).sum();
        assertThat(totalIn).isEqualTo(repository.findAllTransfers().size());
    }

    @Test
    void topCorridorsAreOrderedByTransferCountDescending() {
        List<Corridor> corridors = service.topCorridors(10);
        assertThat(corridors).isNotEmpty();
        for (int i = 0; i < corridors.size() - 1; i++) {
            assertThat(corridors.get(i).transferCount()).isGreaterThanOrEqualTo(corridors.get(i + 1).transferCount());
        }
    }

    @Test
    void corridorLimitIsRespected() {
        assertThat(service.topCorridors(3)).hasSizeLessThanOrEqualTo(3);
    }
}
