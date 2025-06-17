package com.example.demo.handler;

import com.example.demo.model.GameMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class GameWebSocketHandler extends TextWebSocketHandler {
    private final ObjectMapper mapper = new ObjectMapper();
    private final CopyOnWriteArrayList<WebSocketSession> sessions = new CopyOnWriteArrayList<>();

    // 連接建立時
    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) {
        sessions.add(session);
        System.out.println("🟢 新連接建立 | ID: " + session.getId());
        System.out.println("🌐 當前在線連線數: " + sessions.size());
    }

    // 接收訊息處理
    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) throws Exception {
        String rawMessage = message.getPayload();
        System.out.println("\n📥 收到原始訊息: " + rawMessage);

        try {
            GameMessage gameMessage = mapper.readValue(rawMessage, GameMessage.class);
            System.out.println("🔍 解析結果: type=" + gameMessage.getType() 
                             + " | cardId=" + gameMessage.getCardId());

            switch (gameMessage.getType()) {
                case "playCard":
                    handlePlayCard(gameMessage.getCardId(), session);
                    break;
                case "endTurn":
                    handleEndTurn(session);
                    break;
                default:
                    System.out.println("⚠️ 未知消息類型: " + gameMessage.getType());
                    session.sendMessage(new TextMessage("{\"error\":\"Unsupported message type\"}"));
            }
        } catch (Exception e) {
            System.err.println("❌ JSON解析失敗: " + e.getMessage());
            System.err.println("⚠️ 原始訊息內容: " + rawMessage);
            session.sendMessage(new TextMessage("{\"error\":\"Invalid message format\"}"));
        }
    }

    // 處理出牌邏輯
    private void handlePlayCard(String cardId, WebSocketSession senderSession) throws IOException {
        System.out.println("🃏 處理出牌 | cardId: " + cardId);
        
        GameMessage response = new GameMessage();
        response.setType("playCard");
        response.setCardId(cardId);
        
        broadcastMessage(mapper.writeValueAsString(response), senderSession);
    }

    // 處理回合結束
    private void handleEndTurn(WebSocketSession senderSession) throws IOException {
        System.out.println("⏹️ 處理回合結束");
        
        GameMessage response = new GameMessage();
        response.setType("endTurn");
        
        broadcastMessage(mapper.writeValueAsString(response), null); // 廣播給所有人
    }

    // 廣播訊息方法
    private void broadcastMessage(String message, WebSocketSession excludeSession) {
        System.out.println("📤 廣播訊息: " + message);
        
        sessions.forEach(session -> {
            try {
                if (session.isOpen() && !session.equals(excludeSession)) {
                    session.sendMessage(new TextMessage(message));
                    System.out.println("   ➡️ 已發送給: " + session.getId());
                }
            } catch (IOException e) {
                System.err.println("❌ 訊息發送失敗: " + session.getId());
                e.printStackTrace();
            }
        });
    }

    // 連接關閉時
    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) {
        sessions.remove(session);
        System.out.println("🔴 連接關閉 | ID: " + session.getId());
        System.out.println("📉 當前在線連線數: " + sessions.size());
    }

    // 錯誤處理
    @Override
    public void handleTransportError(@NonNull WebSocketSession session, @NonNull Throwable exception) {
        System.err.println("‼️ 連線異常: " + session.getId());
        exception.printStackTrace();
    }
}
