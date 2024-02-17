package com.lv2dev.echonet.service;

import com.lv2dev.echonet.dto.MemberDTO;
import com.lv2dev.echonet.model.Member;
import com.lv2dev.echonet.persistence.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

@Service
public class MemberService {
    private MemberRepository memberRepository;

    private PasswordEncoder passwordEncoder;

    private S3Service s3Service;

    private EmailService emailService;

    //secretKey 추가
    @Value("${secretKey}")
    private String secretKey;

    public void signUp(MemberDTO memberDTO) throws IOException {
        // 이메일 중복 확인
        if (memberRepository.existsByEmail(memberDTO.getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email already in use");
        }

        // 닉네임 중복 확인
        if (memberRepository.existsByNickname(memberDTO.getNickname())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nickname already in use");
        }

        // 비밀번호 규칙 검증
        if (!isValidPassword(memberDTO.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password does not meet the criteria");
        }

        Member member = Member.builder()
                .email(memberDTO.getEmail())
                .nickname(memberDTO.getNickname())
                .joinDay(LocalDateTime.now())
                .role(0)
                .state(0)
                .password(passwordEncoder.encode(memberDTO.getPassword())) // 비밀번호 설정
                .build();

        // 이미지가 입력되었을 때만 처리
        if (memberDTO.getProfile() != null && !memberDTO.getProfile().isEmpty()) {
            String uniqueFileName = UUID.randomUUID().toString() + "_" + System.currentTimeMillis();
            String profileUrl = s3Service.uploadFile(memberDTO.getProfile(), "member/profile", uniqueFileName);
            member.setProfile(profileUrl);
        }

        // 회원 정보 저장
        memberRepository.save(member);
    }

    private boolean isValidPassword(String password) {
        // 비밀번호 규칙: 특수문자 1개 이상, 대문자 1개 이상, 영문자, 소문자
        String passwordPattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$";
        return password.matches(passwordPattern);
    }


    /**
     * 제공된 정보를 기반으로 기존 회원의 정보를 업데이트합니다.
     * 새로운 비밀번호, 닉네임, 또는 프로필 이미지가 제공되면 해당 필드를 업데이트합니다.
     *
     * @param memberId 업데이트할 회원의 ID.
     * @param memberDTO 회원의 새 정보가 담긴 객체.
     * @param profileImage 업데이트가 요청된 새 프로필 이미지 파일; 그렇지 않으면 null.
     * @return 업데이트된 {@link Member} 엔티티.
     * @throws IOException 프로필 이미지 업로드 중 오류 발생 시.
     */
    public Member updateMemberInfo(Long memberId, MemberDTO memberDTO, MultipartFile profileImage) throws IOException {
        Member existingMember = findMemberById(memberId);
        updateMemberDetails(existingMember, memberDTO);
        updateProfileImageIfNeeded(existingMember, profileImage);
        return memberRepository.save(existingMember);
    }

    private Member findMemberById(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found with id: " + memberId));
    }

    private void updateMemberDetails(Member member, MemberDTO memberDTO) {
        if (memberDTO.getPassword() != null && !memberDTO.getPassword().isEmpty()) {
            member.setPassword(passwordEncoder.encode(memberDTO.getPassword()));
        }
        if (memberDTO.getNickname() != null && !memberDTO.getNickname().isEmpty()) {
            member.setNickname(memberDTO.getNickname());
        }
    }

    private void updateProfileImageIfNeeded(Member member, MultipartFile profileImage) throws IOException {
        if (profileImage != null && !profileImage.isEmpty()) {
            String profileUrl = uploadProfileImage(profileImage);
            member.setProfile(profileUrl);
        }
    }

    private String uploadProfileImage(MultipartFile profileImage) throws IOException {
        String uniqueFileName = UUID.randomUUID().toString() + "_" + System.currentTimeMillis();
        return s3Service.uploadFile(profileImage, "member/profile", uniqueFileName);
    }

    /**
     * 사용자의 비밀번호를 재설정합니다.
     *
     * @param email 비밀번호를 재설정하려는 사용자의 이메일 주소입니다.
     * @param newPassword 사용자가 설정할 새로운 비밀번호입니다.
     * @throws ResponseStatusException 사용자를 찾을 수 없거나, 기타 오류 발생 시 예외를 발생시킵니다.
     */
    public void resetPassword(String email, String newPassword) {
        // 사용자 이메일로 회원 정보 조회
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with email: " + email));

        // 새 비밀번호를 암호화
        String encodedPassword = passwordEncoder.encode(newPassword);

        // 암호화된 비밀번호로 회원 정보 업데이트
        member.setPassword(encodedPassword);
        memberRepository.save(member);

        // 변경 성공 후 회원에게 이메일 전송
        emailService.sendEmailNotification(email, "비밀번호 변경 알림", "귀하의 비밀번호가 성공적으로 변경되었습니다.");
    }


    /**
     * 이메일 주소로 사용자를 찾습니다.
     *
     * @param email 사용자를 찾으려는 이메일 주소입니다.
     * @return Member 이메일 주소를 가진 사용자입니다.
     * @throws ResponseStatusException 사용자를 찾을 수 없거나, 기타 오류 발생 시 예외를 발생시킵니다.
     */
    public Member findMemberByEmail(String email) {
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not found"));
    }


    /**
     * 사용자가 비밀번호를 잊어버렸을 때, 이메일을 통해 비밀번호 재설정 링크를 요청할 수 있는 기능을 추가합니다.
     * 이 메소드는 임시 비밀번호를 생성하고 이를 이메일로 전송하는 기능을 수행합니다.
     *
     * @param email 비밀번호를 재설정하려는 사용자의 이메일 주소입니다.
     * @throws ResponseStatusException 사용자를 찾을 수 없거나, 기타 오류 발생 시 예외를 발생시킵니다.
     */
    public void requestPasswordReset(String email) {
        // 사용자 이메일로 회원 정보 조회
        Member member = findMemberByEmail(email);

        // 임시 비밀번호 생성
        String tempPassword = generateTempPassword();

        // 임시 비밀번호를 암호화하여 회원 정보 업데이트
        member.setPassword(passwordEncoder.encode(tempPassword));
        memberRepository.save(member);

        // 임시 비밀번호를 이메일로 전송
        emailService.sendEmailNotification(email, "비밀번호 재설정 요청", "귀하의 임시 비밀번호는 " + tempPassword + "입니다.");
    }

    /**
     * 임시 비밀번호를 생성하는 메소드입니다.
     * 이 메소드는 8자리의 랜덤한 문자열을 생성하여 반환합니다.
     *
     * @return String 생성된 임시 비밀번호입니다.
     */
    private String generateTempPassword() {
        // 비밀번호에 사용될 문자 세트
        String chars = secretKey;

        // 랜덤 객체 생성
        Random rnd = new Random();

        // 8자리의 랜덤 문자열 생성
        StringBuilder tempPassword = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            tempPassword.append(chars.charAt(rnd.nextInt(chars.length())));
        }

        return tempPassword.toString();
    }

    /**
     * 사용자의 닉네임을 변경합니다.
     *
     * @param memberId 변경하려는 사용자의 ID.
     * @param newNickname 사용자가 설정할 새로운 닉네임.
     */
    public void changeNickname(Long memberId, String newNickname) {
        Member member = findMemberById(memberId);
        member.setNickname(newNickname);
        memberRepository.save(member);
    }

    /**
     * 사용자의 프로필 이미지를 변경합니다.
     *
     * @param memberId 변경하려는 사용자의 ID.
     * @param newProfileImage 사용자가 설정할 새로운 프로필 이미지.
     * @throws IOException 프로필 이미지 업로드 중 오류 발생 시.
     */
    public void changeProfileImage(Long memberId, MultipartFile newProfileImage) throws IOException {
        Member member = findMemberById(memberId);
        String profileUrl = uploadProfileImage(newProfileImage);
        member.setProfile(profileUrl);
        memberRepository.save(member);
    }


}
