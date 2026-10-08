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
import top.sama.haode.order.domain.User;
import top.sama.haode.order.repository.UserRepository;

@RestController
@RequestMapping("/api/users/me")
public class UserProfileController {
    private final UserRepository userRepository;

    public UserProfileController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public ProfileResponse get(@RequestAttribute("userId") String userId) {
        return ProfileResponse.from(findUser(userId));
    }

    @PutMapping("/avatar")
    public ProfileResponse updateAvatar(
            @RequestAttribute("userId") String userId,
            @Valid @RequestBody AvatarRequest request
    ) {
        User user = findUser(userId);
        user.updateAvatar(request.avatarUrl());
        return ProfileResponse.from(userRepository.save(user));
    }

    private User findUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));
    }

    public record AvatarRequest(@NotBlank String avatarUrl) {}

    public record ProfileResponse(String id, String phone, String avatarUrl) {
        static ProfileResponse from(User user) {
            return new ProfileResponse(user.getId(), user.getPhone(), user.getAvatarUrl());
        }
    }
}
