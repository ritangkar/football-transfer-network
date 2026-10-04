# Football Transfer Network

An in-memory graph engine over a synthetic transfer market: transfer flows,
club-to-club "corridors", nationality flows, PageRank-based centrality, and
rule-based anomaly detection — fee outliers, transfer flurries, and
boomerang transfers.

Part of the [Commerce Engineering Lab](https://ritangkar.github.io/#commerce-lab) —
17 small, production-minded engineering capabilities by Ritangkar Dey.

---

## 1. Project Overview

26 clubs across 4 fictional leagues, 220 players, 336 transfers (2021–2026).
Clubs are graph nodes; transfers are directed, weighted edges. The service
answers: who's spending, which routes are busiest, where does talent flow
from, which clubs are structurally central, and what looks unusual.

## 2. Problem

A raw transfer list is a table. The interesting structure — which clubs
are hubs, which routes are recurring, what's an outlier versus normal
market activity — only shows up once you treat it as a graph and actually
run graph algorithms over it, not just filter/sort the table.

## 3. Solution

An in-memory directed graph (no graph database — see design decisions),
with a real, verified PageRank implementation for centrality and three
independent, named anomaly checks, each citing the exact transfer ids behind it.

## 4. Key Features

- Per-club transfer flow: total spent, received, net spend
- Top club-to-club "corridors" (most frequently used transfer routes)
- Nationality → destination-league flow analysis
- **Weighted PageRank** centrality (transfer count as edge weight),
  verified against an independent Python reference implementation
- Three anomaly types: **fee outliers** (IQR rule), **transfer flurries**
  (5+ signings by one club within 30 days), **boomerang transfers**
  (a player returning to a previously-affiliated club)

## 5. Architecture

```
clubs.json + players.json + transfers.json
        │
        ▼
TransferDataRepository (in-memory, loaded once)
   ├── TransferFlowService (flows, corridors)
   ├── NationalityFlowService
   ├── CentralityService (PageRank)
   └── AnomalyDetectionService (outliers, flurries, boomerangs)
```

See [`docs/architecture.md`](docs/architecture.md) for the full diagram.

## 6. Technical Approach

- Standard power-iteration PageRank, damping 0.85, dangling-node mass
  redistributed evenly, convergence in ~16 iterations on this dataset
- IQR-based fee-outlier detection (`Q3 + 1.5×IQR`), the standard statistical rule
- Every anomaly cites the specific transfer id(s) it's based on

## 7. Design Decisions

See [`docs/design-decisions.md`](docs/design-decisions.md) — including why
PageRank was verified against an independent Python implementation before
being written in Java, and what that verification actually caught.

## 8. Sample Input

```
GET /api/centrality
GET /api/anomalies
GET /api/corridors?limit=5
GET /api/nationality-flows?limit=10
```

## 9. Sample Output

Real output from the bundled dataset:

```json
// GET /api/centrality (top result)
{
  "clubId": "C05",
  "clubName": "Silverlake FC",
  "pageRank": 0.0685,
  "inDegree": 22,
  "outDegree": 13
}
```

```json
// GET /api/anomalies (one of several)
{
  "type": "TRANSFER_FLURRY",
  "title": "Silverlake FC signed 5 players within 30 days",
  "description": "Silverlake FC made 5 incoming signings between 2025-07-05 and 2025-08-04.",
  "evidence": ["T0301", "T0308", "T0314", "T0317", "T0319"]
}
```

*Silverlake FC's top PageRank position is a direct, verifiable consequence
of the synthetic dataset's deliberately-injected transfer flurry — see the
test suite.*

## 10. How to Run

```bash
git clone <your-repo-url>
cd football-transfer-network
mvn spring-boot:run
```

API available at `http://localhost:8086`.

## 11. How to Test

```bash
mvn test
```

Tests assert against real bundled-data invariants (flow totals reconcile,
scores sum correctly, sorting is correct) and, for PageRank and anomaly
detection specifically, against independently pre-computed reference
values (see docs/design-decisions.md).

## 12. API Documentation

| Method | Path | Description |
|---|---|---|
| GET | `/api/clubs` | All clubs |
| GET | `/api/clubs/{id}/flow` | One club's transfer flow |
| GET | `/api/flows` | Every club's transfer flow, sorted by spend |
| GET | `/api/corridors` | `limit` — top club-to-club routes |
| GET | `/api/nationality-flows` | `limit` — nationality → league flows |
| GET | `/api/centrality` | PageRank-ranked club list |
| GET | `/api/anomalies` | All detected anomalies |

## 13. Portfolio Demo

The Commerce Engineering Lab demo (`lab/demos/football-transfer-network.js`)
ports flow, centrality and anomaly detection to client-side JavaScript over
the same bundled dataset. See [`docs/demo.md`](docs/demo.md).

## 14. Limitations

- Synthetic data — 4 fictional leagues, no real clubs or players
- PageRank uses transfer count, not fee, as edge weight (documented
  rationale in design decisions) — a fee-weighted variant would surface
  different clubs as central
- Boomerang detection can flag coincidental repeats alongside deliberate
  "returns" in a system this size — see design decisions for the exact count

## 15. Future Enhancements

- A fee-weighted PageRank variant for direct comparison
- Multi-season trend tracking (is a club's centrality rising or falling?)
- A shortest-path / "degrees of separation" endpoint between two clubs

## 16. License

MIT — see [`LICENSE`](LICENSE).
