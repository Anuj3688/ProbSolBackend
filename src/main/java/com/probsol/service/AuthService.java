package com.probsol.service;

import com.probsol.dto.request.LoginRequest;
import com.probsol.dto.request.RegisterRequest;
import com.probsol.dto.response.AuthResponse;
import com.probsol.dto.response.RefreshTokenResponse;
import com.probsol.dto.response.UserResponse;
import com.probsol.entity.RefreshToken;
import com.probsol.entity.User;
import com.probsol.exception.BadRequestException;
import com.probsol.exception.UnauthorizedException;
import com.probsol.repository.RefreshTokenRepository;
import com.probsol.repository.UserRepository;
import com.probsol.security.JwtTokenProvider;
import com.probsol.security.UserPrincipal;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;

@Service
public class AuthService {

    public static final String REFRESH_COOKIE_NAME = "probsol_rt";

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request, HttpServletResponse response) {
        if (userRepository.existsByEmail(request.email().trim().toLowerCase())) {
            throw new BadRequestException("Email is already registered");
        }

        User user = new User(
                request.email().trim().toLowerCase(),
                passwordEncoder.encode(request.password()),
                request.displayName().trim()
        );
        User savedUser = userRepository.save(user);

        String accessToken = tokenProvider.generateAccessToken(savedUser.getId(), savedUser.getEmail());
        createAndSetRefreshToken(savedUser, response);

        return new AuthResponse(
                new UserResponse(savedUser.getId(), savedUser.getEmail(), savedUser.getDisplayName(), savedUser.getCreatedAt()),
                accessToken
        );
    }

    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletResponse response) {
        String email = request.email().trim().toLowerCase();
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.password())
        );

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        String accessToken = tokenProvider.generateAccessToken(user.getId(), user.getEmail());
        createAndSetRefreshToken(user, response);

        return new AuthResponse(
                new UserResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.getCreatedAt()),
                accessToken
        );
    }

    @Transactional
    public RefreshTokenResponse refreshToken(HttpServletRequest request, HttpServletResponse response) {
        String rawRefreshToken = extractRefreshTokenFromCookie(request)
                .orElseThrow(() -> new UnauthorizedException("Refresh token missing"));

        String tokenHash = tokenProvider.hashToken(rawRefreshToken);
        RefreshToken storedToken = refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(tokenHash)
                .orElseThrow(() -> new UnauthorizedException("Invalid or revoked refresh token"));

        if (storedToken.isExpired()) {
            storedToken.setRevokedAt(Instant.now());
            refreshTokenRepository.save(storedToken);
            throw new UnauthorizedException("Refresh token expired");
        }

        // Token rotation: Revoke old token and issue a new one
        storedToken.setRevokedAt(Instant.now());
        refreshTokenRepository.save(storedToken);

        User user = storedToken.getUser();
        String newAccessToken = tokenProvider.generateAccessToken(user.getId(), user.getEmail());
        createAndSetRefreshToken(user, response);

        return new RefreshTokenResponse(newAccessToken);
    }

    @Transactional
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        extractRefreshTokenFromCookie(request).ifPresent(rawToken -> {
            String tokenHash = tokenProvider.hashToken(rawToken);
            refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
                token.setRevokedAt(Instant.now());
                refreshTokenRepository.save(token);
            });
        });

        // Clear refresh cookie
        ResponseCookie clearCookie = ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(Duration.ZERO)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, clearCookie.toString());
    }

    public UserResponse getCurrentUser(UserPrincipal principal) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new UnauthorizedException("User not found"));
        return new UserResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.getCreatedAt());
    }

    private void createAndSetRefreshToken(User user, HttpServletResponse response) {
        String rawRefreshToken = tokenProvider.generateRefreshTokenString();
        String tokenHash = tokenProvider.hashToken(rawRefreshToken);
        Instant expiresAt = Instant.now().plusMillis(tokenProvider.getRefreshTokenExpirationMs());

        RefreshToken refreshToken = new RefreshToken(user, tokenHash, expiresAt);
        refreshTokenRepository.save(refreshToken);

        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, rawRefreshToken)
                .httpOnly(true)
                .secure(false) // Set to true if running strictly on HTTPS
                .path("/")
                .maxAge(Duration.ofMillis(tokenProvider.getRefreshTokenExpirationMs()))
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private Optional<String> extractRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        return Arrays.stream(request.getCookies())
                .filter(c -> REFRESH_COOKIE_NAME.equals(c.getName()))
                .map(Cookie::getValue)
                .filter(val -> val != null && !val.isBlank())
                .findFirst();
    }
}
