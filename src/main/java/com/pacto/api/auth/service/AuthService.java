package com.pacto.api.auth.service;

import com.pacto.api.auth.dto.LoginRequest;
import com.pacto.api.auth.dto.LoginResponse;
import com.pacto.api.auth.dto.MeResponse;
import com.pacto.api.auth.dto.SignupRequest;
import com.pacto.api.auth.entity.Role;
import com.pacto.api.auth.entity.User;
import com.pacto.api.auth.jwt.JwtProvider;
import com.pacto.api.auth.repository.UserRepository;
import com.pacto.api.common.exception.DuplicateEmailException;
import com.pacto.api.common.exception.EmailNotFoundException;
import com.pacto.api.common.exception.InvalidPasswordException;
import com.pacto.api.common.exception.RoleMismatchException;
import com.pacto.api.common.exception.UserNotFoundException;
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
        walletRepository.save(Wallet.create(savedUser.getUserId()));
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

    @Transactional(readOnly = true)
    public MeResponse getMe(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        return new MeResponse(
                user.getUserId(),
                user.getEmail(),
                user.getRole().name()
        );
    }
}
