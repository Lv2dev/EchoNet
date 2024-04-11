package com.lv2dev.echonet.service;

import com.lv2dev.echonet.dto.MemberDTO;
import com.lv2dev.echonet.model.LoginHistory;
import com.lv2dev.echonet.model.Member;
import com.lv2dev.echonet.model.PasswordResetToken;
import com.lv2dev.echonet.persistence.LoginHistoryRepository;
import com.lv2dev.echonet.persistence.MemberRepository;
import com.lv2dev.echonet.persistence.PasswordResetTokenRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

@Service
public class MemberService {
    private MemberRepository memberRepository;

    private PasswordEncoder passwordEncoder;

    private S3Service s3Service;

    private EmailService emailService;

    private PasswordResetTokenRepository passwordResetTokenRepository;

    private LoginHistoryRepository loginHistoryRepository;

    private NotificationService notificationService;

    // 최대 로그인 시도 횟수
    @Value("${maxLoginAttempt}")
    private int MAX_LOGIN_ATTEMPT;

    // 계정 잠금 시간 (시간 단위)
    @Value("${lockTimeHours}")
    private int LOCK_TIME_HOURS;

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

    public Member findMemberById(Long memberId) {
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
    /**
     * 사용자가 시스템에 로그인하는 메서드입니다.
     * 이메일과 비밀번호를 인자로 받아 해당 정보가 유효한지 검증합니다.
     *
     * @param email 사용자가 입력한 이메일 주소입니다.
     * @param password 사용자가 입력한 비밀번호입니다.
     * @return Member 로그인에 성공한 사용자의 정보를 담고 있는 Member 객체를 반환합니다.
     * @throws UsernameNotFoundException 입력한 이메일에 해당하는 사용자를 찾을 수 없을 때 발생합니다.
     * @throws LockedException 사용자가 로그인 시도 횟수 제한을 초과하여 계정이 잠겼을 때 발생합니다.
     * @throws BadCredentialsException 입력한 비밀번호가 일치하지 않을 때 발생합니다.
     */
    public Member login(String email, String password) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        if (member.getLoginAttempt() >= MAX_LOGIN_ATTEMPT) {
            if (member.getLastLoginAttempt().isBefore(LocalDateTime.now().minusHours(LOCK_TIME_HOURS))) {
                member.setLoginAttempt(0);
            } else {
                throw new LockedException("Account is locked due to too many failed login attempts. Please try again later.");
            }
        }

        if (!passwordEncoder.matches(password, member.getPassword())) {
            member.setLoginAttempt(member.getLoginAttempt() + 1);
            member.setLastLoginAttempt(LocalDateTime.now());
            memberRepository.save(member);
            throw new BadCredentialsException("Invalid password");
        }

        member.setLoginAttempt(0);
        memberRepository.save(member);

        return member;
    }

    /**
     * 회원 탈퇴 기능을 수행하는 메소드입니다.
     * 이 메소드는 회원의 ID를 인자로 받아 해당 회원을 데이터베이스에서 삭제합니다.
     *
     * 구현 시에는 비밀번호를 입력받아 인증을 수행하도록 구현한다.
     *
     * @param memberId 탈퇴하려는 회원의 ID.
     * @throws IllegalArgumentException 해당 ID를 가진 회원이 없을 경우 발생합니다.
     */
    public void deleteMember(Long memberId) {
        // 회원 존재 여부 확인
        if (!memberRepository.existsById(memberId)) {
            throw new IllegalArgumentException("Member not found with id: " + memberId);
        }

        // 회원 삭제
        memberRepository.deleteById(memberId);
    }


    /**
     * 로그인 기록을 생성하는 메소드입니다.
     * 로그인한 회원의 정보, 로그인 시간, IP 주소, 브라우저 정보, 디바이스 정보를 설정한 후에 이를 저장합니다.
     *
     * @param member 로그인한 회원
     * @param request HttpServletRequest 객체
     */
    public void createLoginHistory(Member member, HttpServletRequest request) {
        // 로그인 기록 객체를 생성합니다.
        LoginHistory loginHistory = new LoginHistory();

        // 로그인한 회원의 정보를 설정합니다.
        loginHistory.setMember(member);

        // 로그인 시간을 설정합니다.
        loginHistory.setLoginTime(LocalDateTime.now());

        // IP 주소를 설정합니다.
        loginHistory.setIpAddress(request.getRemoteAddr());

        // 브라우저 정보를 설정합니다.
        loginHistory.setBrowserInfo(request.getHeader("User-Agent"));

        // 디바이스 정보를 설정합니다. 실제 환경에서는 디바이스 정보를 얻는 라이브러리를 사용할 수 있습니다.
        loginHistory.setDeviceInfo("Unknown");

        // 로그인 기록을 저장합니다.
        loginHistoryRepository.save(loginHistory);
    }


    /**
     * 사용자의 비밀번호를 변경합니다.
     *
     * @param email        비밀번호를 변경하려는 사용자의 이메일 주소입니다.
     * @param currentPassword 사용자의 현재 비밀번호입니다.
     * @param newPassword  사용자가 설정할 새로운 비밀번호입니다.
     * @throws IllegalArgumentException 현재 비밀번호가 올바르지 않거나, 사용자를 찾을 수 없는 경우 예외를 발생시킵니다.
     */
    public void changePassword(String email, String currentPassword, String newPassword) {
        // 이메일을 통해 사용자를 찾습니다.
        Optional<Member> memberOptional = memberRepository.findByEmail(email);

        Member member = memberOptional.orElseThrow(() -> new IllegalArgumentException("User not found"));

        // 사용자를 찾을 수 없는 경우 예외를 발생시킵니다.
        if (member == null) {
            throw new IllegalArgumentException("User not found");
        }

        // 현재 비밀번호가 일치하는지 확인합니다.
        if (!passwordEncoder.matches(currentPassword, member.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        // 비밀번호가 일치하면 새로운 비밀번호로 변경합니다.
        member.setPassword(passwordEncoder.encode(newPassword));
        memberRepository.save(member);
    }


    /**
     * 사용자를 위한 비밀번호 재설정 토큰을 생성하고, 이메일로 토큰을 보냅니다.
     *
     * @param user 토큰을 발급받는 사용자
     * @param token 비밀번호 재설정 토큰 문자열
     */
    public void createPasswordResetTokenForUser(final Member user, final String token) {
        final PasswordResetToken myToken = new PasswordResetToken(token, user);
        passwordResetTokenRepository.save(myToken);
        notificationService.sendEmailNotification(user.getEmail(), "Password reset request",
                "To reset your password, click the link below:\n" +
                        "http://localhost:8080/user/resetPassword?token=" + token);
    }

}
