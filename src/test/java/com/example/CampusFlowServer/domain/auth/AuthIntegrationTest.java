package com.example.CampusFlowServer.domain.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.CampusFlowServer.domain.auth.entity.RefreshToken;
import com.example.CampusFlowServer.domain.auth.repository.RefreshTokenRepository;
import com.example.CampusFlowServer.domain.auth.token.RefreshTokenHasher;
import com.example.CampusFlowServer.domain.member.entity.Member;
import com.example.CampusFlowServer.domain.member.enums.MemberRole;
import com.example.CampusFlowServer.domain.member.repository.MemberRepository;
import jakarta.servlet.http.Cookie;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthIntegrationTest {

    private static final String ACCESS_TOKEN_COOKIE = "ACCESS_TOKEN";
    private static final String REFRESH_TOKEN_COOKIE = "REFRESH_TOKEN";
    private static final String CSRF_COOKIE = "XSRF-TOKEN";
    private static final String CSRF_HEADER = "X-XSRF-TOKEN";
    private static final String PASSWORD = "password123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private RefreshTokenHasher refreshTokenHasher;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void csrfTokenIsIssued() throws Exception {
        MvcResult result = mockMvc.perform(apiGet("/api/v1/csrf"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("CSRF token issued"))
            .andReturn();

        assertThat(extractSetCookieValue(result, CSRF_COOKIE)).isNotBlank();
        assertThat(countSetCookies(result, CSRF_COOKIE)).isOne();
    }

    @Test
    void loginIssuesTokenCookiesAndStoresRefreshTokenHash() throws Exception {
        Member member = saveMember("student001", "Kim Student", MemberRole.STUDENT);

        MvcResult result = login("student001")
            .andExpect(status().isOk())
            .andExpect(content().string(not(containsString("accessToken"))))
            .andExpect(content().string(not(containsString("refreshToken"))))
            .andExpect(jsonPath("$.memberId").value(member.getId()))
            .andExpect(jsonPath("$.loginId").value("student001"))
            .andExpect(jsonPath("$.name").value("Kim Student"))
            .andExpect(jsonPath("$.role").value("STUDENT"))
            .andReturn();

        String accessToken = extractSetCookieValue(result, ACCESS_TOKEN_COOKIE);
        String refreshToken = extractSetCookieValue(result, REFRESH_TOKEN_COOKIE);

        assertThat(accessToken).isNotBlank();
        assertThat(refreshToken).isNotBlank();
        assertThat(refreshTokenRepository.findByTokenHash(refreshTokenHasher.hash(refreshToken)))
            .isPresent();
    }

    @Test
    void meReturnsAuthenticatedMemberFromAccessTokenCookie() throws Exception {
        Member member = saveMember("student002", "Lee Student", MemberRole.STUDENT);
        MvcResult loginResult = login("student002")
            .andExpect(status().isOk())
            .andReturn();

        mockMvc.perform(apiGet("/api/v1/members/me")
                .cookie(extractResponseCookie(loginResult, ACCESS_TOKEN_COOKIE)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.memberId").value(member.getId()))
            .andExpect(jsonPath("$.loginId").value("student002"))
            .andExpect(jsonPath("$.name").value("Lee Student"))
            .andExpect(jsonPath("$.role").value("STUDENT"));
    }

    @Test
    void protectedApiWithoutAccessTokenReturnsUnauthenticated() throws Exception {
        mockMvc.perform(apiGet("/api/v1/members/me"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("AUTH_005"));
    }

    @Test
    void reissueIssuesNewTokenCookiesAndRevokesOldRefreshToken() throws Exception {
        saveMember("student003", "Park Student", MemberRole.STUDENT);
        MvcResult loginResult = login("student003")
            .andExpect(status().isOk())
            .andReturn();
        String oldRefreshToken = extractSetCookieValue(loginResult, REFRESH_TOKEN_COOKIE);

        Thread.sleep(1100);

        Cookie csrfCookie = getCsrfCookie();
        MvcResult reissueResult = mockMvc.perform(apiPost("/api/v1/auth/reissue")
                .cookie(extractResponseCookie(loginResult, REFRESH_TOKEN_COOKIE), csrfCookie)
                .header(CSRF_HEADER, csrfCookie.getValue()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Token reissued"))
            .andReturn();

        assertThat(extractSetCookieValue(reissueResult, ACCESS_TOKEN_COOKIE)).isNotBlank();
        assertThat(extractSetCookieValue(reissueResult, REFRESH_TOKEN_COOKIE)).isNotBlank();
        RefreshToken revokedRefreshToken = refreshTokenRepository
            .findByTokenHash(refreshTokenHasher.hash(oldRefreshToken))
            .orElseThrow();
        assertThat(revokedRefreshToken.isRevoked()).isTrue();
    }

    @Test
    void logoutExpiresTokenCookiesAndRevokesRefreshToken() throws Exception {
        saveMember("student004", "Choi Student", MemberRole.STUDENT);
        MvcResult loginResult = login("student004")
            .andExpect(status().isOk())
            .andReturn();
        String refreshToken = extractSetCookieValue(loginResult, REFRESH_TOKEN_COOKIE);

        Cookie csrfCookie = getCsrfCookie();
        MvcResult logoutResult = mockMvc.perform(apiPost("/api/v1/auth/logout")
                .cookie(extractResponseCookie(loginResult, REFRESH_TOKEN_COOKIE), csrfCookie)
                .header(CSRF_HEADER, csrfCookie.getValue()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Logged out"))
            .andReturn();

        assertThat(findSetCookie(logoutResult, ACCESS_TOKEN_COOKIE)).contains("Max-Age=0");
        assertThat(findSetCookie(logoutResult, REFRESH_TOKEN_COOKIE)).contains("Max-Age=0");
        RefreshToken revokedRefreshToken = refreshTokenRepository
            .findByTokenHash(refreshTokenHasher.hash(refreshToken))
            .orElseThrow();
        assertThat(revokedRefreshToken.isRevoked()).isTrue();
    }

    @Test
    void roleTestAllowsStudentEndpointAndDeniesProfessorEndpointForStudent() throws Exception {
        saveMember("student005", "Jung Student", MemberRole.STUDENT);
        MvcResult loginResult = login("student005")
            .andExpect(status().isOk())
            .andReturn();
        Cookie accessTokenCookie = extractResponseCookie(loginResult, ACCESS_TOKEN_COOKIE);

        mockMvc.perform(apiGet("/api/v1/auth/role-test/student").cookie(accessTokenCookie))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("STUDENT access granted"));

        mockMvc.perform(apiGet("/api/v1/auth/role-test/professor").cookie(accessTokenCookie))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("AUTH_006"));
    }

    private Member saveMember(String loginId, String name, MemberRole role) {
        Member member = BeanUtils.instantiateClass(Member.class);
        ReflectionTestUtils.setField(member, "loginId", loginId);
        ReflectionTestUtils.setField(member, "password", passwordEncoder.encode(PASSWORD));
        ReflectionTestUtils.setField(member, "name", name);
        ReflectionTestUtils.setField(member, "role", role);
        ReflectionTestUtils.setField(member, "active", true);
        return memberRepository.saveAndFlush(member);
    }

    private Cookie getCsrfCookie() throws Exception {
        MvcResult result = mockMvc.perform(apiGet("/api/v1/csrf"))
            .andExpect(status().isOk())
            .andReturn();

        return extractResponseCookie(result, CSRF_COOKIE);
    }

    private org.springframework.test.web.servlet.ResultActions login(String loginId) throws Exception {
        Cookie csrfCookie = getCsrfCookie();

        return mockMvc.perform(apiPost("/api/v1/auth/login")
            .cookie(csrfCookie)
            .header(CSRF_HEADER, csrfCookie.getValue())
            .header(HttpHeaders.USER_AGENT, "MockMvc")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"loginId":"%s","password":"%s"}
                """.formatted(loginId, PASSWORD)));
    }

    private MockHttpServletRequestBuilder apiGet(String path) {
        return get(path).servletPath(path);
    }

    private MockHttpServletRequestBuilder apiPost(String path) {
        return post(path).servletPath(path);
    }

    private Cookie extractResponseCookie(MvcResult result, String cookieName) {
        Cookie cookie = result.getResponse().getCookie(cookieName);
        if (cookie != null) {
            return cookie;
        }

        return new Cookie(cookieName, extractHeaderCookieValue(result, cookieName));
    }

    private String extractSetCookieValue(MvcResult result, String cookieName) {
        return extractResponseCookie(result, cookieName).getValue();
    }

    private String extractHeaderCookieValue(MvcResult result, String cookieName) {
        String setCookie = findSetCookie(result, cookieName);
        return setCookie.substring((cookieName + "=").length(), setCookie.indexOf(';'));
    }

    private String findSetCookie(MvcResult result, String cookieName) {
        List<String> setCookies = result.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
        return setCookies.stream()
            .filter(header -> header.startsWith(cookieName + "="))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Missing Set-Cookie: " + cookieName));
    }

    private long countSetCookies(MvcResult result, String cookieName) {
        return List.of(result.getResponse().getCookies()).stream()
            .filter(cookie -> cookieName.equals(cookie.getName()))
            .count();
    }
}
