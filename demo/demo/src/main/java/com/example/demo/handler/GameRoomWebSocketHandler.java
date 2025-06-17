// com.example.demo.handler.GameRoomWebSocketHandler.java
package com.example.demo.handler;

import com.example.demo.manager.GameRoomManager;
import com.example.demo.model.GameMessage;
import com.example.demo.model.GameRoom;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.lang.NonNull;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GameRoomWebSocketHandler extends TextWebSocketHandler {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final GameRoomManager roomManager;
    private final Map<String, String> sessionToRoomIdMap = new ConcurrentHashMap<>();
    // private final Map<String, String> playerNames = new ConcurrentHashMap<>(); // 這個 playerNames 應該由 GameRoom 內部管理

    public GameRoomWebSocketHandler(GameRoomManager roomManager) {
        this.roomManager = roomManager;
    }

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) {
        System.out.println("房間模式: 新連接 playerId=" + session.getId());
        // 連接時不立即做太多事，等待客戶端發送 createRoom 或 joinRoom
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) throws Exception {
        String payload = message.getPayload();
        GameMessage gameMessage = objectMapper.readValue(payload, GameMessage.class);
        String playerId = session.getId(); // WebSocket Session ID
        // 客戶端發送的 playerId 欄位現在用於承載其顯示名稱
        String playerNameFromClient = gameMessage.getPlayerId();


        System.out.println("Room msg from " + playerId + " (PlayerName: " + playerNameFromClient + "): " + payload);

        String currentRoomId = sessionToRoomIdMap.get(playerId);
        GameRoom currentRoom = (currentRoomId != null) ? roomManager.getRoom(currentRoomId) : null;

        switch (gameMessage.getType()) {
            case "createRoom":
                // 如果玩家已在某房間，先處理離開舊房間的邏輯（可選）
                if (currentRoom != null) {
                    System.out.println("Player " + playerId + " is already in room " + currentRoomId + ", leaving before creating new one.");
                    currentRoom.removePlayer(session);
                    sessionToRoomIdMap.remove(playerId);
                }
                GameRoom newRoom = roomManager.createRoom(session, playerNameFromClient);
                if (newRoom != null) {
                    sessionToRoomIdMap.put(playerId, newRoom.getId());
                    // 回傳房間ID給創建者
                    GameMessage roomCreatedMsg = new GameMessage();
                    roomCreatedMsg.setType("roomCreated");
                    roomCreatedMsg.setRoomId(newRoom.getId());
                    roomCreatedMsg.setPlayerId(playerId); // 這裡的playerId是session.getId()
                    // 附加玩家在房間內的名稱
                    roomCreatedMsg.setData(Map.of("message", "房間創建成功！房間號：" + newRoom.getId(),
                                                 "playerName", newRoom.getPlayerNames().get(playerId)));
                    System.out.println(roomCreatedMsg.toString());
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(roomCreatedMsg)));
                    newRoom.startGameIfReady(); // 如果是單人測試或已有等待者，可能會開始；否則僅廣播等待
                } else {
                    sendError(session, "創建房間失敗。");
                }
                break;

            case "joinRoom":
                String roomIdToJoin = gameMessage.getRoomId();
                if (currentRoom != null && !currentRoom.getId().equals(roomIdToJoin)) {
                     System.out.println("Player " + playerId + " is in room " + currentRoomId + ", leaving before joining " + roomIdToJoin);
                     currentRoom.removePlayer(session);
                     sessionToRoomIdMap.remove(playerId);
                } else if (currentRoom != null && currentRoom.getId().equals(roomIdToJoin)) {
                    System.out.println("Player " + playerId + " is already in room " + roomIdToJoin + ".");
                    // 可以選擇重新發送房間狀態或提示已在房間內
                    currentRoom.broadcastRoomState("您已在房間 " + roomIdToJoin + " 中。");
                    break;
                }

                GameRoom joinedRoom = roomManager.joinRoom(roomIdToJoin, session, playerNameFromClient);
                if (joinedRoom != null) {
                    sessionToRoomIdMap.put(playerId, roomIdToJoin);
                    GameMessage roomJoinedMsg = new GameMessage();
                    roomJoinedMsg.setType("roomJoined");
                    roomJoinedMsg.setRoomId(roomIdToJoin);
                    roomJoinedMsg.setPlayerId(playerId); // session.getId()
                    roomJoinedMsg.setData(Map.of("message", "成功加入房間！",
                                                 "playerName", joinedRoom.getPlayerNames().get(playerId)));
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(roomJoinedMsg)));
                    // joinedRoom.startGameIfReady() 已經在 RoomManager.joinRoom 內部被調用，會觸發狀態廣播
                } else {
                    sendError(session, "加入房間失敗 (房間不存在、已滿或ID錯誤)。");
                }
                break;

            case "playCard":
                if (currentRoom != null) {
                    currentRoom.handlePlayCard(session, gameMessage.getCardId());
                } else {
                    sendError(session, "您不在任何房間內或房間不存在，無法出牌。");
                }
                break;

            case "endTurn":
                if (currentRoom != null) {
                    currentRoom.handleEndTurn(session);
                } else {
                    sendError(session, "您不在任何房間內或房間不存在，無法結束回合。");
                }
                break;

            case "leaveRoom":
                if (currentRoom != null) { // 使用從 sessionToRoomIdMap 獲取的 currentRoom
                    String nameOfLeavingPlayer = currentRoom.getPlayerNames().get(playerId);
                    currentRoom.removePlayer(session); // GameRoom.removePlayer 內部會處理後續廣播和清理
                    
                    GameMessage leftRoomMsg = new GameMessage();
                    leftRoomMsg.setType("leftRoom"); // 告知客戶端操作成功
                    leftRoomMsg.setMessage("您已離開房間 " + currentRoomId);
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(leftRoomMsg)));

                    // 從映射中移除
                    sessionToRoomIdMap.remove(playerId);

                    // 如果房間因此變空，GameRoomManager應處理其移除
                    if (currentRoom.getPlayers().isEmpty()) {
                        roomManager.removeEmptyRoom(currentRoomId); // 假設GameRoomManager有此方法
                        System.out.println("Room " + currentRoomId + " is empty and has been signaled for removal by leaveRoom handler.");
                    } else {
                        // GameRoom.removePlayer 內部應已廣播玩家離開的消息給剩餘玩家
                    }
                } else {
                     sendError(session, "您當前不在任何房間內。");
                }
                break;

            default:
                sendError(session, "未知的房間訊息類型: " + gameMessage.getType());
        }
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) {
        String playerId = session.getId();
        String roomId = sessionToRoomIdMap.remove(playerId);

        if (roomId != null) {
            GameRoom room = roomManager.getRoom(roomId);
            if (room != null) {
                room.removePlayer(session); // 通知房間該玩家已離開
                if (room.getPlayers().isEmpty()) {
                     roomManager.removeEmptyRoom(roomId); // 假設方法
                     System.out.println("Room " + roomId + " became empty after player " + playerId + " disconnected and was removed.");
                }
            }
        }
        System.out.println("房間模式: 連接關閉 playerId=" + playerId + ", 原房間ID=" + (roomId != null ? roomId : "N/A") + ", 原因: " + status);
    }

    @Override
    public void handleTransportError(@NonNull WebSocketSession session, @NonNull Throwable exception) {
        System.err.println("房間模式: 連線異常 playerId=" + session.getId() + ", 錯誤: " + exception.getMessage());
        // 通常在傳輸錯誤後，連接也會關閉，afterConnectionClosed 會被調用
        // 可以選擇在這裡也執行類似 afterConnectionClosed 的清理邏輯，以防萬一
        afterConnectionClosed(session, CloseStatus.PROTOCOL_ERROR); // 模擬協議錯誤導致的關閉
    }
    
    private void sendError(WebSocketSession session, String errorMessage) {
        // ... (保持不變) ...
        GameMessage msg = new GameMessage();
        msg.setType("error");
        msg.setMessage(errorMessage);
        try {
            if (session.isOpen()){
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(msg)));
            }
        } catch (IOException e) {
            System.err.println("Error sending error message to " + session.getId() + ": " + e.getMessage());
        }
    }
    // GameRoomWebSocketHandler 內部的 Map<String, String> playerNames 應該移除，
    // 玩家名稱由 GameRoom.playerNames 管理
}