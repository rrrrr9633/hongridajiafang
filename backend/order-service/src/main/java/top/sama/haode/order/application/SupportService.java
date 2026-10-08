package top.sama.haode.order.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.sama.haode.order.domain.SupportMessage;
import top.sama.haode.order.domain.SupportSetting;
import top.sama.haode.order.domain.User;
import top.sama.haode.order.repository.SupportMessageRepository;
import top.sama.haode.order.repository.SupportSettingRepository;
import top.sama.haode.order.repository.UserRepository;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SupportService {
    private static final String SETTING_ID = "default";
    private final SupportSettingRepository settings;
    private final SupportMessageRepository messages;
    private final UserRepository users;

    public SupportService(
            SupportSettingRepository settings,
            SupportMessageRepository messages,
            UserRepository users
    ) {
        this.settings = settings;
        this.messages = messages;
        this.users = users;
    }

    @Transactional
    public SupportSetting settings() {
        return settings.findById(SETTING_ID).orElseGet(() -> settings.save(new SupportSetting(SETTING_ID)));
    }

    @Transactional
    public SupportSetting updateWechatId(String wechatId) {
        SupportSetting setting = settings();
        setting.setWechatId(wechatId);
        return settings.save(setting);
    }

    @Transactional
    public List<SupportMessage> listUserMessages(String userId, Instant after) {
        markRead(userId, "ADMIN");
        if (after == null) return messages.findByUserIdOrderByCreatedAtAsc(userId);
        return messages.findByUserIdAndCreatedAtAfterOrderByCreatedAtAsc(userId, after);
    }

    @Transactional
    public SupportMessage sendFromUser(String userId, String content) {
        return messages.save(new SupportMessage(userId, "USER", requireContent(content)));
    }

    @Transactional
    public List<ThreadView> listThreads(String query) {
        Map<String, Long> unread = new HashMap<>();
        for (Object[] row : messages.countUnreadForAdmin()) {
            unread.put((String) row[0], ((Number) row[1]).longValue());
        }
        String needle = query == null ? "" : query.trim().toLowerCase();
        return messages.findLatestPerUser().stream().map(last -> {
            User user = users.findById(last.getUserId()).orElse(null);
            String phone = user == null ? "" : user.getPhone();
            String avatar = user == null ? null : user.getAvatarUrl();
            return new ThreadView(
                    last.getUserId(),
                    phone,
                    avatar,
                    last.getContent(),
                    last.getSender(),
                    last.getCreatedAt(),
                    unread.getOrDefault(last.getUserId(), 0L)
            );
        }).filter(thread -> needle.isEmpty()
                || thread.userId().toLowerCase().contains(needle)
                || (thread.phone() != null && thread.phone().toLowerCase().contains(needle))
                || (thread.lastContent() != null && thread.lastContent().toLowerCase().contains(needle))
        ).toList();
    }

    @Transactional
    public List<SupportMessage> listAdminMessages(String userId, Instant after) {
        users.findById(userId).orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        markRead(userId, "USER");
        if (after == null) return messages.findByUserIdOrderByCreatedAtAsc(userId);
        return messages.findByUserIdAndCreatedAtAfterOrderByCreatedAtAsc(userId, after);
    }

    @Transactional
    public SupportMessage sendFromAdmin(String userId, String content) {
        users.findById(userId).orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        return messages.save(new SupportMessage(userId, "ADMIN", requireContent(content)));
    }

    private void markRead(String userId, String sender) {
        Instant now = Instant.now();
        messages.findByUserIdAndSenderAndReadAtIsNull(userId, sender).forEach(item -> item.markRead(now));
    }

    private static String requireContent(String content) {
        String text = content == null ? "" : content.trim();
        if (text.isEmpty()) throw new IllegalArgumentException("消息不能为空");
        if (text.length() > 2000) throw new IllegalArgumentException("消息过长");
        return text;
    }

    public record ThreadView(
            String userId,
            String phone,
            String avatarUrl,
            String lastContent,
            String lastSender,
            Instant lastAt,
            long unread
    ) {}

    public record MessageView(UUID id, String sender, String content, Instant createdAt) {
        public static MessageView from(SupportMessage message) {
            return new MessageView(message.getId(), message.getSender(), message.getContent(), message.getCreatedAt());
        }
    }
}
