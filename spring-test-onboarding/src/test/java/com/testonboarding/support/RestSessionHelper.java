package com.testonboarding.support;

import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Collections;
import java.util.List;

/**
 * E2E 테스트용 세션 도우미 — 진짜 HTTP로 "브라우저가 하는 일"을 흉내낸다.
 *
 * 브라우저는 쿠키를 자동 관리하지만 TestRestTemplate은 그러지 않는다.
 * 그래서 이 클래스가 대신:
 * 1. CSRF 토큰 쿠키(XSRF-TOKEN)를 받아 보관하고, 쓰기 요청마다 헤더(X-XSRF-TOKEN)로 동봉
 * 2. 로그인 성공 시 세션 쿠키(JSESSIONID)를 보관하고, 이후 요청마다 동봉
 *
 * 사용 예:
 *   RestSessionHelper session = new RestSessionHelper(restTemplate);
 *   session.login("writer1", "spring123!");
 *   session.post("/api/posts", new PostCreateRequest("제목", "내용"), Void.class);
 */
public class RestSessionHelper {

    private static final String CSRF_COOKIE = "XSRF-TOKEN";
    private static final String CSRF_HEADER = "X-XSRF-TOKEN";
    private static final String SESSION_COOKIE = "JSESSIONID";

    private final TestRestTemplate restTemplate;
    private String csrfToken;
    private String sessionId;

    public RestSessionHelper(TestRestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * 아무 GET이나 한 번 호출하면 서버가 XSRF-TOKEN 쿠키를 발급한다 (CookieCsrfTokenRepository).
     */
    public void fetchCsrfToken() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/posts", String.class);
        String token = extractCookie(response, CSRF_COOKIE);
        if (token != null) {
            this.csrfToken = token;
        }
    }

    /**
     * 폼 로그인. 성공(200)이면 세션 쿠키를 보관하고 true, 실패(401)면 false.
     */
    public boolean login(String username, String password) {
        ensureCsrfToken();

        HttpHeaders headers = baseHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("username", username);
        form.add("password", password);

        ResponseEntity<String> response =
                restTemplate.postForEntity("/login", new HttpEntity<>(form, headers), String.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            String newSession = extractCookie(response, SESSION_COOKIE);
            if (newSession != null) {
                this.sessionId = newSession;
            }
            return true;
        }
        return false;
    }

    public <T> ResponseEntity<T> get(String path, Class<T> responseType) {
        return restTemplate.exchange(path, HttpMethod.GET,
                new HttpEntity<>(baseHeaders()), responseType);
    }

    public <T> ResponseEntity<T> post(String path, Object body, Class<T> responseType) {
        HttpHeaders headers = baseHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.exchange(path, HttpMethod.POST,
                new HttpEntity<>(body, headers), responseType);
    }

    /**
     * 보관 중인 쿠키(세션 + CSRF)와 CSRF 헤더를 실은 공통 헤더.
     */
    private HttpHeaders baseHeaders() {
        HttpHeaders headers = new HttpHeaders();
        StringBuilder cookies = new StringBuilder();
        if (sessionId != null) {
            cookies.append(SESSION_COOKIE).append('=').append(sessionId);
        }
        if (csrfToken != null) {
            if (cookies.length() > 0) {
                cookies.append("; ");
            }
            cookies.append(CSRF_COOKIE).append('=').append(csrfToken);
            headers.add(CSRF_HEADER, csrfToken);
        }
        if (cookies.length() > 0) {
            headers.add(HttpHeaders.COOKIE, cookies.toString());
        }
        return headers;
    }

    private void ensureCsrfToken() {
        if (csrfToken == null) {
            fetchCsrfToken();
        }
    }

    /**
     * Set-Cookie 응답 헤더들에서 원하는 쿠키 값을 꺼낸다.
     * 예: "JSESSIONID=ABC123; Path=/; HttpOnly" → "ABC123"
     */
    private String extractCookie(ResponseEntity<?> response, String cookieName) {
        List<String> setCookies = response.getHeaders()
                .getOrDefault(HttpHeaders.SET_COOKIE, Collections.emptyList());
        for (String setCookie : setCookies) {
            if (setCookie.startsWith(cookieName + "=")) {
                String value = setCookie.substring(cookieName.length() + 1);
                int semicolon = value.indexOf(';');
                return semicolon >= 0 ? value.substring(0, semicolon) : value;
            }
        }
        return null;
    }
}
