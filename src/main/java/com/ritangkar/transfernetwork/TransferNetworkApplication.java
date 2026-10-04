package com.ritangkar.transfernetwork;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Football Transfer Network.
 *
 * An in-memory directed graph over a synthetic transfer market: clubs are
 * nodes, transfers are weighted edges. Demonstrates transfer flow analysis,
 * club-to-club "corridors", nationality flows, PageRank-based centrality,
 * and rule-based anomaly detection (fee outliers, transfer flurries,
 * boomerang transfers) — no graph database, since the dataset comfortably
 * fits in memory (see docs/design-decisions.md).
 */
@SpringBootApplication
public class TransferNetworkApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransferNetworkApplication.class, args);
    }
}
