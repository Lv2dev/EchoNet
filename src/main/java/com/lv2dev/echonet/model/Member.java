package com.lv2dev.echonet.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "member")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // DB에서 자동증가
    @Column(name = "id")
    private Long id; // 사용자에게 고유하게 부여되는 값

    @Column(name = "refresh_token")
    private String refreshToken;

    @Column(name = "password")
    private String password;

    @Column(name = "email")
    private String email;

    @Column(name = "nickname")
    private String nickname;

    @Column(name = "join_day")
    private LocalDateTime joinDay;

    @Column(name = "profile")
    private String profile; // 프로필 이미지가 들어있는 경로

    @Column(name = "role")
    private int role; // 0:학생, 1:선생, 2:관리자

    @Column(name = "state")
    private int state;

    @Column(name = "login_attempt")
    private int loginAttempt; // 로그인 시도 횟수

    @Column(name = "last_login_attempt")
    private LocalDateTime lastLoginAttempt; // 마지막 로그인 시도 시각
}
