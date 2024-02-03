package com.lv2dev.echonet.service;

import com.lv2dev.echonet.persistence.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {
    private final MemberRepository memberRepository;

    @Value("${secretKey}")
    private String secretKey;
}
