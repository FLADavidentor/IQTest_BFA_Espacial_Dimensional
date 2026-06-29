package com.iqtest.bfaespacial.service;
import com.iqtest.bfaespacial.model.Intento;

import com.iqtest.bfaespacial.common.SubtestCerradoPorTiempoEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/** Push-based timer closure (P2-A): per-intento SSE emitters, fed by the timer-close event. */
@Service
public class TimerEventService {

    private static final Logger log = LoggerFactory.getLogger(TimerEventService.class);
    private static final long TIMEOUT_MS = 30 * 60 * 1000L;

    private final Map<Long, List<SseEmitter>> emisores = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long intentoId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        emisores.computeIfAbsent(intentoId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> remove(intentoId, emitter));
        emitter.onTimeout(() -> remove(intentoId, emitter));
        emitter.onError(e -> remove(intentoId, emitter));
        return emitter;
    }

    // Fire after the close commits; fallbackExecution lets it run without a surrounding tx too.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onCerradoPorTiempo(SubtestCerradoPorTiempoEvent ev) {
        List<SseEmitter> list = emisores.getOrDefault(ev.intentoId(), List.of());
        for (SseEmitter e : list) {
            try {
                e.send(SseEmitter.event().name("cerrado").data(ev.subtestType()));
            } catch (IOException | IllegalStateException ex) {
                remove(ev.intentoId(), e);
            }
        }
        log.debug("SSE cerrado push intento={} subtest={}", ev.intentoId(), ev.subtestType());
    }

    private void remove(Long intentoId, SseEmitter emitter) {
        List<SseEmitter> list = emisores.get(intentoId);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) emisores.remove(intentoId);
        }
    }
}

