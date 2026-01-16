package org.factory.repository;

import org.factory.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    Optional<Event> findByEventId(String eventId);

    @Query("""
      SELECT COUNT(e) FROM Event e
      WHERE e.machineId = :machineId
      AND e.eventTime >= :start AND e.eventTime < :end
    """)
    long countEvents(String machineId, Instant start, Instant end);

    @Query("""
          SELECT COALESCE(SUM(e.defectCount), 0)
          FROM Event e
          WHERE e.machineId = :machineId
            AND e.defectCount <> -1
            AND e.eventTime >= :start
            AND e.eventTime < :end
        """)
    long sumDefects(String machineId, Instant start, Instant end);


    @Query("""
      SELECT e.lineId,
             SUM(CASE WHEN e.defectCount != -1 THEN e.defectCount ELSE 0 END),
             COUNT(e)
      FROM Event e
      WHERE e.factoryId = :factoryId
        AND e.eventTime >= :from
        AND e.eventTime < :to
      GROUP BY e.lineId
      ORDER BY SUM(CASE WHEN e.defectCount != -1 THEN e.defectCount ELSE 0 END) DESC
    """)
    List<Object[]> findTopDefectLines(String factoryId, Instant from, Instant to);

}