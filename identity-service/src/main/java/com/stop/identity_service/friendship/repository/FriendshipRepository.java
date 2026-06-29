package com.stop.identity_service.friendship.repository;

import com.stop.identity_service.friendship.entity.Friendship;
import com.stop.identity_service.friendship.entity.FriendshipStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface FriendshipRepository extends JpaRepository<Friendship, UUID> {

    @Query("""
            SELECT COUNT(f) > 0 FROM Friendship f
            WHERE ((f.requesterId = :a AND f.receiverId = :b)
               OR  (f.requesterId = :b AND f.receiverId = :a))
              AND f.status IN ('PENDING', 'ACCEPTED')
            """)
    boolean existsPair(@Param("a") UUID a, @Param("b") UUID b);

    @Query("""
            SELECT f FROM Friendship f
            WHERE (f.requesterId = :a AND f.receiverId = :b)
               OR (f.requesterId = :b AND f.receiverId = :a)
            """)
    Optional<Friendship> findPair(@Param("a") UUID a, @Param("b") UUID b);

    @Query("""
            SELECT f FROM Friendship f
            WHERE (f.requesterId = :userId OR f.receiverId = :userId)
              AND f.status = :status
            ORDER BY f.createdAt DESC
            """)
    Slice<Friendship> findAllByUserAndStatus(@Param("userId") UUID userId,
                                             @Param("status") FriendshipStatus status,
                                             Pageable pageable);

    @Query("""
            SELECT f FROM Friendship f
            WHERE f.receiverId = :userId AND f.status = 'PENDING'
            ORDER BY f.createdAt DESC
            """)
    Slice<Friendship> findIncomingRequests(@Param("userId") UUID userId, Pageable pageable);

    @Query("""
            SELECT f FROM Friendship f
            WHERE f.requesterId = :userId AND f.status = 'PENDING'
            ORDER BY f.createdAt DESC
            """)
    Slice<Friendship> findOutgoingRequests(@Param("userId") UUID userId, Pageable pageable);
}
