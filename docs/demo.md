# Portfolio Demo

**Demo level:** 2 — Portfolio simulation (bundled sample data, client-side).

`lab/demos/football-transfer-network.js` ports `TransferFlowService`,
`CentralityService` (PageRank) and `AnomalyDetectionService` to
JavaScript over the exact same bundled dataset (26 clubs, 220 players, 336
transfers), so a recruiter can browse the top spenders, the busiest
transfer corridors, the PageRank-ranked centrality table, and all three
anomaly types — including the same Silverlake FC transfer-flurry finding
and the four marquee fee outliers — entirely client-side, no backend call.

The real Spring Boot API (`mvn spring-boot:run`) exposes the full surface,
including nationality-flow queries beyond what the demo visualizes.
