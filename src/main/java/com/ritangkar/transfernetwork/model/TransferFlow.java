package com.ritangkar.transfernetwork.model;

public record TransferFlow(
        String clubId,
        String clubName,
        double totalSpent,
        double totalReceived,
        double netSpend,
        int transfersIn,
        int transfersOut
) {
}
