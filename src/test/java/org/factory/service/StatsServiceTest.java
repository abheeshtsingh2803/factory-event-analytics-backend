package org.factory.service;

import org.factory.dto.request.EventRequest;
import org.factory.dto.response.StatsResponse;
import org.factory.repository.EventRepository;
import org.factory.service.event.EventService;
import org.factory.service.stats.StatsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class StatsServiceTest {

    @Autowired
    private EventService eventService;

    @Autowired
    private StatsService statsService;

    @Autowired
    private EventRepository repository;

    // 6. defectCount = -1 ignored
    @Test
    void defectMinusOneIgnoredInTotals() {
        EventRequest e1 = event("E10", -1);
        eventService.ingest(List.of(e1));

        StatsResponse stats = statsService.getMachineStats(
                "M1",
                Instant.now().minusSeconds(60),
                Instant.now().plusSeconds(60)
        );

        assertEquals(0, stats.defectsCount);
    }

    // 7. start inclusive, end exclusive
    @Test
    void timeWindowBoundaryRespected() {
        Instant t1 = Instant.now();
        Instant t2 = t1.plusSeconds(10);

        EventRequest e1 = event("E11", 1);
        e1.eventTime = t1;

        EventRequest e2 = event("E12", 1);
        e2.eventTime = t2;

        eventService.ingest(List.of(e1, e2));

        StatsResponse stats = statsService.getMachineStats(
                "M1", t1, t2
        );

        assertEquals(1, stats.eventsCount); // e2 excluded
    }

    private EventRequest event(String id, int defects) {
        EventRequest e = new EventRequest();
        e.eventId = id;
        e.eventTime = Instant.now();
        e.machineId = "M1";
        e.factoryId = "F01";
        e.lineId = "L01";
        e.durationMs = 1000;
        e.defectCount = defects;
        return e;
    }
}
