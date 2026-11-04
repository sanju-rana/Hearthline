package com.restaurant.config;

import com.restaurant.websocket.JwtHandshakeHandler;
import com.restaurant.websocket.JwtHandshakeInterceptor;
import com.restaurant.websocket.SubscriptionGuard;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtHandshakeInterceptor handshakeInterceptor;
    private final SubscriptionGuard subscriptionGuard;
    private final AppProperties properties;

    public WebSocketConfig(
            JwtHandshakeInterceptor handshakeInterceptor,
            SubscriptionGuard subscriptionGuard,
            AppProperties properties
    ) {
        this.handshakeInterceptor = handshakeInterceptor;
        this.subscriptionGuard = subscriptionGuard;
        this.properties = properties;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setHandshakeHandler(new JwtHandshakeHandler())
                .addInterceptors(handshakeInterceptor)
                .setAllowedOriginPatterns(properties.cors().allowedOrigins().toArray(String[]::new))
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(subscriptionGuard);
    }
}
