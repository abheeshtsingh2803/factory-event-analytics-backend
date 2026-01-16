package org.factory.service;

import org.factory.dto.request.EventRequest;
import org.factory.dto.response.BatchResponse;
import org.factory.repository.EventRepository;
import org.factory.service.event.EventService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class EventServiceTest {

    @Autowired
    private EventService eventService;

    @Autowired
    private EventRepository repository;

    // 1. Identical duplicate eventId → deduped
    @Test
    void identicalDuplicateIsDeduped() {
        EventRequest e1 = validEvent("E1");

        eventService.ingest(List.of(e1));
        BatchResponse res = eventService.ingest(List.of(e1));

        assertEquals(1, res.deduped);
        assertEquals(1L, repository.count());
    }

    // 2. Different payload + newer receivedTime → update happens
    @Test
    void newerPayloadUpdatesEvent() throws InterruptedException {
        EventRequest e1 = validEvent("E2");
        eventService.ingest(List.of(e1));

        Thread.sleep(10); // ensure newer receivedTime

        EventRequest updated = validEvent("E2");
        updated.defectCount = 5;

        BatchResponse res = eventService.ingest(List.of(updated));

        assertEquals(1, res.updated);
    }

    // 3. Different payload + older receivedTime → ignored
    @Test
    void olderPayloadIgnored() {
        EventRequest e1 = validEvent("E3");
        eventService.ingest(List.of(e1));
        repository.flush();

        EventRequest older = validEvent("E3");
        older.eventTime = e1.eventTime.minusSeconds(60);

        BatchResponse res = eventService.ingest(List.of(older));

        assertEquals(1, res.deduped);
        assertEquals(1L, repository.count());
    }


    // 4. Invalid duration rejected
    @Test
    void invalidDurationRejected() {
        EventRequest bad = validEvent("E4");
        bad.durationMs = -10;

        BatchResponse res = eventService.ingest(List.of(bad));

        assertEquals(1, res.rejected);
        assertEquals(0L, repository.count());
    }

    // 5. Future eventTime rejected
    @Test
    void futureEventTimeRejected() {
        EventRequest future = validEvent("E5");
        future.eventTime = Instant.now().plusSeconds(3600);

        BatchResponse res = eventService.ingest(List.of(future));

        assertEquals(1, res.rejected);
    }

    private EventRequest validEvent(String id) {
        EventRequest e = new EventRequest();
        e.eventId = id;
        e.eventTime = Instant.now();
        e.machineId = "M1";
        e.factoryId = "F01";
        e.lineId = "L01";
        e.durationMs = 1000;
        e.defectCount = 1;
        return e;
    }
}
