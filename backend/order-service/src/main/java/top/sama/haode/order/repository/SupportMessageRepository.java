package top.sama.haode.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import top.sama.haode.order.domain.SupportMessage;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface SupportMessageRepository extends JpaRepository<SupportMessage, UUID> {
    List<SupportMessage> findByUserIdOrderByCreatedAtAsc(String userId);

    List<SupportMessage> findByUserIdAndCreatedAtAfterOrderByCreatedAtAsc(String userId, Instant after);

    List<SupportMessage> findByUserIdAndSenderAndReadAtIsNull(String userId, String sender);

    @Query("""
            select m from SupportMessage m
            where m.createdAt = (
                select max(x.createdAt) from SupportMessage x where x.userId = m.userId
            )
            order by m.createdAt desc
            """)
    List<SupportMessage> findLatestPerUser();

    @Query("""
            select m.userId, count(m) from SupportMessage m
            where m.sender = 'USER' and m.readAt is null
            group by m.userId
            """)
    List<Object[]> countUnreadForAdmin();
}
