package org.factory.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "events", uniqueConstraints = @UniqueConstraint(columnNames = "eventId"))
public class Event {

    @Id
    @GeneratedValue
    private Long id;

    private String eventId;

    /* Instant:
     * It is mainly used for machine-based time recording,
     * such as event timestamps and logging, and offers nanosecond precision.
     * */
    private Instant eventTime;
    private Instant receivedTime;
    private String machineId;
    private String factoryId;
    private String lineId;
    private long durationMs;
    private int defectCount;

    @Lob
    private String payloadHash;
}