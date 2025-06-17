// com.example.demo.model.GameMessage.java
package com.example.demo.model;

import java.util.Map;

public class GameMessage {
    private String type;     // 訊息類型，例如："playCard", "endTurn", "createRoom", "joinRoom", "roomUpdate", "gameState", "error", "gameStart", "aiAction"
    private String cardId;   // 卡牌ID (用於出牌)
    private String roomId;   // 房間ID (用於房間相關操作)
    private String playerId; // 玩家ID (可以是 session ID 或自訂ID)
    private String message;  // 用於錯誤訊息或一般文字訊息
    private Map<String, Object> data; // 彈性負載，用於傳輸複雜數據，如遊戲狀態、房間詳情、卡牌數據等

    // Getter 和 Setter 方法
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getCardId() { return cardId; }
    public void setCardId(String cardId) { this.cardId = cardId; }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }

    public String getPlayerId() { return playerId; }
    public void setPlayerId(String playerId) { this.playerId = playerId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Map<String, Object> getData() { return data; }
    public void setData(Map<String, Object> data) { this.data = data; }

    @Override
    public String toString() {
        return "GameMessage{" +
               "type='" + type + '\'' +
               ", cardId='" + cardId + '\'' +
               ", roomId='" + roomId + '\'' +
               ", playerId='" + playerId + '\'' +
               ", message='" + message + '\'' +
               ", data=" + (data != null ? data.toString() : "null") +
               '}';
    }
}