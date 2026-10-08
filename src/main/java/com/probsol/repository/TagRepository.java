package com.probsol.repository;

import com.probsol.dto.response.TagCountProjection;
import com.probsol.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, String> {
    Optional<Tag> findByUserIdAndName(String userId, String name);
    List<Tag> findByUserId(String userId);

    @Query("SELECT t.id AS id, t.name AS name, COUNT(e.id) AS count " +
           "FROM Tag t " +
           "LEFT JOIN t.entries e ON e.deletedAt IS NULL " +
           "WHERE t.user.id = :userId " +
           "GROUP BY t.id, t.name " +
           "ORDER BY COUNT(e.id) DESC, t.name ASC")
    List<TagCountProjection> findTagsWithCountByUserId(@Param("userId") String userId);
}
