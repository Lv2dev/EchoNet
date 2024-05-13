package com.lv2dev.echonet.persistence;

import com.lv2dev.echonet.model.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
}
