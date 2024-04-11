package com.lv2dev.echonet.persistence;

import com.lv2dev.echonet.model.Member;
import com.lv2dev.echonet.model.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
}
