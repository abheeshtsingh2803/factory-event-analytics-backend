package org.factory.service.event;

import org.factory.dto.request.EventRequest;
import org.factory.dto.response.BatchResponse;
import java.util.List;

public interface EventService {

    BatchResponse ingest(List<EventRequest> requests);

}
