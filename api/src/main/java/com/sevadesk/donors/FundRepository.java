package com.sevadesk.donors;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FundRepository extends JpaRepository<Fund, UUID> {
    List<Fund> findByActiveTrueOrderByName();
    Optional<Fund> findByName(String name);
}
