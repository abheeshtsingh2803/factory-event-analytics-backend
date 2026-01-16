package org.factory.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.Instant;

@Data
@RequiredArgsConstructor
@AllArgsConstructor
public class EventRequest {
    public String eventId;
    public Instant eventTime;
    public String machineId;
    public long durationMs;
    public int defectCount;
    public String factoryId;
    public String lineId;
}