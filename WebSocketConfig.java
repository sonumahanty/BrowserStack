package com.example.demo;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final LogWatcherHandler logWatcherHandler;

    public WebSocketConfig(LogWatcherHandler logWatcherHandler) {
        this.logWatcherHandler = logWatcherHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(logWatcherHandler, "/ws/log").setAllowedOrigins("*");
    }
}
