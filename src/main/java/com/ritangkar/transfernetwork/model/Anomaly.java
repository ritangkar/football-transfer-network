package com.ritangkar.transfernetwork.model;

import java.util.List;

/**
 * One detected pattern: a fee outlier, a transfer flurry, or a boomerang
 * transfer. {@code evidence} always ties back to specific transfer ids so
 * a finding can be checked against sample-data/transfers.json directly.
 */
public record Anomaly(
        String type,   // FEE_OUTLIER, TRANSFER_FLURRY, BOOMERANG
        String title,
        String description,
        List<String> evidence
) {
}
