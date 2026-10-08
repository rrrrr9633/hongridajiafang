package top.sama.haode.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import top.sama.haode.order.domain.AnnouncementRead;

import java.util.UUID;

public interface AnnouncementReadRepository extends JpaRepository<AnnouncementRead, AnnouncementRead.Id> {
    @Query("select count(r) from AnnouncementRead r where r.id.userId = :userId")
    long countByUserId(@Param("userId") String userId);

    @Query("select r.id.announcementId from AnnouncementRead r where r.id.userId = :userId")
    java.util.List<UUID> findReadIdsByUserId(@Param("userId") String userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from AnnouncementRead r where r.id.announcementId = :announcementId")
    void deleteByAnnouncementId(@Param("announcementId") UUID announcementId);
}
