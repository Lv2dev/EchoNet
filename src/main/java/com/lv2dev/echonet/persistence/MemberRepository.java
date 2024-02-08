package com.lv2dev.echonet.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lv2dev.echonet.model.Member;

import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Integer> {
    /**
     * 이메일로 찾기
     * */
    Optional<Member> findByEmail(String email);

    /**
     * existsByEmail
     * */
    boolean existsByEmail(String email);

    /**
     * 닉네임 중복 체크
     * */
    boolean existsByNickname(String nickname); // 닉네임 중복 체크를 위한 메소드

    /**
     * 아이디로 찾기
     * */
    Optional<Member> findById(Long id);

}
