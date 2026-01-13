package org.factory.dto.response;

import lombok.Data;

@Data
public class StatsResponse {
    public String machineId;
    public long eventsCount;
    public long defectsCount;
    public double avgDefectRate;
    public String status;
}
