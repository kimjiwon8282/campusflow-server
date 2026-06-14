package com.example.CampusFlowServer.domain.auth.handler;

import com.example.CampusFlowServer.domain.auth.exception.AuthErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class AuthSecurityResponseWriter {

    private final ObjectMapper objectMapper;

    public void write(HttpServletResponse response, AuthErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        objectMapper.writeValue(
            response.getWriter(),
            Map.of(
                "code", errorCode.getCode(),
                "message", errorCode.getMessage()
            )
        );
    }
}
