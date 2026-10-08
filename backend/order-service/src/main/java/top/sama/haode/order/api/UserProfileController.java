package top.sama.haode.order.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import top.sama.haode.order.application.AnnouncementService;
import top.sama.haode.order.application.CouponService;
import top.sama.haode.order.domain.User;
import top.sama.haode.order.repository.UserRepository;

@RestController
@RequestMapping("/api/users/me")
public class UserProfileController {
    private final UserRepository userRepository;
    private final CouponService couponService;
    private final AnnouncementService announcementService;

    public UserProfileController(UserRepository userRepository, CouponService couponService, AnnouncementService announcementService) {
        this.userRepository = userRepository;
        this.couponService = couponService;
        this.announcementService = announcementService;
    }

    @GetMapping
    public ProfileResponse get(@RequestAttribute("userId") String userId) {
        User user = couponService.ensureInviteCode(findUser(userId));
        return ProfileResponse.from(user, announcementService.unreadCount(userId));
    }

    @PutMapping("/avatar")
    public ProfileResponse updateAvatar(
            @RequestAttribute("userId") String userId,
            @Valid @RequestBody AvatarRequest request
    ) {
        User user = findUser(userId);
        user.updateAvatar(request.avatarUrl());
        return ProfileResponse.from(userRepository.save(user), announcementService.unreadCount(userId));
    }

    private User findUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));
    }

    public record AvatarRequest(@NotBlank String avatarUrl) {}

    public record ProfileResponse(String id, String phone, String avatarUrl, String inviteCode, boolean inviterBound, long unreadAnnouncements) {
        static ProfileResponse from(User user, long unreadAnnouncements) {
            return new ProfileResponse(user.getId(), user.getPhone(), user.getAvatarUrl(), user.getInviteCode(), user.hasInviter(), unreadAnnouncements);
        }
    }
}
