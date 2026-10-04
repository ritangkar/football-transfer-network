package com.ritangkar.transfernetwork.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ritangkar.transfernetwork.model.Club;
import com.ritangkar.transfernetwork.model.Player;
import com.ritangkar.transfernetwork.model.Transfer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class TransferDataRepository {

    private final List<Club> clubs;
    private final List<Player> players;
    private final List<Transfer> transfers;
    private final Map<String, Club> clubsById;
    private final Map<String, Player> playersById;

    public TransferDataRepository() {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        try {
            this.clubs = readList(mapper, "sample-data/clubs.json", Club[].class);
            this.players = readList(mapper, "sample-data/players.json", Player[].class);
            this.transfers = readList(mapper, "sample-data/transfers.json", Transfer[].class);
        } catch (IOException e) {
            throw new IllegalStateException("Could not load bundled sample data", e);
        }

        Map<String, Club> byId = new LinkedHashMap<>();
        for (Club c : clubs) byId.put(c.id(), c);
        this.clubsById = byId;

        Map<String, Player> pById = new LinkedHashMap<>();
        for (Player p : players) pById.put(p.id(), p);
        this.playersById = pById;
    }

    private <T> List<T> readList(ObjectMapper mapper, String classpathLocation, Class<T[]> arrayType) throws IOException {
        try (InputStream in = new ClassPathResource(classpathLocation).getInputStream()) {
            return List.of(mapper.readValue(in, arrayType));
        }
    }

    public List<Club> findAllClubs() { return clubs; }
    public List<Player> findAllPlayers() { return players; }
    public List<Transfer> findAllTransfers() { return transfers; }

    public Optional<Club> findClubById(String id) { return Optional.ofNullable(clubsById.get(id)); }
    public Optional<Player> findPlayerById(String id) { return Optional.ofNullable(playersById.get(id)); }

    public String clubName(String id) {
        return findClubById(id).map(Club::name).orElse(id);
    }
}
