package com.lv2dev.echonet.controller;

import com.lv2dev.echonet.model.Feedback;
import com.lv2dev.echonet.service.FeedbackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/feedbacks")
public class FeedbackController {
    @Autowired
    private FeedbackService feedbackService;

    @PostMapping
    public Feedback createFeedback(@RequestParam Long userId, @RequestParam String content) {
        return feedbackService.createFeedback(userId, content);
    }

    @GetMapping
    public List<Feedback> getAllFeedbacks() {
        return feedbackService.getAllFeedbacks();
    }
}
