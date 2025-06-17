// com.example.demo.manager.GameRoomManager.java
package com.example.demo.manager;

import com.example.demo.model.GameRoom;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component // 讓 Spring 管理這個 Bean
public class GameRoomManager {
    private final Map<String, GameRoom> activeRooms = new ConcurrentHashMap<>();

    public GameRoom createRoom(WebSocketSession player1, String playerName) {
        String roomId = UUID.randomUUID().toString().substring(0, 6); // 生成一個短ID作為房間號
        GameRoom room = new GameRoom(roomId);
        room.addPlayer(player1, playerName);
        activeRooms.put(roomId, room);
        System.out.println("Room created: " + roomId + " by player " + player1.getId());
        return room;
    }

    public GameRoom joinRoom(String roomId, WebSocketSession player2, String playerName) {
        GameRoom room = activeRooms.get(roomId);
        if (room != null && !room.isFull()) {
            if (room.addPlayer(player2, playerName)) {
                 System.out.println("Player " + player2.getId() + " joined room " + roomId);
                room.startGameIfReady(); // 如果人滿了就開始遊戲
                return room;
            } else {
                // 可能是并发导致加入失败，理论上被room.isFull()挡住
                return null;
            }
        }
        System.out.println("Player " + player2.getId() + " failed to join room " + roomId + ". Room found: " + (room != null) + (room != null ? ", isFull: " + room.isFull() : ""));
        return null; // 房間不存在或已滿
    }

    public GameRoom getRoom(String roomId) {
        return activeRooms.get(roomId);
    }

    public void removePlayerFromRoom(WebSocketSession session) {
        // 遍歷所有房間，找到該玩家並移除
        // 為了效率，更好的做法是讓 WebSocketSession 自身也知道它所在的 roomId
        // 此處為簡化實現，採用遍歷
        activeRooms.values().forEach(room -> {
            boolean playerWasInRoom = room.getPlayers().stream().anyMatch(p -> p.getId().equals(session.getId()));
            if (playerWasInRoom) {
                room.removePlayer(session);
                if (room.getPlayers().isEmpty()) { // 如果房間空了，可以考慮移除房間
                    activeRooms.remove(room.getId());
                    System.out.println("Room " + room.getId() + " is empty and removed.");
                }
            }
        });
    }
    public void removeEmptyRoom(String roomId) {
        GameRoom room = activeRooms.get(roomId);
        if (room != null && room.getPlayers().isEmpty()) {
            activeRooms.remove(roomId);
            System.out.println("GameRoomManager: Room " + roomId + " was empty and has been removed.");
        }
    }
    // 可以添加一個方法來獲取所有活躍房間的列表 (例如，用於大廳顯示)
    public Map<String, GameRoom> getActiveRooms() {
        return activeRooms;
    }
}
