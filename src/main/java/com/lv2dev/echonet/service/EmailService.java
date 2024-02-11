package com.lv2dev.echonet.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * 이메일 전송 서비스를 담당합니다.
 */
@Service
public class EmailService {
    private final JavaMailSender javaMailSender;

    @Autowired
    public EmailService(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    /**
     * 비밀번호 변경 알림을 사용자 이메일로 전송합니다.
     *
     * @param to 받는 사람의 이메일 주소입니다.
     * @param subject 이메일 제목입니다.
     * @param text 이메일 본문입니다.
     */
    public void sendEmailNotification(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);

        // 이메일 전송
        javaMailSender.send(message);
    }
}
