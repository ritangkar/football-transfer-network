package com.ritangkar.transfernetwork.model;

import java.time.LocalDate;

/**
 * One transfer: a directed edge from fromClubId to toClubId. feeMillions is
 * 0 for FREE and LOAN transfers by convention (see sample-data/transfers.json).
 */
public record Transfer(
        String id,
        String playerId,
        String fromClubId,
        String toClubId,
        LocalDate transferDate,
        double feeMillions,
        String transferType // PERMANENT, LOAN, FREE
) {
}
