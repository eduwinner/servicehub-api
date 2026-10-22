package com.servicehub.repository;

import com.servicehub.model.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByRequest_Id(Long requestId);

    @EntityGraph(attributePaths = {"request", "request.service", "reviewer"})
    Page<Review> findByRequest_Service_Id(Long serviceId, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"request", "request.service", "reviewer"})
    Optional<Review> findById(Long id);
}
