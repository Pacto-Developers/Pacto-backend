package com.pacto.api.auth.service;

import com.pacto.api.auth.dto.LoginRequest;
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
import com.pacto.api.common.exception.MissingRoleException;
import com.pacto.api.common.exception.ProfileImageAccessDeniedException;
import com.pacto.api.common.exception.RoleMismatchException;
import com.pacto.api.common.exception.UserNotFoundException;
import com.pacto.api.file.domain.FileCategory;
import com.pacto.api.file.service.FileUploadService;
import com.pacto.api.wallet.entity.Wallet;
import com.pacto.api.wallet.repository.WalletRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtProvider jwtProvider;
    @Mock WalletRepository walletRepository;
    @Mock BloggerProfileRepository bloggerProfileRepository;
    @Mock AdvertiserProfileRepository advertiserProfileRepository;
    @Mock FileUploadService fileUploadService;
    @InjectMocks AuthService authService;

    @Test
    void signup_시_지갑_자동_생성() {
        SignupRequest request = new SignupRequest();
        ReflectionTestUtils.setField(request, "email", "test@test.com");
        ReflectionTestUtils.setField(request, "password", "password");
        ReflectionTestUtils.setField(request, "role", "BLOGGER");

        when(userRepository.existsByEmail("test@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password")).thenReturn("encoded");

        User savedUser = User.builder()
                .userId(1L).email("test@test.com").password("encoded").role(Role.BLOGGER).build();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        authService.signup(request);

        ArgumentCaptor<Wallet> captor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(1L);
    }

    @Test
    void signup_블로거는_블로거_프로필을_생성한다() {
        SignupRequest request = new SignupRequest();
        ReflectionTestUtils.setField(request, "email", "blogger@test.com");
        ReflectionTestUtils.setField(request, "password", "password");
        ReflectionTestUtils.setField(request, "role", "BLOGGER");
        SignupRequest.BloggerProfileRequest bloggerProfileRequest = new SignupRequest.BloggerProfileRequest();
        ReflectionTestUtils.setField(bloggerProfileRequest, "name", "홍길동");
        ReflectionTestUtils.setField(bloggerProfileRequest, "blogUrl", "https://blog.example.com");
        ReflectionTestUtils.setField(bloggerProfileRequest, "contact", "010-1234-5678");
        ReflectionTestUtils.setField(bloggerProfileRequest, "nickname", "길동");
        ReflectionTestUtils.setField(bloggerProfileRequest, "bankName", "국민은행");
        ReflectionTestUtils.setField(bloggerProfileRequest, "accountNumber", "123-456");
        ReflectionTestUtils.setField(bloggerProfileRequest, "accountHolder", "홍길동");
        ReflectionTestUtils.setField(bloggerProfileRequest, "profileImageUrl", "https://image.example.com/profile.png");
        ReflectionTestUtils.setField(request, "bloggerProfile", bloggerProfileRequest);

        when(userRepository.existsByEmail("blogger@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password")).thenReturn("encoded");

        User savedUser = User.builder()
                .userId(1L).email("blogger@test.com").password("encoded").role(Role.BLOGGER).build();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        authService.signup(request);

        ArgumentCaptor<BloggerProfile> captor = ArgumentCaptor.forClass(BloggerProfile.class);
        verify(bloggerProfileRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isEqualTo(savedUser);
        assertThat(captor.getValue().getName()).isEqualTo("홍길동");
        assertThat(captor.getValue().getBlogUrl()).isEqualTo("https://blog.example.com");
        assertThat(captor.getValue().getContact()).isEqualTo("010-1234-5678");
        assertThat(captor.getValue().getNickname()).isEqualTo("길동");
        assertThat(captor.getValue().getBankName()).isEqualTo("국민은행");
        assertThat(captor.getValue().getAccountNumber()).isEqualTo("123-456");
        assertThat(captor.getValue().getAccountHolder()).isEqualTo("홍길동");
        assertThat(captor.getValue().getProfileImageUrl()).isEqualTo("https://image.example.com/profile.png");
        verify(advertiserProfileRepository, never()).save(any());
    }

    @Test
    void signup_블로거_프로필_정보가_없어도_빈_프로필을_생성한다() {
        SignupRequest request = new SignupRequest();
        ReflectionTestUtils.setField(request, "email", "empty-blogger@test.com");
        ReflectionTestUtils.setField(request, "password", "password");
        ReflectionTestUtils.setField(request, "role", "BLOGGER");

        when(userRepository.existsByEmail("empty-blogger@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password")).thenReturn("encoded");

        User savedUser = User.builder()
                .userId(1L).email("empty-blogger@test.com").password("encoded").role(Role.BLOGGER).build();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        authService.signup(request);

        ArgumentCaptor<BloggerProfile> captor = ArgumentCaptor.forClass(BloggerProfile.class);
        verify(bloggerProfileRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isEqualTo(savedUser);
        assertThat(captor.getValue().getNickname()).isNull();
    }

    @Test
    void signup_광고주는_광고주_프로필을_생성한다() {
        SignupRequest request = new SignupRequest();
        ReflectionTestUtils.setField(request, "email", "advertiser@test.com");
        ReflectionTestUtils.setField(request, "password", "password");
        ReflectionTestUtils.setField(request, "role", "ADVERTISER");
        SignupRequest.AdvertiserProfileRequest advertiserProfileRequest = new SignupRequest.AdvertiserProfileRequest();
        ReflectionTestUtils.setField(advertiserProfileRequest, "managerName", "김담당");
        ReflectionTestUtils.setField(advertiserProfileRequest, "companyName", "팩토컴퍼니");
        ReflectionTestUtils.setField(advertiserProfileRequest, "businessNumber", "123-45-67890");
        ReflectionTestUtils.setField(advertiserProfileRequest, "contact", "02-1234-5678");
        ReflectionTestUtils.setField(advertiserProfileRequest, "brandName", "팩토");
        ReflectionTestUtils.setField(advertiserProfileRequest, "bankName", "신한은행");
        ReflectionTestUtils.setField(advertiserProfileRequest, "accountNumber", "987-654");
        ReflectionTestUtils.setField(advertiserProfileRequest, "accountHolder", "팩토컴퍼니");
        ReflectionTestUtils.setField(request, "advertiserProfile", advertiserProfileRequest);

        when(userRepository.existsByEmail("advertiser@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password")).thenReturn("encoded");

        User savedUser = User.builder()
                .userId(1L).email("advertiser@test.com").password("encoded").role(Role.ADVERTISER).build();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        authService.signup(request);

        ArgumentCaptor<AdvertiserProfile> captor = ArgumentCaptor.forClass(AdvertiserProfile.class);
        verify(advertiserProfileRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isEqualTo(savedUser);
        assertThat(captor.getValue().getManagerName()).isEqualTo("김담당");
        assertThat(captor.getValue().getCompanyName()).isEqualTo("팩토컴퍼니");
        assertThat(captor.getValue().getBusinessNumber()).isEqualTo("123-45-67890");
        assertThat(captor.getValue().getContact()).isEqualTo("02-1234-5678");
        assertThat(captor.getValue().getBrandName()).isEqualTo("팩토");
        assertThat(captor.getValue().getBankName()).isEqualTo("신한은행");
        assertThat(captor.getValue().getAccountNumber()).isEqualTo("987-654");
        assertThat(captor.getValue().getAccountHolder()).isEqualTo("팩토컴퍼니");
        verify(bloggerProfileRepository, never()).save(any());
    }

    @Test
    void signup_이메일_중복_예외() {
        SignupRequest request = new SignupRequest();
        ReflectionTestUtils.setField(request, "email", "dup@test.com");
        ReflectionTestUtils.setField(request, "password", "password");
        ReflectionTestUtils.setField(request, "role", "BLOGGER");

        when(userRepository.existsByEmail("dup@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessage("이미 존재하는 이메일입니다.");

        verify(walletRepository, never()).save(any());
        verify(bloggerProfileRepository, never()).save(any());
        verify(advertiserProfileRepository, never()).save(any());
    }

    @Test
    void login_존재하지_않는_이메일_예외() {
        LoginRequest request = new LoginRequest();
        ReflectionTestUtils.setField(request, "email", "missing@test.com");
        ReflectionTestUtils.setField(request, "password", "password");
        ReflectionTestUtils.setField(request, "role", "BLOGGER");

        when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(EmailNotFoundException.class)
                .hasMessage("존재하지 않는 이메일입니다.");
    }

    @Test
    void login_비밀번호_불일치_예외() {
        LoginRequest request = new LoginRequest();
        ReflectionTestUtils.setField(request, "email", "test@test.com");
        ReflectionTestUtils.setField(request, "password", "wrong");
        ReflectionTestUtils.setField(request, "role", "BLOGGER");

        User user = User.builder()
                .userId(1L).email("test@test.com").password("encoded").role(Role.BLOGGER).build();
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidPasswordException.class)
                .hasMessage("비밀번호가 일치하지 않습니다.");
    }

    @Test
    void login_요청_role과_유저_role이_다르면_예외() {
        LoginRequest request = new LoginRequest();
        ReflectionTestUtils.setField(request, "email", "test@test.com");
        ReflectionTestUtils.setField(request, "password", "password");
        ReflectionTestUtils.setField(request, "role", "ADVERTISER");

        User user = User.builder()
                .userId(1L).email("test@test.com").password("encoded").role(Role.BLOGGER).build();
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "encoded")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(RoleMismatchException.class)
                .hasMessage("로그인 role이 일치하지 않습니다.");
    }

    @Test
    void login_role이_없으면_예외() {
        LoginRequest request = new LoginRequest();
        ReflectionTestUtils.setField(request, "email", "test@test.com");
        ReflectionTestUtils.setField(request, "password", "password");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(MissingRoleException.class)
                .hasMessage("role은 필수입니다.");
    }

    @Test
    void signup_요청_role로_유저를_생성한다() {
        SignupRequest request = new SignupRequest();
        ReflectionTestUtils.setField(request, "email", "advertiser@test.com");
        ReflectionTestUtils.setField(request, "password", "password");
        ReflectionTestUtils.setField(request, "role", "ADVERTISER");

        when(userRepository.existsByEmail("advertiser@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            return User.builder()
                    .userId(1L)
                    .email(user.getEmail())
                    .password(user.getPassword())
                    .role(user.getRole())
                    .build();
        });

        authService.signup(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(Role.ADVERTISER);
    }

    @Test
    void getMe_유저없음_예외() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getMe(1L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("유저를 찾을 수 없습니다.");
    }

    @Test
    void getMe_기존_블로거_프로필이_없으면_빈_프로필을_생성한다() {
        User user = User.builder()
                .userId(1L).email("blogger@test.com").password("encoded").role(Role.BLOGGER).build();
        BloggerProfile createdProfile = BloggerProfile.create(user);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bloggerProfileRepository.findById(1L)).thenReturn(Optional.empty());
        when(bloggerProfileRepository.save(any(BloggerProfile.class))).thenReturn(createdProfile);

        MeResponse response = authService.getMe(1L);

        ArgumentCaptor<BloggerProfile> captor = ArgumentCaptor.forClass(BloggerProfile.class);
        verify(bloggerProfileRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isEqualTo(user);
        assertThat(response.getBloggerProfile()).isNotNull();
        assertThat(response.getAdvertiserProfile()).isNull();
        verify(advertiserProfileRepository, never()).save(any());
    }

    @Test
    void getMe_기존_광고주_프로필이_없으면_빈_프로필을_생성한다() {
        User user = User.builder()
                .userId(1L).email("advertiser@test.com").password("encoded").role(Role.ADVERTISER).build();
        AdvertiserProfile createdProfile = AdvertiserProfile.create(user);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(advertiserProfileRepository.findById(1L)).thenReturn(Optional.empty());
        when(advertiserProfileRepository.save(any(AdvertiserProfile.class))).thenReturn(createdProfile);

        MeResponse response = authService.getMe(1L);

        ArgumentCaptor<AdvertiserProfile> captor = ArgumentCaptor.forClass(AdvertiserProfile.class);
        verify(advertiserProfileRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isEqualTo(user);
        assertThat(response.getBloggerProfile()).isNull();
        assertThat(response.getAdvertiserProfile()).isNotNull();
        verify(bloggerProfileRepository, never()).save(any());
    }

    @Test
    void getMe_프로필이_이미_있으면_새로_생성하지_않는다() {
        User user = User.builder()
                .userId(1L).email("blogger@test.com").password("encoded").role(Role.BLOGGER).build();
        BloggerProfile profile = BloggerProfile.create(user);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bloggerProfileRepository.findById(1L)).thenReturn(Optional.of(profile));

        MeResponse response = authService.getMe(1L);

        verify(bloggerProfileRepository, never()).save(any());
        assertThat(response.getBloggerProfile()).isNotNull();
    }

    @Test
    void getMe_블로거는_블로거_프로필_정보를_반환한다() {
        User user = User.builder()
                .userId(1L).email("blogger@test.com").password("encoded").role(Role.BLOGGER).build();
        BloggerProfile profile = BloggerProfile.create(user);
        profile.updateProfile(
                "홍길동",
                "https://blog.example.com",
                "010-1234-5678",
                "길동",
                "국민은행",
                "123-456",
                "홍길동",
                "https://image.example.com/profile.png"
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bloggerProfileRepository.findById(1L)).thenReturn(Optional.of(profile));

        MeResponse response = authService.getMe(1L);

        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("blogger@test.com");
        assertThat(response.getRole()).isEqualTo("BLOGGER");
        assertThat(response.getAdvertiserProfile()).isNull();
        assertThat(response.getBloggerProfile().getName()).isEqualTo("홍길동");
        assertThat(response.getBloggerProfile().getBlogUrl()).isEqualTo("https://blog.example.com");
        assertThat(response.getBloggerProfile().getContact()).isEqualTo("010-1234-5678");
        assertThat(response.getBloggerProfile().getNickname()).isEqualTo("길동");
        assertThat(response.getBloggerProfile().getBankName()).isEqualTo("국민은행");
        assertThat(response.getBloggerProfile().getAccountNumber()).isEqualTo("123-456");
        assertThat(response.getBloggerProfile().getAccountHolder()).isEqualTo("홍길동");
        assertThat(response.getBloggerProfile().getProfileImageUrl()).isEqualTo("https://image.example.com/profile.png");
    }

    @Test
    void getMe_광고주는_광고주_프로필_정보를_반환한다() {
        User user = User.builder()
                .userId(1L).email("advertiser@test.com").password("encoded").role(Role.ADVERTISER).build();
        AdvertiserProfile profile = AdvertiserProfile.create(user);
        profile.updateProfile(
                "김담당",
                "팩토컴퍼니",
                "123-45-67890",
                "02-1234-5678",
                "팩토",
                "신한은행",
                "987-654",
                "팩토컴퍼니"
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(advertiserProfileRepository.findById(1L)).thenReturn(Optional.of(profile));

        MeResponse response = authService.getMe(1L);

        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("advertiser@test.com");
        assertThat(response.getRole()).isEqualTo("ADVERTISER");
        assertThat(response.getBloggerProfile()).isNull();
        assertThat(response.getAdvertiserProfile().getManagerName()).isEqualTo("김담당");
        assertThat(response.getAdvertiserProfile().getCompanyName()).isEqualTo("팩토컴퍼니");
        assertThat(response.getAdvertiserProfile().getBusinessNumber()).isEqualTo("123-45-67890");
        assertThat(response.getAdvertiserProfile().getContact()).isEqualTo("02-1234-5678");
        assertThat(response.getAdvertiserProfile().getBrandName()).isEqualTo("팩토");
        assertThat(response.getAdvertiserProfile().getBankName()).isEqualTo("신한은행");
        assertThat(response.getAdvertiserProfile().getAccountNumber()).isEqualTo("987-654");
        assertThat(response.getAdvertiserProfile().getAccountHolder()).isEqualTo("팩토컴퍼니");
    }

    @Test
    void updateMyProfile_블로거는_블로거_프로필을_부분_수정한다() {
        User user = User.builder()
                .userId(1L).email("blogger@test.com").password("encoded").role(Role.BLOGGER).build();
        BloggerProfile profile = BloggerProfile.create(user);
        profile.updateProfile(
                "홍길동",
                "https://old-blog.example.com",
                "010-0000-0000",
                "기존닉네임",
                "국민은행",
                "123-456",
                "홍길동",
                "https://image.example.com/old.png"
        );

        ProfileUpdateRequest request = new ProfileUpdateRequest();
        ProfileUpdateRequest.BloggerProfileRequest bloggerProfileRequest =
                new ProfileUpdateRequest.BloggerProfileRequest();
        ReflectionTestUtils.setField(bloggerProfileRequest, "nickname", "새닉네임");
        ReflectionTestUtils.setField(bloggerProfileRequest, "blogUrl", "https://new-blog.example.com");
        ReflectionTestUtils.setField(request, "bloggerProfile", bloggerProfileRequest);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bloggerProfileRepository.findById(1L)).thenReturn(Optional.of(profile));

        MeResponse response = authService.updateMyProfile(1L, request);

        assertThat(response.getBloggerProfile().getName()).isEqualTo("홍길동");
        assertThat(response.getBloggerProfile().getNickname()).isEqualTo("새닉네임");
        assertThat(response.getBloggerProfile().getBlogUrl()).isEqualTo("https://new-blog.example.com");
        assertThat(response.getBloggerProfile().getContact()).isEqualTo("010-0000-0000");
        assertThat(response.getBloggerProfile().getBankName()).isEqualTo("국민은행");
        assertThat(response.getAdvertiserProfile()).isNull();
    }

    @Test
    void updateMyProfile_광고주는_광고주_프로필을_부분_수정한다() {
        User user = User.builder()
                .userId(1L).email("advertiser@test.com").password("encoded").role(Role.ADVERTISER).build();
        AdvertiserProfile profile = AdvertiserProfile.create(user);
        profile.updateProfile(
                "기존담당",
                "기존회사",
                "123-45-67890",
                "02-0000-0000",
                "기존브랜드",
                "신한은행",
                "987-654",
                "기존회사"
        );

        ProfileUpdateRequest request = new ProfileUpdateRequest();
        ProfileUpdateRequest.AdvertiserProfileRequest advertiserProfileRequest =
                new ProfileUpdateRequest.AdvertiserProfileRequest();
        ReflectionTestUtils.setField(advertiserProfileRequest, "managerName", "새담당");
        ReflectionTestUtils.setField(advertiserProfileRequest, "brandName", "새브랜드");
        ReflectionTestUtils.setField(request, "advertiserProfile", advertiserProfileRequest);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(advertiserProfileRepository.findById(1L)).thenReturn(Optional.of(profile));

        MeResponse response = authService.updateMyProfile(1L, request);

        assertThat(response.getAdvertiserProfile().getManagerName()).isEqualTo("새담당");
        assertThat(response.getAdvertiserProfile().getCompanyName()).isEqualTo("기존회사");
        assertThat(response.getAdvertiserProfile().getBusinessNumber()).isEqualTo("123-45-67890");
        assertThat(response.getAdvertiserProfile().getBrandName()).isEqualTo("새브랜드");
        assertThat(response.getAdvertiserProfile().getBankName()).isEqualTo("신한은행");
        assertThat(response.getBloggerProfile()).isNull();
    }

    @Test
    void updateMyProfile_기존_프로필이_없으면_생성한_뒤_수정한다() {
        User user = User.builder()
                .userId(1L).email("blogger@test.com").password("encoded").role(Role.BLOGGER).build();
        BloggerProfile createdProfile = BloggerProfile.create(user);

        ProfileUpdateRequest request = new ProfileUpdateRequest();
        ProfileUpdateRequest.BloggerProfileRequest bloggerProfileRequest =
                new ProfileUpdateRequest.BloggerProfileRequest();
        ReflectionTestUtils.setField(bloggerProfileRequest, "nickname", "새닉네임");
        ReflectionTestUtils.setField(request, "bloggerProfile", bloggerProfileRequest);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bloggerProfileRepository.findById(1L)).thenReturn(Optional.empty());
        when(bloggerProfileRepository.save(any(BloggerProfile.class))).thenReturn(createdProfile);

        MeResponse response = authService.updateMyProfile(1L, request);

        ArgumentCaptor<BloggerProfile> captor = ArgumentCaptor.forClass(BloggerProfile.class);
        verify(bloggerProfileRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isEqualTo(user);
        assertThat(response.getBloggerProfile().getNickname()).isEqualTo("새닉네임");
    }

    @Test
    void updateMyProfile_유저없음_예외() {
        ProfileUpdateRequest request = new ProfileUpdateRequest();

        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.updateMyProfile(1L, request))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("유저를 찾을 수 없습니다.");
    }

    @Test
    void uploadProfileImage_블로거는_업로드에_성공하고_profileImageKey를_저장한다() {
        User user = User.builder()
                .userId(1L).email("blogger@test.com").password("encoded").role(Role.BLOGGER).build();
        BloggerProfile profile = BloggerProfile.create(user);
        MockMultipartFile file = new MockMultipartFile(
                "file", "profile.png", "image/png", "dummy-image-bytes".getBytes());

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bloggerProfileRepository.findById(1L)).thenReturn(Optional.of(profile));
        when(fileUploadService.upload(FileCategory.PROFILE, 1L, file))
                .thenReturn("profiles/1/new-key.png");

        String objectKey = authService.uploadProfileImage(1L, file);

        assertThat(objectKey).isEqualTo("profiles/1/new-key.png");
        assertThat(profile.getProfileImageKey()).isEqualTo("profiles/1/new-key.png");
        verify(fileUploadService, never()).delete(any());
    }

    @Test
    void uploadProfileImage_기존_이미지가_있으면_업로드_후_기존_키를_삭제한다() {
        User user = User.builder()
                .userId(1L).email("blogger@test.com").password("encoded").role(Role.BLOGGER).build();
        BloggerProfile profile = BloggerProfile.create(user);
        profile.updateProfileImageKey("profiles/1/old-key.png");
        MockMultipartFile file = new MockMultipartFile(
                "file", "profile.png", "image/png", "dummy-image-bytes".getBytes());

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bloggerProfileRepository.findById(1L)).thenReturn(Optional.of(profile));
        when(fileUploadService.upload(FileCategory.PROFILE, 1L, file))
                .thenReturn("profiles/1/new-key.png");

        authService.uploadProfileImage(1L, file);

        assertThat(profile.getProfileImageKey()).isEqualTo("profiles/1/new-key.png");
        verify(fileUploadService).delete("profiles/1/old-key.png");
    }

    @Test
    void uploadProfileImage_광고주는_예외를_던지고_S3를_호출하지_않는다() {
        User user = User.builder()
                .userId(1L).email("advertiser@test.com").password("encoded").role(Role.ADVERTISER).build();
        MockMultipartFile file = new MockMultipartFile(
                "file", "profile.png", "image/png", "dummy-image-bytes".getBytes());

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.uploadProfileImage(1L, file))
                .isInstanceOf(ProfileImageAccessDeniedException.class)
                .hasMessage("블로거만 프로필 이미지를 업로드할 수 있습니다.");

        verifyNoInteractions(fileUploadService);
        verify(bloggerProfileRepository, never()).findById(any());
    }

    @Test
    void uploadProfileImage_유저없음_예외() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "profile.png", "image/png", "dummy-image-bytes".getBytes());

        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.uploadProfileImage(1L, file))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("유저를 찾을 수 없습니다.");

        verifyNoInteractions(fileUploadService);
    }

    @Test
    void getMe_profileImageKey가_없으면_profileImageDownloadUrl은_null이다() {
        User user = User.builder()
                .userId(1L).email("blogger@test.com").password("encoded").role(Role.BLOGGER).build();
        BloggerProfile profile = BloggerProfile.create(user);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bloggerProfileRepository.findById(1L)).thenReturn(Optional.of(profile));

        MeResponse response = authService.getMe(1L);

        assertThat(response.getBloggerProfile().getProfileImageDownloadUrl()).isNull();
        verifyNoInteractions(fileUploadService);
    }

    @Test
    void getMe_profileImageKey가_있으면_presigned_URL로_변환한다() throws MalformedURLException {
        User user = User.builder()
                .userId(1L).email("blogger@test.com").password("encoded").role(Role.BLOGGER).build();
        BloggerProfile profile = BloggerProfile.create(user);
        profile.updateProfileImageKey("profiles/1/photo.png");
        URL presignedUrl = new URL("https://test-bucket.s3.amazonaws.com/profiles/1/photo.png?X-Amz-Signature=abc");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bloggerProfileRepository.findById(1L)).thenReturn(Optional.of(profile));
        when(fileUploadService.getPresignedUrl(eq("profiles/1/photo.png"), any(Duration.class)))
                .thenReturn(presignedUrl);

        MeResponse response = authService.getMe(1L);

        assertThat(response.getBloggerProfile().getProfileImageDownloadUrl())
                .isEqualTo(presignedUrl.toString());

        ArgumentCaptor<Duration> durationCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(fileUploadService).getPresignedUrl(eq("profiles/1/photo.png"), durationCaptor.capture());
        assertThat(durationCaptor.getValue()).isEqualTo(Duration.ofMinutes(10));
    }
}
