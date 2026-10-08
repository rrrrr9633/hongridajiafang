package top.sama.haode.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import top.sama.haode.order.domain.Session;

import java.time.Instant;
import java.util.Optional;

public interface SessionRepository extends JpaRepository<Session, String> {
    Optional<Session> findByTokenHashAndExpiresAtAfter(String tokenHash, Instant now);
}
