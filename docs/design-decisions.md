# Design Decisions

---

**Decision:** No graph database — an in-memory graph built from three JSON
files at startup.

**Why:** 26 clubs and 336 transfers is a graph that fits comfortably in a
few hundred kilobytes of memory. Every algorithm here (PageRank, corridor
counting, outlier/flurry/boomerang detection) is a handful of passes over a
list — a graph database's real value (native traversal query languages,
storage for graphs too large to fit in memory, ACID updates to a live
graph) doesn't apply to a static, small, read-only dataset.

**Trade-off:** Wouldn't scale as-is to a real, continuously-updated
transfer-market dataset spanning many seasons and leagues.

**Future:** If the dataset grew by an order of magnitude or needed live
updates, that's the point where a graph database's traversal primitives
would start paying for their operational cost.

---

**Decision:** Verify `CentralityService` (PageRank) and
`AnomalyDetectionService` against independent Python reference
implementations, run against the exact same bundled JSON files, before
writing the Java versions.

**Why:** PageRank in particular is easy to get subtly wrong (dangling-node
handling, normalization, convergence criteria) in a way that still compiles
and runs without error. Building a reference implementation in a different
language against the same data, and only then writing (and testing) the
Java version against the reference's output, is a stronger correctness
check than eyeballing the algorithm once.

**What this caught:** the "top corridors" analysis and the fee-outlier
count both turned out richer than initially expected — the IQR method
flags several organically-high fees (around 8–9.5M) in addition to the 4
deliberately-injected marquee transfers (55–90M), because the fee
distribution's interquartile spread is narrow (most fees cluster near the
median of 2.7M). That's correct IQR behavior on this data, not a bug —
worth stating plainly rather than tuning the threshold after the fact to
produce a "cleaner" number.

**Trade-off:** Extra upfront verification time.

**Future:** N/A — this verify-then-port pattern is used across this
collection wherever an algorithm's correctness isn't visually obvious from
reading the code (see the Personal Research Agent and Customer Churn
Prediction API projects for the same pattern).

---

**Decision:** Report exactly one boomerang finding per return event, using
"has this player been affiliated with this club before" (including their
very first club) rather than only flagging round-trip transfers between
exactly two clubs.

**Why:** A player who plays for A → B → C → A has genuinely returned to a
former club, even though the round trip involves three clubs, not two. The
broader definition catches this; a strict two-club round-trip check
wouldn't.

**Trade-off:** With 220 players cycling through 26 clubs across 10 transfer
windows, some "boomerang" findings arise by chance rather than reflecting
a deliberate football-transfer-market pattern (a genuine return, a
disappointing loan spell, etc.) — the synthetic data doesn't encode
*why* a transfer happened, only that it did. 16 boomerangs were found
against 6 deliberately injected; see the test suite for the exact figure
this repository's bundled data produces.

**Future:** A real dataset with transfer-type context (e.g., explicit
"loan return" flags) could distinguish planned returns from coincidental ones.
