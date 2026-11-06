package com.sevadesk.events;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface EventRegistrationRepository extends JpaRepository<EventRegistration, UUID> {

    /** Event names already used for registrations, most recently used first. */
    @Query(value = """
        select event_name from event_registration
        group by event_name order by max(created_at) desc limit 50
        """, nativeQuery = true)
    java.util.List<String> recentEventNames();

    /** Newest first; eventName (ignoring case) and contactId are optional filters. */
    @Query("""
        select r from EventRegistration r
        where (:eventName is null or lower(r.eventName) = lower(cast(:eventName as string)))
          and (:contactId is null or r.contactId = :contactId)
        order by r.createdAt desc
        """)
    Page<EventRegistration> search(@Param("eventName") String eventName, @Param("contactId") UUID contactId,
                                   Pageable pageable);
}
