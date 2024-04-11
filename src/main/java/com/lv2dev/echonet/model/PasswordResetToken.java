package com.lv2dev.echonet.model;

import jakarta.persistence.*;

import java.util.Calendar;
import java.util.Date;

/**
 * 비밀번호 재설정 토큰을 나타내는 엔티티입니다.
 * 이 엔티티는 사용자, 토큰, 만료 날짜를 가지고 있습니다.
 */
@Entity
public class PasswordResetToken {

    private static final int EXPIRATION = 60 * 24;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String token;

    @OneToOne(targetEntity = Member.class, fetch = FetchType.EAGER)
    @JoinColumn(nullable = false, name = "user_id")
    private Member member;

    private Date expiryDate;

    public PasswordResetToken() {}

    /**
     * 토큰과 사용자를 받아서 새로운 PasswordResetToken 객체를 생성합니다.
     * 만료 날짜는 현재 시간에서 24시간 뒤로 설정됩니다.
     *
     * @param token 비밀번호 재설정 토큰 문자열
     * @param user 토큰을 발급받는 사용자
     */
    public PasswordResetToken(final String token, final Member member) {
        super();

        this.token = token;
        this.member = member;
        this.expiryDate = calculateExpiryDate(EXPIRATION);
    }

    // getters and setters

    /**
     * 만료 날짜를 계산하는 메소드입니다.
     * 현재 시간에서 입력받은 분만큼 더한 시간을 반환합니다.
     *
     * @param expiryTimeInMinutes 만료 시간(분)
     * @return 만료 날짜
     */
    private Date calculateExpiryDate(final int expiryTimeInMinutes) {
        final Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(new Date().getTime());
        cal.add(Calendar.MINUTE, expiryTimeInMinutes);
        return new Date(cal.getTime().getTime());
    }
}