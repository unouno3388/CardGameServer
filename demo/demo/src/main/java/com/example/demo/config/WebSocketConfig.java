package com.example.demo.config;

import com.example.demo.handler.GameAIWebSocketHandler; // 新增：AI模式處理器
import com.example.demo.handler.GameRoomWebSocketHandler; // 新增：房間模式處理器
// GameWebSocketHandler 可以保留給其他用途，或如果其功能完全被新處理器覆蓋則移除
// import com.example.demo.handler.GameWebSocketHandler;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final GameAIWebSocketHandler gameAIWebSocketHandler;
    private final GameRoomWebSocketHandler gameRoomWebSocketHandler;

    // 更新建構函數以注入新的處理器
    public WebSocketConfig(GameAIWebSocketHandler gameAIWebSocketHandler, GameRoomWebSocketHandler gameRoomWebSocketHandler) {
        this.gameAIWebSocketHandler = gameAIWebSocketHandler;
        this.gameRoomWebSocketHandler = gameRoomWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(@NonNull WebSocketHandlerRegistry registry) {
        // 線上單人模式 (AI在伺服器) 的端點
        registry.addHandler(gameAIWebSocketHandler, "/game/ai")
                .setAllowedOriginPatterns("*"); // 允許所有來源的跨域請求

        // 線上雙人房間模式的端點
        registry.addHandler(gameRoomWebSocketHandler, "/game/room")
                .setAllowedOriginPatterns("*"); // 允許所有來源的跨域請求

        // 如果您還想保留舊的 "/game" 端點，可以取消註解下面的程式碼
        // registry.addHandler(new GameWebSocketHandler(), "/game")
        // .setAllowedOriginPatterns("*");
    }

    // 如果您的處理器沒有使用 @Component 標註，可以在這裡將它們定義為 Bean
    // (假設它們將會是 @Component，這樣 Spring 會自動掃描並註冊它們)
    // @Bean
    // public GameAIWebSocketHandler gameAIWebSocketHandler() {
    //     return new GameAIWebSocketHandler(/* 依賴注入（如果需要） */);
    // }

    // @Bean
    // public GameRoomWebSocketHandler gameRoomWebSocketHandler() {
    //     return new GameRoomWebSocketHandler(/* 依賴注入，例如 RoomManager */);
    // }
}
