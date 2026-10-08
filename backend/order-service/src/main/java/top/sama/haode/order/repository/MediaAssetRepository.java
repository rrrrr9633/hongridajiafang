package top.sama.haode.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import top.sama.haode.order.domain.MediaAsset;

import java.util.UUID;

public interface MediaAssetRepository extends JpaRepository<MediaAsset, UUID> {}
