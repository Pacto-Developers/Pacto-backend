package com.pacto.api.auth.service;

import com.pacto.api.auth.dto.SignupRequest;
import com.pacto.api.auth.entity.Role;
import com.pacto.api.auth.entity.User;
import com.pacto.api.auth.jwt.JwtProvider;
import com.pacto.api.auth.repository.UserRepository;
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
    @InjectMocks AuthService authService;

    @Test
    void signup_시_지갑_자동_생성() {
        SignupRequest request = new SignupRequest();
        ReflectionTestUtils.setField(request, "email", "test@test.com");
        ReflectionTestUtils.setField(request, "password", "password");

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
    void signup_이메일_중복_예외() {
        SignupRequest request = new SignupRequest();
        ReflectionTestUtils.setField(request, "email", "dup@test.com");
        ReflectionTestUtils.setField(request, "password", "password");

        when(userRepository.existsByEmail("dup@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("이미 존재하는 이메일입니다.");

        verify(walletRepository, never()).save(any());
    }
}
