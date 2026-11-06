package com.sevadesk.reporting;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Open dashboard connections per temple, fed with Server-Sent Events.
 * In-memory: fine for one app server. With two or more servers, fan out through PostgreSQL LISTEN/NOTIFY.
 */
@Component
class DashboardStream {

    private static final Logger log = LoggerFactory.getLogger(DashboardStream.class);
    private static final long TIMEOUT = Duration.ofMinutes(30).toMillis();

    private final Map<UUID, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    SseEmitter open(UUID tenantId) {
        var emitter = new SseEmitter(TIMEOUT);
        var list = emitters.computeIfAbsent(tenantId, k -> new CopyOnWriteArrayList<>());
        list.add(emitter);
        Runnable remove = () -> list.remove(emitter);
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(e -> remove.run());
        return emitter;
    }

    void send(UUID tenantId, String event, Object data) {
        for (SseEmitter emitter : emitters.getOrDefault(tenantId, List.of())) {
            try {
                emitter.send(SseEmitter.event().name(event).data(data));
            } catch (IOException | IllegalStateException e) {
                log.debug("Dropping closed dashboard stream: {}", e.getMessage());
                emitter.completeWithError(e);
            }
        }
    }

    /** Keeps proxies (Cloudflare, Caddy) from closing idle streams. */
    @Scheduled(fixedRate = 25_000)
    void heartbeat() {
        emitters.forEach((tenant, list) -> list.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event().comment("ping"));
            } catch (IOException | IllegalStateException e) {
                emitter.completeWithError(e);
            }
        }));
    }
}
