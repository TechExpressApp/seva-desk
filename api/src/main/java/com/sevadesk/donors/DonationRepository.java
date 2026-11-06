package com.sevadesk.donors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DonationRepository extends JpaRepository<Donation, UUID> {
    Page<Donation> findAllByOrderByDonatedAtDesc(Pageable pageable);
    boolean existsByPaymentModeAndReference(Donation.PaymentMode mode, String reference);

    Page<Donation> findByContactIdOrderByDonatedAtDesc(UUID contactId, Pageable pageable);
}
