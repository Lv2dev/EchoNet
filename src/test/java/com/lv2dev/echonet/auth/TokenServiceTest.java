package com.lv2dev.echonet.auth;

import com.lv2dev.echonet.model.Member;
import com.lv2dev.echonet.persistence.MemberRepository;
import com.lv2dev.echonet.service.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Value;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TokenServiceTest {

    @InjectMocks
    private TokenService tokenService;

    @Mock
    private MemberRepository memberRepository;

    @Value("${secretKey}")
    private String secretKey;

    private Member member;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        member = new Member();
        member.setId(1L);
        when(memberRepository.findById(anyLong())).thenReturn(java.util.Optional.of(member));
    }

    @Test
    void createAccessToken() {
        // Act
        String token = tokenService.createAccessToken(member);

        // Assert
        assertNotNull(token);
        // 여기서는 토큰이 생성되었는지만 확인합니다. 실제 토큰의 구조나 유효성은 별도의 라이브러리 함수를 사용해 검증해야 합니다.
    }

    @Test
    void isTokenValid() {
        // Arrange
        String validToken = tokenService.createAccessToken(member);

        // Act
        boolean isValid = tokenService.isTokenValid(validToken);

        // Assert
        assertTrue(isValid);
    }

    @Test
    void createRefreshToken() {
        // Act
        String refreshToken = tokenService.createRefreshToken(member);

        // Assert
        assertNotNull(refreshToken);
    }

    @Test
    void isRefreshTokenValid() {
        // Arrange
        String validRefreshToken = tokenService.createRefreshToken(member);

        // Act
        boolean isValid = tokenService.isRefreshTokenValid(validRefreshToken);

        // Assert
        assertTrue(isValid);
    }

    @Test
    void refreshAccessToken_WithInvalidToken_ShouldThrowException() {
        // Arrange
        String invalidRefreshToken = "invalidToken";

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> tokenService.refreshAccessToken(invalidRefreshToken));
    }
}

