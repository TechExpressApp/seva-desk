package com.sevadesk.finance;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.UUID;

public interface ExpenseRepository extends JpaRepository<Expense, UUID> {

    Page<Expense> findAllByOrderBySpentOnDescCreatedAtDesc(Pageable pageable);

    /** Optional filters: purpose, contact, date range. */
    @Query("""
        select e from Expense e
        where (:purpose is null or e.purpose = :purpose)
          and (:contactId is null or e.contactId = :contactId)
          and (cast(:from as date) is null or e.spentOn >= :from)
          and (cast(:to as date) is null or e.spentOn <= :to)
        order by e.spentOn desc, e.createdAt desc
        """)
    Page<Expense> search(@Param("purpose") ExpensePurpose purpose, @Param("contactId") UUID contactId,
                         @Param("from") LocalDate from, @Param("to") LocalDate to, Pageable pageable);
}
