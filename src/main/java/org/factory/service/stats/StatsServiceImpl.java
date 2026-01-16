package org.factory.service.stats;

import org.factory.dto.response.StatsResponse;
import org.factory.dto.response.TopDefectLineResponse;
import org.factory.repository.EventRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class StatsServiceImpl implements StatsService {

    private final EventRepository repository;

    public StatsServiceImpl(EventRepository repository) {
        this.repository = repository;
    }

    @Override
    public StatsResponse getMachineStats(
            String machineId,
            Instant start,
            Instant end
    ) {

        long events = repository.countEvents(machineId, start, end);
        long defects = repository.sumDefects(machineId, start, end);

        double hours = Duration.between(start, end).toSeconds() / 3600.0;
        double rate = hours == 0 ? 0 : defects / hours;

        StatsResponse res = new StatsResponse();
        res.machineId = machineId;
        res.eventsCount = events;
        res.defectsCount = defects;
        res.avgDefectRate = rate;
        res.status = rate < 2.0 ? "Healthy" : "Warning";

        return res;
    }

    @Override
    public List<TopDefectLineResponse> getTopDefectLines(
            String factoryId,
            Instant from,
            Instant to,
            int limit
    ) {

        List<Object[]> rows = repository.findTopDefectLines(factoryId, from, to);

        return rows.stream()
                .limit(limit)
                .map(row -> {
                    String lineId = (String) row[0];
                    long totalDefects = (long) row[1];
                    long eventCount = (long) row[2];

                    double percent = eventCount == 0
                            ? 0.0
                            : (totalDefects * 100.0) / eventCount;

                    percent = Math.round(percent * 100.0) / 100.0; // 2 decimals

                    return new TopDefectLineResponse(
                            lineId, totalDefects, eventCount, percent
                    );
                })
                .toList();
    }
}
