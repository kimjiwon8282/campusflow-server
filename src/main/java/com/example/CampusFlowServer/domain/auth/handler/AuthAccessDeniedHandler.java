package com.example.CampusFlowServer.domain.auth.handler;

import com.example.CampusFlowServer.domain.auth.exception.AuthErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthAccessDeniedHandler implements AccessDeniedHandler {

    private final AuthSecurityResponseWriter responseWriter;

    @Override
    public void handle(
        HttpServletRequest request,
        HttpServletResponse response,
        AccessDeniedException accessDeniedException
    ) throws IOException {
        AuthErrorCode errorCode = accessDeniedException instanceof CsrfException
            ? AuthErrorCode.CSRF_TOKEN_INVALID
            : AuthErrorCode.ACCESS_DENIED;

        responseWriter.write(response, errorCode);
    }
}
