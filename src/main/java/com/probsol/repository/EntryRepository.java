package com.probsol.repository;

import com.probsol.entity.Entry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EntryRepository extends JpaRepository<Entry, String>, JpaSpecificationExecutor<Entry> {

    Optional<Entry> findByIdAndUserIdAndDeletedAtIsNull(String id, String userId);

    long countByUserIdAndDeletedAtIsNull(String userId);

    long countByUserIdAndTypeAndDeletedAtIsNull(String userId, String type);

    long countByUserIdAndTypeAndStatusAndDeletedAtIsNull(String userId, String type, String status);
}
