package com.testonboarding.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 루트 진입점 — 글 목록으로 안내.
 */
@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "redirect:/posts";
    }
}
