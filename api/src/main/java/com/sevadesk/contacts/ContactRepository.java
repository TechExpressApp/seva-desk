package com.sevadesk.contacts;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ContactRepository extends JpaRepository<Contact, UUID> {

    Optional<Contact> findFirstByEmailIgnoreCase(String email);

    Optional<Contact> findFirstByEmailIgnoreCaseOrderByCreatedAtAsc(String email);

    /** Same phone number however it was typed: compares the last 10 digits (or all, if shorter). */
    @Query(value = """
        select * from contact
        where right(regexp_replace(coalesce(phone, ''), '[^0-9]', '', 'g'), 10) = :digits
        order by created_at limit 1
        """, nativeQuery = true)
    Optional<Contact> findFirstByPhoneDigits(@Param("digits") String digits);

    /** Same full name however it was split into first/last, ignoring case and extra spaces. */
    @Query(value = """
        select * from contact
        where lower(regexp_replace(trim(first_name || ' ' || coalesce(last_name, '')), '\\s+', ' ', 'g')) = :fullName
        order by created_at limit 1
        """, nativeQuery = true)
    Optional<Contact> findFirstByFullName(@Param("fullName") String fullName);

    @Query("""
        select c from Contact c
        where lower(c.firstName) like lower(concat('%', :q, '%'))
           or lower(c.lastName)  like lower(concat('%', :q, '%'))
           or c.phone like concat('%', :q, '%')
           or lower(c.email) like lower(concat('%', :q, '%'))
        """)
    Page<Contact> search(@Param("q") String q, Pageable pageable);
}
