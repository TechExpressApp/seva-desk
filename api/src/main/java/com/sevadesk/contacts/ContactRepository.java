package com.sevadesk.contacts;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ContactRepository extends JpaRepository<Contact, UUID> {

    @Query("""
        select c from Contact c
        where lower(c.firstName) like lower(concat('%', :q, '%'))
           or lower(c.lastName)  like lower(concat('%', :q, '%'))
           or c.phone like concat('%', :q, '%')
           or lower(c.email) like lower(concat('%', :q, '%'))
        """)
    Page<Contact> search(@Param("q") String q, Pageable pageable);
}
