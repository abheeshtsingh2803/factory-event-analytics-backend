package org.factory.service.event;

import org.factory.dto.response.BatchResponse;
import org.factory.dto.request.EventRequest;
import org.factory.model.Event;
import org.factory.repository.EventRepository;
import org.factory.util.HashUtil;

import org.springframework.dao.DataIntegrityViolationException;
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
    public BatchResponse ingest(List<EventRequest> requests) {

        BatchResponse response = new BatchResponse();

        for (EventRequest request : requests) {

            // Reject invalid duration
            if (request.durationMs < 0 || request.durationMs > MAX_DURATION) {
                response.rejected++;
                continue;
            }

            // Reject future events
            if (request.eventTime.isAfter(Instant.now().plusSeconds(900))) {
                response.rejected++;
                continue;
            }

            String hash = HashUtil.hash(request.toString());

            Instant received = request.eventTime;

            Optional<Event> existing = repository.findByEventId(request.eventId);

            // Dedup / Update
            if (existing.isPresent()) {
                handleUpdate(existing.get(), request, hash, received, response);
                continue;
            }

            // Insert only if truly new
            try {
                saveNewEvent(request, hash, received);
                response.accepted++;
            } catch (DataIntegrityViolationException ex) {
                response.deduped++;
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
            Event existing,
            EventRequest r,
            String hash,
            Instant received,
            BatchResponse response
    ) {

        if (received.isBefore(existing.getReceivedTime())) {
            response.deduped++;
            return;
        }

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
            return;
        }

        response.deduped++;
    }
}
