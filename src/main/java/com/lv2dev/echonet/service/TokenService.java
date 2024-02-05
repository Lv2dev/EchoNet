package com.lv2dev.echonet.service;

import com.lv2dev.echonet.model.Member;
import com.lv2dev.echonet.persistence.MemberRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {
    private final MemberRepository memberRepository;

    @Value("${secretKey}")
    private String secretKey;

    /**
     * AccessToken 발급하기
     * */
    public String createAccessToken(Member member) {
        return Jwts.builder() // JWT를 생성하기 위한 Builder 초기화 하기
                .setSubject(member.getId().toString()) // JWT payload에 저장되는 sub(ject) 클레임 설정
                .setIssuedAt(new Date()) // token의 발급시간 설정 -> 현재시간
                .setExpiration(new Date(System.currentTimeMillis() + 3600000)) // 토큰의 만료 시간을 설정 -> 현재 시간으로부터 1시간 후로 설정
                .signWith(SignatureAlgorithm.HS512, secretKey) // 토큰의 서명 알고리즘(HS512)과 서명에 사용할 비밀키 설정
                .compact(); // 위에서 설정한 내용으로 JWT를 생성하고 압축된 문자열 형태의 토큰을 반환
    }

    /**
     * Token 유효성 검사하기
     */
    public boolean isTokenValid(String token) {
        try {
            // JWT를 파싱하기 위한 Parser를 초기화합니다. setSigningKey 메소드를 사용하여,
            Jwts.parser()
                    .setSigningKey(secretKey) // 토큰의 서명을 검증하기 위해 사용되는 비밀키를 설정합니다.
                    .build()
                    // parseClaimsJws 메소드를 사용하여 전달받은 토큰을 파싱하고 검증합니다.
                    // 이 메소드는 서명이 유효한지, 토큰이 만료되었는지 등을 검사합니다.
                    .parseClaimsJws(token);
            // 위 과정에서 예외가 발생하지 않았다면, 토큰은 유효한 것으로 간주하고 true를 반환
            return true;
        } catch (Exception e) {
            // 파싱 중에 예외가 발생한 경우 (예: 서명 불일치, 토큰 만료 등),
            // 토큰은 유효하지 않은 것으로 간주하고 false를 반환
            return false;
        }
    }

    /**
     * RefreshToken 발급하기
     * */
    public String createRefreshToken(Member member) {
        return Jwts.builder()
                .setSubject(member.getId().toString())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 7 * 24 * 3600000)) // 7 days
                .signWith(SignatureAlgorithm.HS512, secretKey)
                .compact();
    }

    /**
     * Refresh Token 유효성 검사하기
     * */
    public boolean isRefreshTokenValid(String token) {
        try {
            Jwts.parser()
                    .setSigningKey(secretKey)
                    .build().parseSignedClaims(token).getPayload();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Access Token 갱신하기
     * */
    public String refreshAccessToken(String refreshToken) {
        if (!isRefreshTokenValid(refreshToken)) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        Claims claims = Jwts.parser()
                .setSigningKey(secretKey)
                .build().parseClaimsJws(refreshToken).getBody();

        Member member = memberRepository.findById(Long.parseLong(claims.getSubject()))
                .orElseThrow(() -> new IllegalArgumentException("Invalid member id"));

        return createAccessToken(member);
    }


}
