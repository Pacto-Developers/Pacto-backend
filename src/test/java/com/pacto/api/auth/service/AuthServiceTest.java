package com.pacto.api.auth.service;

import com.pacto.api.auth.dto.LoginRequest;
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
import com.pacto.api.common.exception.RoleMismatchException;
import com.pacto.api.common.exception.UserNotFoundException;
import com.pacto.api.wallet.entity.Wallet;
import com.pacto.api.wallet.repository.WalletRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

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

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bloggerProfileRepository.findById(1L)).thenReturn(Optional.empty());

        authService.getMe(1L);

        ArgumentCaptor<BloggerProfile> captor = ArgumentCaptor.forClass(BloggerProfile.class);
        verify(bloggerProfileRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isEqualTo(user);
        verify(advertiserProfileRepository, never()).save(any());
    }

    @Test
    void getMe_기존_광고주_프로필이_없으면_빈_프로필을_생성한다() {
        User user = User.builder()
                .userId(1L).email("advertiser@test.com").password("encoded").role(Role.ADVERTISER).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(advertiserProfileRepository.findById(1L)).thenReturn(Optional.empty());

        authService.getMe(1L);

        ArgumentCaptor<AdvertiserProfile> captor = ArgumentCaptor.forClass(AdvertiserProfile.class);
        verify(advertiserProfileRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isEqualTo(user);
        verify(bloggerProfileRepository, never()).save(any());
    }

    @Test
    void getMe_프로필이_이미_있으면_새로_생성하지_않는다() {
        User user = User.builder()
                .userId(1L).email("blogger@test.com").password("encoded").role(Role.BLOGGER).build();
        BloggerProfile profile = BloggerProfile.create(user);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bloggerProfileRepository.findById(1L)).thenReturn(Optional.of(profile));

        authService.getMe(1L);

        verify(bloggerProfileRepository, never()).save(any());
    }
}
