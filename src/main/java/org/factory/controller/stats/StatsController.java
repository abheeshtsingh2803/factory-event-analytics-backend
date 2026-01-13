package org.factory.controller.stats;

import org.factory.dto.response.StatsResponse;
import org.factory.dto.response.TopDefectLineResponse;
import org.factory.repository.EventRepository;
import org.factory.service.stats.StatsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/stats")
public class StatsController {

    private final EventRepository eventRepository;
    private final StatsService statsService;

    public StatsController(EventRepository eventRepository, StatsService statsService) {
        this.eventRepository = eventRepository;
        this.statsService = statsService;
    }

    @GetMapping
    public StatsResponse stats(
            @RequestParam String machineId,
            @RequestParam Instant start,
            @RequestParam Instant end
    ) {

        long events = eventRepository.countEvents(machineId, start, end);
        long defects = eventRepository.sumDefects(machineId, start, end);

        double hours = Duration.between(start, end).toSeconds() / 3600.0;
        double rate = defects / hours;

        StatsResponse res = new StatsResponse();
        res.machineId = machineId;
        res.eventsCount = events;
        res.defectsCount = defects;
        res.avgDefectRate = rate;
        res.status = rate < 2.0 ? "Healthy" : "Warning";

        return res;
    }

    @GetMapping("/top-defect-lines")
    public List<TopDefectLineResponse> topDefectLines(
            @RequestParam String factoryId,
            @RequestParam Instant from,
            @RequestParam Instant to,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return statsService.getTopDefectLines(factoryId, from, to, limit);
    }

}
