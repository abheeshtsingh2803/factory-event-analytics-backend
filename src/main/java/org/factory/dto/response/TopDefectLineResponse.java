package org.factory.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
@AllArgsConstructor
public class TopDefectLineResponse {

    public String lineId;
    public long totalDefects;
    public long eventCount;
    public double defectsPercent;

}