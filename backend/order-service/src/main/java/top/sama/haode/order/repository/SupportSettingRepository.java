package top.sama.haode.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import top.sama.haode.order.domain.SupportSetting;

public interface SupportSettingRepository extends JpaRepository<SupportSetting, String> {
}
