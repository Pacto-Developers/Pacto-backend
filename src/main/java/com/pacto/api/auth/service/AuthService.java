package com.pacto.api.auth.service;

import com.pacto.api.auth.dto.LoginRequest;
import com.pacto.api.auth.dto.LoginResponse;
import com.pacto.api.auth.dto.MeResponse;
import com.pacto.api.auth.dto.ProfileUpdateRequest;
import com.pacto.api.auth.dto.SignupRequest;
import com.pacto.api.auth.entity.AdvertiserProfile;
import com.pacto.api.auth.entity.BloggerProfile;
import com.pacto.api.auth.entity.Role;
import com.pacto.api.auth.entity.User;
import com.pacto.api.auth.jwt.JwtProvider;
import com.pacto.api.auth.repository.AdvertiserProfileRepository;
import com.pacto.api.auth.repository.BloggerProfileRepository;
import com.pacto.api.auth.repository.UserRepository;
import com.pacto.api.common.exception.DuplicateEmailException;
import com.pacto.api.common.exception.EmailNotFoundException;
import com.pacto.api.common.exception.InvalidPasswordException;
import com.pacto.api.common.exception.ProfileImageAccessDeniedException;
import com.pacto.api.common.exception.RoleMismatchException;
import com.pacto.api.common.exception.UserNotFoundException;
import com.pacto.api.file.domain.FileCategory;
import com.pacto.api.file.service.FileUploadService;
import com.pacto.api.wallet.entity.Wallet;
import com.pacto.api.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final WalletRepository walletRepository;
    private final BloggerProfileRepository bloggerProfileRepository;
    private final AdvertiserProfileRepository advertiserProfileRepository;
    private final FileUploadService fileUploadService;

    @Transactional
    public void signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException();
        }

        Role role = Role.from(request.getRole());

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .build();

        User savedUser = userRepository.save(user);
        createProfile(savedUser, request);
        walletRepository.save(Wallet.create(savedUser.getUserId()));
    }

    private void createProfile(User user, SignupRequest request) {
        if (user.getRole() == Role.BLOGGER) {
            BloggerProfile profile = BloggerProfile.create(user);
            applyBloggerProfile(profile, request.getBloggerProfile());
            bloggerProfileRepository.save(profile);
            return;
        }

        AdvertiserProfile profile = AdvertiserProfile.create(user);
        applyAdvertiserProfile(profile, request.getAdvertiserProfile());
        advertiserProfileRepository.save(profile);
    }

    private BloggerProfile getOrCreateBloggerProfile(User user) {
        return bloggerProfileRepository.findById(user.getUserId())
                .orElseGet(() -> bloggerProfileRepository.save(BloggerProfile.create(user)));
    }

    private AdvertiserProfile getOrCreateAdvertiserProfile(User user) {
        return advertiserProfileRepository.findById(user.getUserId())
                .orElseGet(() -> advertiserProfileRepository.save(AdvertiserProfile.create(user)));
    }

    private void applyBloggerProfile(
            BloggerProfile profile,
            SignupRequest.BloggerProfileRequest request
    ) {
        if (request == null) {
            return;
        }

        profile.updateProfile(
                request.getName(),
                request.getBlogUrl(),
                request.getContact(),
                request.getNickname(),
                request.getBankName(),
                request.getAccountNumber(),
                request.getAccountHolder(),
                request.getProfileImageUrl()
        );
    }

    private void applyAdvertiserProfile(
            AdvertiserProfile profile,
            SignupRequest.AdvertiserProfileRequest request
    ) {
        if (request == null) {
            return;
        }

        profile.updateProfile(
                request.getManagerName(),
                request.getCompanyName(),
                request.getBusinessNumber(),
                request.getContact(),
                request.getBrandName(),
                request.getBankName(),
                request.getAccountNumber(),
                request.getAccountHolder()
        );
    }

    private void applyBloggerProfilePartially(
            BloggerProfile profile,
            ProfileUpdateRequest.BloggerProfileRequest request
    ) {
        if (request == null) {
            return;
        }

        profile.updateProfilePartially(
                request.getName(),
                request.getBlogUrl(),
                request.getContact(),
                request.getNickname(),
                request.getBankName(),
                request.getAccountNumber(),
                request.getAccountHolder(),
                request.getProfileImageUrl()
        );
    }

    private void applyAdvertiserProfilePartially(
            AdvertiserProfile profile,
            ProfileUpdateRequest.AdvertiserProfileRequest request
    ) {
        if (request == null) {
            return;
        }

        profile.updateProfilePartially(
                request.getManagerName(),
                request.getCompanyName(),
                request.getBusinessNumber(),
                request.getContact(),
                request.getBrandName(),
                request.getBankName(),
                request.getAccountNumber(),
                request.getAccountHolder()
        );
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {

        Role loginRole = Role.from(request.getRole());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(EmailNotFoundException::new);

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidPasswordException();
        }

        if (user.getRole() != loginRole) {
            throw new RoleMismatchException();
        }

        String accessToken = jwtProvider.createToken(
                user.getUserId(),
                user.getRole().name()
        );

        return new LoginResponse(accessToken);
    }

    @Transactional
    public MeResponse getMe(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        if (user.getRole() == Role.BLOGGER) {
            BloggerProfile profile = getOrCreateBloggerProfile(user);

            return MeResponse.ofBlogger(
                    user.getUserId(),
                    user.getEmail(),
                    user.getRole().name(),
                    profile
            );
        }

        AdvertiserProfile profile = getOrCreateAdvertiserProfile(user);

        return MeResponse.ofAdvertiser(
                user.getUserId(),
                user.getEmail(),
                user.getRole().name(),
                profile
        );
    }

    @Transactional
    public String uploadProfileImage(Long userId, MultipartFile file) {

        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        if (user.getRole() != Role.BLOGGER) {
            throw new ProfileImageAccessDeniedException();
        }

        BloggerProfile profile = getOrCreateBloggerProfile(user);

        String previousImageKey = profile.getProfileImageKey();
        String objectKey = fileUploadService.upload(FileCategory.PROFILE, userId, file);
        profile.updateProfileImageKey(objectKey);

        if (previousImageKey != null && !previousImageKey.isBlank()) {
            // TODO: delete 실패 시 트랜잭션 전체가 롤백되어 신규 업로드까지 실패 처리됨 (캠페인 쪽도 동일 이슈).
            //       best-effort 처리로 개선 필요 - 별도 이슈로 트래킹
            fileUploadService.delete(previousImageKey);
        }

        return objectKey;
    }

    @Transactional
    public MeResponse updateMyProfile(Long userId, ProfileUpdateRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        if (user.getRole() == Role.BLOGGER) {
            BloggerProfile profile = getOrCreateBloggerProfile(user);
            applyBloggerProfilePartially(profile, request.getBloggerProfile());

            return MeResponse.ofBlogger(
                    user.getUserId(),
                    user.getEmail(),
                    user.getRole().name(),
                    profile
            );
        }

        AdvertiserProfile profile = getOrCreateAdvertiserProfile(user);
        applyAdvertiserProfilePartially(profile, request.getAdvertiserProfile());

        return MeResponse.ofAdvertiser(
                user.getUserId(),
                user.getEmail(),
                user.getRole().name(),
                profile
        );
    }
}
