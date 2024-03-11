package com.lv2dev.echonet.persistence;

import com.lv2dev.echonet.model.LoginHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 로그인 기록을 데이터베이스에 저장하는 인터페이스입니다.
 */
@Repository
public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Long> {
}
