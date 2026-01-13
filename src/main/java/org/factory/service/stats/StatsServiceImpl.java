package org.factory.service.stats;

import org.factory.dto.response.TopDefectLineResponse;
import org.factory.repository.EventRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class StatsServiceImpl implements StatsService {

    private final EventRepository repository;

    public StatsServiceImpl(EventRepository repository) {
        this.repository = repository;
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
