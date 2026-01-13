package org.factory.controller.event;

import org.factory.dto.request.EventRequest;
import org.factory.dto.response.BatchResponse;
import org.factory.service.event.EventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/events")
public class EventController {

    @Autowired
    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping("/batch")
    public BatchResponse ingest(@RequestBody List<EventRequest> events) {
        return eventService.ingest(events);
    }
}
