package com.lv2dev.echonet.auth;

// JUnit Jupiter API를 사용하는 테스트 클래스에 필요한 어노테이션입니다. JUnit 5에서 제공하는 기능을 활용할 수 있게 해줍니다.
import com.lv2dev.echonet.dto.MemberDTO;
import com.lv2dev.echonet.model.Member;
import com.lv2dev.echonet.persistence.MemberRepository;
import com.lv2dev.echonet.service.MemberService;
import com.lv2dev.echonet.service.S3Service;
import org.junit.jupiter.api.Test;
// Spring Boot 테스트와 JUnit 5를 연동하기 위한 어노테이션입니다. 이를 통해 Spring TestContext Framework를 JUnit 5와 함께 사용할 수 있습니다.
import org.junit.jupiter.api.extension.ExtendWith;
// Mockito의 어노테이션으로, 자동으로 의존성을 주입할 대상에 사용합니다.
import org.mockito.InjectMocks;
// 테스트 대상에서 사용되는 의존성을 모의 객체로 생성하기 위한 어노테이션입니다.
import org.mockito.Mock;
// Spring Boot 기반의 테스트를 위해 ApplicationContext를 로드하고, Spring Bean을 테스트에 주입할 수 있게 해주는 어노테이션입니다.
import org.springframework.beans.factory.annotation.Autowired;
// Spring Boot 테스트를 위한 기본 어노테이션. 전체 애플리케이션 컨텍스트를 로드하여 통합 테스트 환경을 제공합니다.
import org.springframework.boot.test.context.SpringBootTest;
// Spring 테스트 환경에서 Mock 객체를 사용하기 위한 어노테이션입니다. @MockBean으로 선언된 객체는 Spring 컨텍스트에 등록되어, 해당 타입의 모든 @Autowired 필드에 자동으로 주입됩니다.
import org.springframework.boot.test.mock.mockito.MockBean;
// 비밀번호 인코딩을 위한 Spring Security의 구성 요소입니다.
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
// JUnit Jupiter를 사용하여 Spring TestContext Framework를 활성화하는 데 사용되는 어노테이션입니다.
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

// 정적 메소드 import를 통해 Mockito와 BDDMockito의 메소드를 직접 호출할 수 있습니다.
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.mockito.BDDMockito.given;

// Spring의 테스트 지원을 받으며, JUnit 5를 사용하여 테스트 클래스를 실행합니다.
@ExtendWith(SpringExtension.class)
@SpringBootTest
public class MemberServiceTest {

    @MockBean
    private MemberRepository memberRepository;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @MockBean
    private S3Service s3Service;

    @Autowired
    @InjectMocks
    private MemberService memberService;

    @Test
    public void signUp_Success() throws IOException {
        // Given
        MemberDTO memberDTO = new MemberDTO();
        memberDTO.setEmail("test@example.com");
        memberDTO.setNickname("testUser");
        memberDTO.setPassword("Password@123");

        when(memberRepository.existsByEmail(anyString())).thenReturn(false);
        when(memberRepository.existsByNickname(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");

        // When
        memberService.signUp(memberDTO);

        // Then
        verify(memberRepository, times(1)).save(any(Member.class));
    }

    @Test
    public void signUp_Success_WithProfileImage() throws IOException {
        // Given
        MemberDTO memberDTO = new MemberDTO();
        memberDTO.setEmail("testwithimage@example.com");
        memberDTO.setNickname("testUserImage");
        memberDTO.setPassword("Password@123");

        // 프로필 이미지를 위한 MockMultipartFile 객체 생성
        MultipartFile profileImage = new MockMultipartFile(
                "profile", // 파일 파라미터 이름
                "profile.jpg", // 파일 이름
                "image/jpeg", // 파일 타입
                "<<jpeg data>>".getBytes() // 파일 내용
        );
        memberDTO.setProfile(profileImage);

        // 모의 객체 설정
        when(memberRepository.existsByEmail(anyString())).thenReturn(false);
        when(memberRepository.existsByNickname(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        // S3Service를 통해 파일 업로드를 시뮬레이션합니다. 업로드된 파일 URL을 반환하도록 설정합니다.
        when(s3Service.uploadFile(any(MultipartFile.class), anyString(), anyString())).thenReturn("http://example.com/profile.jpg");

        // When
        memberService.signUp(memberDTO);

        // Then
        // Member 객체가 저장되었는지 확인합니다. 이때, 프로필 이미지 URL이 설정된 Member 객체가 저장되었는지 검증할 수 있습니다.
        verify(memberRepository, times(1)).save(any(Member.class));
        // S3Service의 uploadFile 메소드가 호출되었는지 확인합니다.
        verify(s3Service, times(1)).uploadFile(any(MultipartFile.class), anyString(), anyString());
    }

    @Test
    public void signUp_Fail_IfEmailExists() {
        // Given
        MemberDTO memberDTO = new MemberDTO();
        memberDTO.setEmail("existing@example.com");
        memberDTO.setNickname("testUser");
        memberDTO.setPassword("Password@123");
        // 프로필 이미지 설정은 생략됨

        when(memberRepository.existsByEmail(anyString())).thenReturn(true);

        // When & Then
        assertThrows(ResponseStatusException.class, () -> memberService.signUp(memberDTO));
    }
}

