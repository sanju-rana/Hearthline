package com.restaurant.websocket;

import com.restaurant.security.CookieService;
import com.restaurant.security.JwtAuthenticator;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final CookieService cookies;
    private final JwtAuthenticator authenticator;

    public JwtHandshakeInterceptor(CookieService cookies, JwtAuthenticator authenticator) {
        this.cookies = cookies;
        this.authenticator = authenticator;
    }

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            String token = cookies.read(servletRequest.getServletRequest());
            if (token != null) {
                Authentication authentication = authenticator.authenticate(token);
                if (authentication != null) {
                    attributes.put("AUTH", authentication);
                }
            }
        }
        return true;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception
    ) {
        // The user principal is copied during the handshake. Nothing else is required here.
    }
}
