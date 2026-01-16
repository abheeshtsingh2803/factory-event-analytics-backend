package org.factory.concurrency;

import org.factory.dto.request.EventRequest;
import org.factory.repository.EventRepository;
import org.factory.service.event.EventService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class ConcurrencyTest {

    @Autowired
    private EventService eventService;

    @Autowired
    private EventRepository repository;

    @Test
    void concurrentIngestionIsThreadSafe() throws Exception {

        EventRequest e = new EventRequest();
        e.eventId = "ECON";
        e.eventTime = Instant.now();
        e.machineId = "M1";
        e.factoryId = "F01";
        e.lineId = "L01";
        e.durationMs = 1000;
        e.defectCount = 1;

        ExecutorService pool = Executors.newFixedThreadPool(10);

        for (int i = 0; i < 20; i++) {
            pool.submit(() -> eventService.ingest(List.of(e)));
        }

        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);

        assertEquals(1L, repository.count());
    }
}
