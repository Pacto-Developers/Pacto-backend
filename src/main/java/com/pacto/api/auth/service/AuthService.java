package com.pacto.api.auth.service;

import com.pacto.api.auth.dto.LoginRequest;
import com.pacto.api.auth.dto.LoginResponse;
import com.pacto.api.auth.dto.MeResponse;
import com.pacto.api.auth.dto.SignupRequest;
import com.pacto.api.auth.entity.Role;
import com.pacto.api.auth.entity.User;
import com.pacto.api.auth.jwt.JwtProvider;
import com.pacto.api.auth.repository.UserRepository;
import com.pacto.api.wallet.entity.Wallet;
import com.pacto.api.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final WalletRepository walletRepository;

    public void signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("이미 존재하는 이메일입니다.");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.BLOGGER)
                .build();

        User savedUser = userRepository.save(user);
        walletRepository.save(Wallet.create(savedUser.getUserId()));
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("존재하지 않는 이메일입니다."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("비밀번호가 일치하지 않습니다.");
        }

        String accessToken = jwtProvider.createToken(
                user.getUserId(),
                user.getRole().name()
        );

        return new LoginResponse(accessToken);
    }

    @Transactional(readOnly = true)
    public MeResponse getMe(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));

        return new MeResponse(
                user.getUserId(),
                user.getEmail(),
                user.getRole().name()
        );
    }
}
