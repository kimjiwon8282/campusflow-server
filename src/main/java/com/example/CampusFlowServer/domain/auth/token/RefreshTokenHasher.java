package com.example.CampusFlowServer.domain.auth.token;

import com.example.CampusFlowServer.domain.auth.exception.AuthErrorCode;
import com.example.CampusFlowServer.domain.auth.exception.AuthException;
import com.example.CampusFlowServer.domain.auth.properties.JwtProperties;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class RefreshTokenHasher {

    private static final String HMAC_SHA256 = "HmacSHA256";

    private final JwtProperties jwtProperties;

    public String hash(String rawRefreshToken) {
        if (!StringUtils.hasText(rawRefreshToken)) {
            throw new AuthException(AuthErrorCode.INVALID_TOKEN);
        }

        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec keySpec = new SecretKeySpec(
                jwtProperties.getRefreshSecret().getBytes(StandardCharsets.UTF_8),
                HMAC_SHA256
            );
            mac.init(keySpec);
            byte[] digest = mac.doFinal(rawRefreshToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception e) {
            throw new AuthException(AuthErrorCode.INVALID_TOKEN);
        }
    }
}
