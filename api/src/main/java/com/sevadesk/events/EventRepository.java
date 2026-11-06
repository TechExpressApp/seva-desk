package com.sevadesk.events;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, UUID> {

    Page<Event> findAllByOrderByStartsAtDesc(Pageable pageable);

    boolean existsBySlug(String slug);

    /** The most recent event with this title (ignoring case), to link a registration to it. */
    Optional<Event> findFirstByTitleIgnoreCaseOrderByStartsAtDesc(String title);

    Optional<Event> findBySlugAndStatus(String slug, Event.Status status);

    /** Published events still running or yet to start, soonest first. */
    @Query("""
        select e from Event e
        where e.status = com.sevadesk.events.Event.Status.PUBLISHED
          and coalesce(e.endsAt, e.startsAt) >= :now
        order by e.startsAt
        """)
    List<Event> upcoming(@Param("now") Instant now, Pageable pageable);
}
