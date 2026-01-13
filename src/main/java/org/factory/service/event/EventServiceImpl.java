package org.factory.service.event;

import org.factory.dto.response.BatchResponse;
import org.factory.dto.request.EventRequest;
import org.factory.model.Event;
import org.factory.repository.EventRepository;
import org.factory.util.HashUtil;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class EventServiceImpl implements EventService {

    private static final long MAX_DURATION = 6 * 60 * 60 * 1000; // 6 hours

    private final EventRepository repository;

    public EventServiceImpl(EventRepository repository) {
        this.repository = repository;
    }

    @Override
    public BatchResponse ingest(
            List<EventRequest> requests
    ) {

        BatchResponse response = new BatchResponse();

        for (EventRequest request : requests) {

            // 1. Validation
            if (request.durationMs < 0 || request.durationMs > MAX_DURATION) {
                response.rejected++;
                response.rejections.add(request.eventId + ": INVALID_DURATION");
                continue;
            }

            if (request.eventTime.isAfter(Instant.now().plusSeconds(86400))) {    // Change to 900 due to requirement
                response.rejected++;
                response.rejections.add(request.eventId + ": FUTURE_EVENT_TIME");
                continue;
            }

            // 2. Deduplication Hash
            String payloadHash = HashUtil.hash(request.toString());
            Instant receivedTime = Instant.now();

            Optional<Event> existing = repository.findByEventId(request.eventId);

            if (existing.isEmpty()) {
                saveNewEvent(request, payloadHash, receivedTime);
                response.accepted++;
            } else {
                handleUpdate(existing.get(), request, payloadHash, receivedTime, response);
            }
        }

        return response;
    }

    private void saveNewEvent(
            EventRequest r,
            String hash,
            Instant received
    ) {
        Event e = new Event();
        e.setEventId(r.eventId);
        e.setEventTime(r.eventTime);
        e.setReceivedTime(received);
        e.setMachineId(r.machineId);
        e.setFactoryId(r.factoryId);
        e.setLineId(r.lineId);
        e.setDurationMs(r.durationMs);
        e.setDefectCount(r.defectCount);
        e.setPayloadHash(hash);
        repository.save(e);
    }

    private void handleUpdate(
            Event existing, EventRequest r,
            String hash,
            Instant received,
            BatchResponse response
    ) {

        if (existing.getPayloadHash().equals(hash)) {
            response.deduped++;
            return;
        }

        if (received.isAfter(existing.getReceivedTime())) {
            existing.setEventTime(r.eventTime);
            existing.setReceivedTime(received);
            existing.setMachineId(r.machineId);
            existing.setDurationMs(r.durationMs);
            existing.setDefectCount(r.defectCount);
            existing.setFactoryId(r.factoryId);
            existing.setLineId(r.lineId);
            existing.setPayloadHash(hash);

            repository.save(existing);
            response.updated++;
        } else {
            response.deduped++;
        }
    }
}
