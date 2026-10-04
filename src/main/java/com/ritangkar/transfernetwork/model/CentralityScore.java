package com.ritangkar.transfernetwork.model;

public record CentralityScore(
        String clubId,
        String clubName,
        double pageRank,
        int inDegree,
        int outDegree
) {
}
