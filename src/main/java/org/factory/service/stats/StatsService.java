package org.factory.service.stats;

import org.factory.dto.response.TopDefectLineResponse;

import java.time.Instant;
import java.util.List;

public interface StatsService {
    List<TopDefectLineResponse> getTopDefectLines(
            String factoryId,
            Instant from,
            Instant to,
            int limit
    );
}

