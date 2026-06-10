package com.testonboarding.board.controller;

import com.testonboarding.board.service.BoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 관리자 API — SecurityConfig(hasRole ADMIN) + AdminCheckInterceptor의 이중 보호를 받는다.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final BoardService boardService;

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("postCount", boardService.countPosts());
        return stats;
    }
}
