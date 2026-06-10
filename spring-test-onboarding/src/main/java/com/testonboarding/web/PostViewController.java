package com.testonboarding.web;

import com.testonboarding.board.dto.PostCreateRequest;
import com.testonboarding.board.service.BoardService;
import com.testonboarding.comment.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.validation.Valid;
import java.security.Principal;

/**
 * 게시판 화면(SSR) — Step 12 테스트 대상.
 *
 * REST(BoardController)와의 차이:
 * - @Controller (not @RestController): 반환 String은 "뷰 이름" — Thymeleaf 템플릿으로 렌더링된다
 * - 데이터는 Model에 담아 템플릿에 전달
 * - 폼 제출 성공 후에는 redirect (PRG 패턴: 새로고침 시 중복 제출 방지)
 * - 검증 실패 시 같은 폼 뷰를 다시 반환 (입력값과 에러가 유지된다)
 *
 * 비즈니스 로직은 REST와 같은 Service를 재사용한다 — 화면이 늘어도 규칙은 한 곳에.
 */
@Controller
@RequestMapping("/posts")
@RequiredArgsConstructor
public class PostViewController {

    private final BoardService boardService;
    private final CommentService commentService;

    @GetMapping
    public String list(@RequestParam(defaultValue = "1") int page, Model model) {
        model.addAttribute("posts", boardService.getPosts(page, 10));
        model.addAttribute("page", page);
        return "post/list";
    }

    @GetMapping("/{postId}")
    public String detail(@PathVariable Long postId, Model model) {
        model.addAttribute("post", boardService.getPost(postId));
        model.addAttribute("comments", commentService.getComments(postId));
        return "post/detail";
    }

    /**
     * 글쓰기 폼 — SecurityConfig에서 인증 필요(/posts/new).
     * 미인증 브라우저는 401이 아니라 로그인 페이지로 redirect된다 (EntryPoint 분기).
     */
    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("postCreateRequest", new PostCreateRequest());
        return "post/form";
    }

    /**
     * 폼 제출 — JSON 대신 폼 파라미터가 @ModelAttribute로 바인딩된다.
     * BindingResult가 @Valid 바로 뒤에 와야 검증 실패가 400 대신 "폼 재표시"로 흐른다.
     */
    @PostMapping
    public String create(@Valid @ModelAttribute PostCreateRequest postCreateRequest,
                         BindingResult bindingResult,
                         Principal principal) {
        if (bindingResult.hasErrors()) {
            return "post/form"; // 검증 실패 → 입력값/에러를 유지한 채 폼 재표시
        }

        Long postId = boardService.createPost(principal.getName(), postCreateRequest);
        return "redirect:/posts/" + postId; // PRG: 새로고침해도 중복 등록되지 않는다
    }
}
