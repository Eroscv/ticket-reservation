package com.portfolio.ticketreservation.repository;

import com.portfolio.ticketreservation.domain.TicketType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TicketTypeRepository extends JpaRepository<TicketType, Long> {

    /**
     * SELECT ... FOR UPDATE: blocks concurrent readers of this row until the
     * transaction commits, serializing reservations against the same ticket type.
     * Used by the pessimistic strategy.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TicketType t where t.id = :id")
    Optional<TicketType> findByIdForUpdate(@Param("id") Long id);
}
