package com.lv2dev.echonet.auth;

import com.lv2dev.echonet.dto.MemberDTO;
import com.lv2dev.echonet.model.Member;
import com.lv2dev.echonet.persistence.MemberRepository;
import com.lv2dev.echonet.service.MemberService;
import com.lv2dev.echonet.service.S3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.BDDMockito.given;

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

    private Member existingMember;

    /**
     * 성공적으로 회원가입을 처리하는 경우를 테스트합니다.
     * 이메일과 닉네임이 중복되지 않으며 비밀번호가 요구 사항을 충족하는 경우에 대한 검증을 포함합니다.
     */
    @Test
    public void signUp_Success() throws IOException {
        // Given: 주어진 조건
        MemberDTO memberDTO = new MemberDTO();
        memberDTO.setEmail("test@example.com");
        memberDTO.setNickname("testUser");
        memberDTO.setPassword("Password@123");

        // 모의 객체를 설정하여, 이메일과 닉네임이 중복되지 않음을 시뮬레이션합니다.
        when(memberRepository.existsByEmail(anyString())).thenReturn(false);
        when(memberRepository.existsByNickname(anyString())).thenReturn(false);
        // 비밀번호 인코딩을 시뮬레이션합니다.
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");

        // When: 실행할 작업
        memberService.signUp(memberDTO);

        // Then: 기대하는 결과 검증
        verify(memberRepository, times(1)).save(any(Member.class));
    }

    /**
     * 프로필 이미지와 함께 성공적으로 회원가입을 처리하는 경우를 테스트합니다.
     * 프로필 이미지가 S3에 업로드되고, 해당 URL이 Member 객체에 저장되는 과정을 검증합니다.
     */
    @Test
    public void signUp_Success_WithProfileImage() throws IOException {
        // Given: 주어진 조건
        MemberDTO memberDTO = new MemberDTO();
        memberDTO.setEmail("testwithimage@example.com");
        memberDTO.setNickname("testUserImage");
        memberDTO.setPassword("Password@123");

        MultipartFile profileImage = new MockMultipartFile("profile", "profile.jpg", "image/jpeg", "<<jpeg data>>".getBytes());
        memberDTO.setProfile(profileImage);

        when(memberRepository.existsByEmail(anyString())).thenReturn(false);
        when(memberRepository.existsByNickname(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(s3Service.uploadFile(any(MultipartFile.class), anyString(), anyString())).thenReturn("http://example.com/profile.jpg");

        // When: 실행할 작업
        memberService.signUp(memberDTO);

        // Then: 기대하는 결과 검증
        verify(memberRepository, times(1)).save(any(Member.class));
        verify(s3Service, times(1)).uploadFile(any(MultipartFile.class), anyString(), anyString());
    }

    /**
     * 이메일이 이미 존재하는 경우 회원가입 실패를 테스트합니다.
     * 이미 존재하는 이메일로 회원가입 시도 시, ResponseStatusException 예외가 발생하는지 검증합니다.
     */
    @Test
    public void signUp_Fail_IfEmailExists() {
        // Given: 주어진 조건
        MemberDTO memberDTO = new MemberDTO();
        memberDTO.setEmail("existing@example.com");
        memberDTO.setNickname("testUser");
        memberDTO.setPassword("Password@123");

        // 이메일이 이미 존재함을 시뮬레이션합니다.
        when(memberRepository.existsByEmail(anyString())).thenReturn(true);

        // When & Then: 실행 및 예외 발생 검증
        assertThrows(ResponseStatusException.class, () -> memberService.signUp(memberDTO));
    }

    /**
     * 회원 정보 업데이트를 테스트합니다.
     * 새 닉네임과 프로필 이미지가 제공되면, 해당 정보로 기존 회원 정보가 업데이트되어야 합니다.
     */
    @Test
    @DisplayName("회원 정보 업데이트 테스트")
    void updateMemberInfoTest() throws IOException {
        Long memberId = 1L;
        Member existingMember = new Member();
        existingMember.setId(memberId);
        existingMember.setEmail("test@example.com");
        existingMember.setJoinDay(LocalDateTime.now());

        MemberDTO memberDTO = new MemberDTO();
        memberDTO.setNickname("updatedUser");

        MockMultipartFile profileImage = new MockMultipartFile("profile", "profile.png", "image/png", "imageData".getBytes());

        // memberId에 해당하는 Member 객체를 조회하려 할 때, 항상 특정 Member 객체를 반환하도록 설정합니다.
        // 이렇게 함으로써, 실제 데이터베이스에 접근하지 않아도 테스트 케이스에서는 Member 객체가 정상적으로 조회되는 것처럼 동작합니다.
        when(memberRepository.findById(anyLong())).thenReturn(Optional.of(existingMember));

        // S3Service의 uploadFile 메서드가 호출될 때, 어떤 MultipartFile 객체가 전달되든지 "updatedProfileUrl" 문자열을 반환하도록 설정합니다.
        // 이는 실제로 외부 S3 스토리지에 파일을 업로드하지 않고도, 파일 업로드가 성공적으로 이루어진 것처럼 테스트를 진행할 수 있게 합니다.
        when(s3Service.uploadFile(any(MultipartFile.class), anyString(), anyString())).thenReturn("updatedProfileUrl");

        // MemberRepository의 save 메서드가 호출될 때, 전달된 Member 객체를 그대로 반환하도록 설정합니다.
        // 이는 save 메서드의 동작을 시뮬레이션하며, 실제로 데이터베이스에 데이터를 저장하지 않고도 save 메서드 호출 시
        // 인자로 전달된 객체가 반환되는 것처럼 테스트 환경을 구성합니다.
        // thenAnswer 메서드를 사용하여 메서드 호출 시 전달된 인자(invocation.getArgument(0))를 그대로 반환합니다.
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Member updatedMember = memberService.updateMemberInfo(memberId, memberDTO, profileImage);

        verify(memberRepository, times(1)).save(any(Member.class));
        assert(updatedMember.getNickname().equals(memberDTO.getNickname()));
        assert(updatedMember.getProfile().equals("updatedProfileUrl"));
    }

    @BeforeEach
    void setUp() {
        existingMember = new Member();
        existingMember.setId(1L);
        existingMember.setEmail("existing@example.com");
        existingMember.setNickname("ExistingUser");
        existingMember.setPassword("existingPassword");
        existingMember.setProfile("http://example.com/existingProfile.jpg");

        when(memberRepository.findById(anyLong())).thenReturn(Optional.of(existingMember));
    }

    /**
     * 이메일 업데이트를 테스트합니다.
     * 새로운 이메일이 제공되면 해당 이메일로 회원 정보가 업데이트되어야 합니다.
     */
    @Test
    @DisplayName("이메일 업데이트 테스트")
    void updateMemberEmail() throws IOException {
        MemberDTO memberDTO = new MemberDTO();
        memberDTO.setEmail("updated@example.com");

        memberService.updateMemberInfo(1L, memberDTO, null);

        assertEquals("updated@example.com", existingMember.getEmail());
        verify(memberRepository, times(1)).save(any(Member.class));
    }

    /**
     * 닉네임 업데이트를 테스트합니다.
     * 새로운 닉네임이 제공되면 해당 닉네임으로 회원 정보가 업데이트되어야 합니다.
     */
    @Test
    @DisplayName("닉네임 업데이트 테스트")
    void updateMemberNickname() throws IOException {
        MemberDTO memberDTO = new MemberDTO();
        memberDTO.setNickname("NewNickname");

        memberService.updateMemberInfo(1L, memberDTO, null);

        assertEquals("NewNickname", existingMember.getNickname());
        verify(memberRepository, times(1)).save(any(Member.class));
    }

    /**
     * 비밀번호 업데이트를 테스트합니다.
     * 새로운 비밀번호가 제공되면 해당 비밀번호로 회원 정보가 업데이트되어야 합니다.
     */
    @Test
    @DisplayName("비밀번호 업데이트 테스트")
    void updateMemberPassword() throws IOException {
        MemberDTO memberDTO = new MemberDTO();
        memberDTO.setPassword("NewPassword123!");

        when(passwordEncoder.encode("NewPassword123!")).thenReturn("encodedNewPassword");

        memberService.updateMemberInfo(1L, memberDTO, null);

        assertEquals("encodedNewPassword", existingMember.getPassword());
        verify(passwordEncoder, times(1)).encode("NewPassword123!");
        verify(memberRepository, times(1)).save(any(Member.class));
    }

    /**
     * 프로필 이미지 업데이트를 테스트합니다.
     * 새로운 프로필 이미지가 제공되면 해당 이미지로 회원 정보가 업데이트되어야 합니다.
     */
    @Test
    @DisplayName("프로필 이미지 업데이트 테스트")
    void updateMemberProfileImage() throws IOException {
        MultipartFile newProfileImage = new MockMultipartFile("newProfile", "newProfile.jpg", "image/jpeg", "new image content".getBytes());

        when(s3Service.uploadFile(any(MultipartFile.class), anyString(), anyString())).thenReturn("http://example.com/newProfile.jpg");

        memberService.updateMemberInfo(1L, new MemberDTO(), newProfileImage);

        assertEquals("http://example.com/newProfile.jpg", existingMember.getProfile());
        verify(s3Service, times(1)).uploadFile(any(MultipartFile.class), anyString(), anyString());
        verify(memberRepository, times(1)).save(any(Member.class));
    }

    /**
     * 비밀번호 재설정 요청이 성공적으로 이메일로 임시 비밀번호를 전송하는지 테스트합니다.
     */
    @Test
    public void testRequestPasswordReset_Successful() {
        // 모의 설정
        String email = "user@example.com";
        Member mockMember = new Member();
        mockMember.setEmail(email);
        when(memberRepository.findByEmail(email)).thenReturn(Optional.of(mockMember));
        when(passwordEncoder.encode(anyString())).thenReturn("encryptedPassword");

        // 테스트 대상 메소드 실행
        memberService.requestPasswordReset(email);

        // 상호작용 및 결과 검증
        verify(memberRepository).save(any(Member.class));
    }

    /**
     * 존재하지 않는 이메일에 대한 비밀번호 재설정 요청 시 ResponseStatusException이 발생하는지 테스트합니다.
     */
    @Test
    public void testRequestPasswordReset_UserNotFound_ThrowsException() {
        // 존재하지 않는 사용자 설정
        String email = "nonexistent@example.com";
        when(memberRepository.findByEmail(email)).thenReturn(null);

        // 예외가 발생하는지 확인
        assertThrows(ResponseStatusException.class, () -> memberService.requestPasswordReset(email));
    }

    /**
     * 회원 탈퇴 기능을 테스트하는 메소드입니다.
     * 먼저, 테스트에 필요한 Member 객체를 생성하고 저장합니다.
     * 그 다음, deleteMember 메소드를 호출하여 회원을 삭제합니다.
     * 마지막으로, 해당 회원이 더 이상 데이터베이스에 존재하지 않는지 확인하여 deleteMember 메소드가 성공적으로 실행되었는지 확인합니다.
     */
    @Test
    public void testDeleteMember() {
        // 테스트에 필요한 Member 객체를 생성하고 저장합니다.
        Member member = new Member();
        member.setId(1L);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        doNothing().when(memberRepository).deleteById(any(Long.class));

        // deleteMember 메소드를 호출하여 회원을 삭제합니다.
        memberService.deleteMember(1L);

        // 해당 회원이 더 이상 데이터베이스에 존재하지 않는지 확인하여 deleteMember 메소드가 성공적으로 실행되었는지 확인합니다.
        assertFalse(memberRepository.findById(1L).isPresent());
    }

    /**
     * 사용자 로그인 기록 저장 테스트.
     * MemberService의 createLoginHistory 메서드를 호출하고,
     * 해당 메서드가 예상대로 동작하는지 검증합니다.
     */
    @Test
    public void createLoginHistoryTest() {
        // Given
        Member member = new Member();
        member.setEmail("test@test.com");
        member.setPassword("password");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");

        // When
        memberService.createLoginHistory(member, request);

        // Then
        verify(memberService).createLoginHistory(member, request);
    }
}