package com.confApi.wooba.sales;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class WoobaIssuedPollingStore {
    private final ObjectMapper mapper;
    private final Path path;

    public WoobaIssuedPollingStore(ObjectMapper mapper,
            @Value("${wooba.sales.polling.issued.state-file:${user.home}/.confApi/wooba-issued-polling.json}") String path) {
        this.mapper = mapper;
        this.path = Path.of(path).toAbsolutePath().normalize();
    }

    public State load() throws IOException {
        if (!Files.exists(path)) {
            return new State();
        }
        State state = mapper.readValue(path.toFile(), State.class);
        if (state == null || state.getStreams() == null) {
            throw new IOException("Estado do polling Wooba invalido: " + path);
        }
        return state;
    }

    public void save(State state) throws IOException {
        Files.createDirectories(path.getParent());
        Path temporary = Files.createTempFile(path.getParent(), "wooba-issued-", ".tmp");
        try {
            mapper.writeValue(temporary.toFile(), state);
            try {
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    @Data
    public static class State {
        private Map<Integer, Stream> streams = new LinkedHashMap<>();
        public Stream stream(int type) {
            return streams.computeIfAbsent(type, ignored -> new Stream());
        }
    }

    @Data
    public static class Stream {
        private String queriedThrough;
        private Map<String, Pending> pending = new LinkedHashMap<>();
        private Map<String, Long> completed = new LinkedHashMap<>();
    }

    @Data
    public static class Pending {
        private String uniqueId;
        private String locator;
        private String lastUpdate;
        private int attempts;
        private String lastError;

        public Pending() {}

        public Pending(String uniqueId, String locator, String lastUpdate) {
            this.uniqueId = uniqueId;
            this.locator = locator;
            this.lastUpdate = lastUpdate;
        }

        public String key() {
            return uniqueId + "|" + lastUpdate + "|" + locator;
        }
    }
}
