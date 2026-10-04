package com.ritangkar.transfernetwork.model;

public record Corridor(
        String fromClubId,
        String fromClubName,
        String toClubId,
        String toClubName,
        int transferCount,
        double totalFee
) {
}
