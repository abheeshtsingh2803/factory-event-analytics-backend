package org.factory.service.stats;

import org.factory.dto.response.StatsResponse;
import org.factory.dto.response.TopDefectLineResponse;

import java.time.Instant;
import java.util.List;

public interface StatsService {

    StatsResponse getMachineStats(String machineId, Instant start, Instant end);

    List<TopDefectLineResponse> getTopDefectLines(
            String factoryId,
            Instant from,
            Instant to,
            int limit
    );
}

