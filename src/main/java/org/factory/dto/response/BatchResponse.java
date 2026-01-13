package org.factory.dto.response;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class BatchResponse {
    public int accepted;
    public int deduped;
    public int updated;
    public int rejected;
    public List<String> rejections = new ArrayList<>();
}

