// com.example.demo.model.GameRoom.java
package com.example.demo.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class GameRoom {
    private final String id;
    private final List<WebSocketSession> players = new ArrayList<>(2);
    private final Map<String, String> playerNames = new HashMap<>(); // sessionId -> playerName
    private final Map<String, PlayerGameState> gameStates = new HashMap<>(); // sessionId -> PlayerGameState
    private String currentPlayerId = null;
    private boolean gameStarted = false;
    private boolean gameOver = false;
    private String winnerId = null;
    private final Random random = new Random();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Lock gameLock = new ReentrantLock();

    // 玩家遊戲狀態內部類
    public static class PlayerGameState {
        public String sessionId;
        public String playerName;
        public int maxHealth = 30; // 假設初始最大生命值為30
        public int health = maxHealth;
        public int mana = 10;
        public int maxMana = 10;
        public List<ServerCard> deck = new ArrayList<>();
        public List<ServerCard> hand = new ArrayList<>();
        public List<ServerCard> field = new ArrayList<>(); // 新增：玩家場上的牌

        // CardCreate.Create.cards 應該是 ServerCard[] 類型
        // private static final ServerCard[] CARD_TEMPLATES = CardCreate.Create.cards;
        // // 快取模板以提高效能

        public PlayerGameState(String sessionId, String playerName) {
            this.sessionId = sessionId;
            this.playerName = playerName;
            initializeDeck();
            for (int i = 0; i < 5; i++) { // 初始抽5張牌
                drawCard();
            }
        }

        private void initializeDeck() {
            // 清空牌組以防重複初始化
            deck.clear();
            if (CardCreate.Create != null && CardCreate.Create.cards != null) {
                List<ServerCard> allPossibleCards = new ArrayList<>(Arrays.asList(CardCreate.Create.cards));
                Collections.shuffle(allPossibleCards); // 先洗牌模板

                // 從洗牌後的模板中取牌，並為每張牌創建新實例，分配唯一ID
                for (int i = 0; i < 20 && i < allPossibleCards.size(); i++) { // 假設牌組20張
                    ServerCard template = allPossibleCards.get(i);
                    deck.add(new ServerCard(
                            UUID.randomUUID().toString(), // 遊戲中每張牌的唯一實例ID
                            template.getName(),
                            template.getCost(),
                            template.getAttack(),
                            template.getValue(),
                            template.getEffect(),
                            template.getCardType()));
                }
                // 此處deck不需要再次洗牌，因為是從已洗牌的模板順序取的
            } else {
                System.err.println(
                        "CardCreate.Create.cards is null or empty. Cannot initialize deck for player " + playerName);
            }
        }

        public ServerCard drawCard() {
            if (!deck.isEmpty()) {
                ServerCard drawnCard = deck.remove(0); // 從牌組頂部抽牌
                if (hand.size() < 10) { // 假設手牌上限10張
                    hand.add(drawnCard);
                } else {
                    System.out.println(playerName + " hand is full. Card " + drawnCard.getName() + " is burned.");
                    // 可以選擇是否將燒掉的牌通知客戶端或記錄
                }
                return drawnCard;
            }
            // TODO: 處理牌組抽乾的情況 (疲勞傷害)
            System.out.println(playerName + "'s deck is empty!");
            return null;
        }

        // 從手牌打出一張牌的邏輯
        // targetPlayerStateForEffect 通常是對手，但有些效果可能影響自己或全場
        public boolean playCardFromHand(String cardId, PlayerGameState targetPlayerStateForEffect) {
            ServerCard cardToPlay = hand.stream()
                    .filter(c -> Objects.equals(c.getId(), cardId))
                    .findFirst().orElse(null);

            if (cardToPlay != null && this.mana >= cardToPlay.getCost()) {
                this.mana -= cardToPlay.getCost();
                this.hand.remove(cardToPlay);

                System.out.println(this.playerName + " attempts to play " + cardToPlay.getName());

                // 根據卡牌類型處理
                if ("Minion".equalsIgnoreCase(cardToPlay.getCardType()) ||
                        "Weapon".equalsIgnoreCase(cardToPlay.getCardType()) || // 假設武器也上場
                        "Hero".equalsIgnoreCase(cardToPlay.getCardType())) { // 英雄牌也可能上場或替換英雄
                    this.field.add(cardToPlay); // 卡牌進入戰場
                    System.out.println(this.playerName + " played " + cardToPlay.getName() + " to their field.");
                    // TODO: 處理生物的戰吼效果等 (如果有的話，可以在此觸發或在 applyEffect 中根據卡牌類型細化)
                } else { // 預設為法術牌，直接應用效果
                    System.out.println(this.playerName + " cast spell " + cardToPlay.getName() + ".");
                    applyEffect(cardToPlay, this, targetPlayerStateForEffect);
                }
                return true;
            }
            System.out.println(this.playerName + " failed to play card " + cardId + ". Card found: "
                    + (cardToPlay != null) + ", Mana: " + this.mana + ", Cost: "
                    + (cardToPlay != null ? cardToPlay.getCost() : "N/A"));
            return false;
        }

        // 卡牌效果應用邏輯 (casterState 是出牌者)
        private void applyEffect(ServerCard card, PlayerGameState casterState, PlayerGameState targetState) {
            // 如果效果需要目標但目標為null (例如對手已斷線)，則效果可能無法正確執行
            if (targetState == null && "Deal".equalsIgnoreCase(card.getEffect())) {
                System.out.println("Effect target is null for Deal card " + card.getName() + ". Effect not applied.");
                return;
            }

            System.out.println("Applying effect of card: " + card.getName() + " cast by " + casterState.playerName);
            if ("Deal".equalsIgnoreCase(card.getEffect())) {
                int damage = card.getAttack();
                // Deal 效果通常對目標對手
                if (targetState != null) { // 確保目標存在
                    targetState.health -= damage;
                    targetState.health = Math.max(0, targetState.health);
                    System.out.println(casterState.playerName + "'s " + card.getName() + " deals " + damage
                            + " damage to " + targetState.playerName + ". " + targetState.playerName + " health: "
                            + targetState.health);
                } else {
                    System.out.println(card.getName() + " (Deal effect) had no target.");
                }
            } else if ("Heal".equalsIgnoreCase(card.getEffect())) {
                int healAmount = card.getValue();
                // Heal 效果通常對自己，除非卡牌設計是指定目標治療
                PlayerGameState actualTarget = casterState; // 預設治療自己
                if (targetState != null && card.getName().contains("對目標")) { // 假設卡牌名稱或效果描述能區分
                    actualTarget = targetState;
                }
                actualTarget.health += healAmount;
                actualTarget.health = Math.min(30, actualTarget.health); // 假設最大生命值30
                System.out.println(casterState.playerName + "'s " + card.getName() + " heals " + actualTarget.playerName
                        + " for " + healAmount + ". " + actualTarget.playerName + " health: " + actualTarget.health);
            }
            // TODO: 實現更多卡牌效果類型
        }

        // **關鍵修改：構建發送給客戶端的數據**
        public Map<String, Object> toMapForClient(boolean isThisClientSelf) {
            Map<String, Object> map = new HashMap<>();
            map.put("playerId", sessionId);
            map.put("playerName", playerName);
            map.put("maxHealth", maxHealth);
            map.put("health", health);
            map.put("mana", mana);
            map.put("maxMana", maxMana);
            map.put("deckSize", deck.size());
            map.put("handCount", hand.size()); // **始終發送手牌數量**

            if (isThisClientSelf) {
                // 如果是玩家自己，發送完整的手牌列表
                map.put("hand", hand.stream()
                        .map(c -> Map.of(
                                "id", c.getId(), "name", c.getName(), "cost", c.getCost(),
                                "attack", c.getAttack(), "value", c.getValue(), "effect", c.getEffect(),
                                "cardType", c.getCardType())) // 確保ServerCard有getCardType
                        .collect(Collectors.toList()));
            } else {
                // **對於對手，hand 欄位發送空列表 []**
                map.put("hand", new ArrayList<>());
            }

            // 場地牌對雙方都可見
            map.put("field", field.stream()
                    .map(c -> Map.of(
                            "id", c.getId(), "name", c.getName(), "cost", c.getCost(),
                            "attack", c.getAttack(), "value", c.getValue(), "effect", c.getEffect(),
                            "cardType", c.getCardType()))
                    .collect(Collectors.toList()));
            System.out.println("PlayerGameState.toMapForClient: " + map);
            return map;
        }
    }

    // GameRoom 類的構造函數和方法
    public GameRoom(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public boolean isFull() {
        return players.size() >= 2;
    } // 允許稍多於2以處理並發，但遊戲邏輯基於2人

    public boolean hasGameStarted() {
        return gameStarted;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public List<WebSocketSession> getPlayers() {
        gameLock.lock();
        try {
            return new ArrayList<>(players); // 返回副本
        } finally {
            gameLock.unlock();
        }
    }

    public Map<String, String> getPlayerNames() {
        gameLock.lock(); // 如果 playerNames 可能被並行修改，請確保執行緒安全
        try {
            return new HashMap<>(this.playerNames); // 返回副本以防止外部直接修改
        } finally {
            gameLock.unlock();
        }
    }

    public boolean addPlayer(WebSocketSession session, String playerName) {
        gameLock.lock();
        try {
            if (players.size() < 2) {
                if (players.stream().anyMatch(p -> p.getId().equals(session.getId()))) {
                    System.out.println("Player " + session.getId() + " (" + playerName + ") is already in room " + id);
                    // 如果玩家已在，可以考慮重新發送當前房間狀態給他
                    // broadcastRoomState("您已重新連接到房間。"); // 或者只發給該玩家
                    return true;
                }
                players.add(session);
                String nameToUse = (playerName == null || playerName.trim().isEmpty())
                        ? "玩家_" + session.getId().substring(0, 4)
                        : playerName;
                playerNames.put(session.getId(), nameToUse);
                gameStates.put(session.getId(), new PlayerGameState(session.getId(), nameToUse));
                System.out.println(nameToUse + " (ID: " + session.getId() + ") joined room " + id + ". Total players: "
                        + players.size());
                return true;
            }
            System.out.println(
                    "Room " + id + " is full. Cannot add player " + playerName + " (ID: " + session.getId() + ")");
            return false;
        } finally {
            gameLock.unlock();
        }
    }

    public void removePlayer(WebSocketSession session) {
        gameLock.lock();
        try {
            String leavingPlayerId = session.getId();
            boolean removed = players.removeIf(p -> p.getId().equals(leavingPlayerId));

            if (removed) {
                String leavingPlayerName = playerNames.remove(leavingPlayerId);
                gameStates.remove(leavingPlayerId);
                System.out.println((leavingPlayerName != null ? leavingPlayerName : leavingPlayerId) + " left room "
                        + id + ". Players remaining: " + players.size());

                if (gameStarted && !gameOver) {
                    gameOver = true; // 遊戲中有人離開，遊戲結束
                    if (!players.isEmpty()) {
                        winnerId = players.get(0).getId(); // 剩下的玩家獲勝
                        System.out.println("Room " + id + ": Player "
                                + (leavingPlayerName != null ? leavingPlayerName : leavingPlayerId)
                                + " disconnected. Winner: " + playerNames.get(winnerId));
                        broadcastRoomState((leavingPlayerName != null ? leavingPlayerName : "對方") + " 已離線，"
                                + playerNames.get(winnerId) + " 獲勝！");
                    } else {
                        System.out.println("Room " + id + ": All players left after game started. Game ended.");
                        // 此處可以通知 GameRoomManager 該房間可以被清理
                    }
                } else if (!gameStarted && players.isEmpty()) {
                    System.out.println("Room " + id + " is now empty and can be removed (before game start).");
                    // 通知 GameRoomManager 移除此房間
                } else if (!players.isEmpty()) { // 遊戲未開始，但仍有玩家，或遊戲已結束
                    broadcastRoomState((leavingPlayerName != null ? leavingPlayerName : "一位玩家") + " 已離開房間。");
                }
            }
        } finally {
            gameLock.unlock();
        }
    }

    public void startGameIfReady() {
        gameLock.lock();
        try {
            if (isFull() && !gameStarted) {
                gameStarted = true;
                // 隨機決定先手玩家
                currentPlayerId = players.get(random.nextInt(players.size())).getId();

                // 初始化雙方玩家的法力水晶 (PlayerGameState構造函數已處理牌組和初始手牌)
                for (PlayerGameState state : gameStates.values()) {
                    state.mana = 10; // 初始回合為1法力
                    state.maxMana = 10; // 初始最大法力為1
                }
                System.out.println("Room " + id + " game started. Player count: " + players.size() + ". First turn: "
                        + playerNames.get(currentPlayerId));
                String player1Name = playerNames.get(players.get(0).getId());
                String player2Name = playerNames.get(players.get(1).getId());
                broadcastRoomState(
                        player1Name + " vs " + player2Name + "\n遊戲開始！輪到 " + playerNames.get(currentPlayerId));
            } else if (!isFull() && !gameStarted) {
                System.out.println(
                        "Room " + id + ": Waiting for more players to start. Current players: " + players.size());
                broadcastRoomState("等待對手加入...");
            }
        } finally {
            gameLock.unlock();
        }
    }

    public boolean handlePlayCard(WebSocketSession session, String cardId) {
        gameLock.lock();
        try {
            if (!gameStarted || gameOver) {
                sendError(session, "遊戲未開始或已結束。");
                return false;
            }
            if (!Objects.equals(session.getId(), currentPlayerId)) {
                sendError(session, "現在不是您的回合。");
                return false;
            }

            PlayerGameState casterState = gameStates.get(session.getId());
            String opponentId = getOpponentSessionId(session.getId());
            PlayerGameState targetStateForEffect = (opponentId != null) ? gameStates.get(opponentId) : null;

            if (casterState == null) { // 幾乎不可能發生，除非玩家剛斷線
                sendError(session, "無法獲取您的遊戲狀態。");
                return false;
            }
            // 如果 targetStateForEffect 為 null 且卡牌效果需要目標，應在 PlayerGameState.applyEffect 中處理

            ServerCard cardPlayed = casterState.hand.stream()
                    .filter(c -> Objects.equals(c.getId(), cardId))
                    .findFirst().orElse(null); // 先找到卡牌對象

            if (cardPlayed == null) {
                sendError(session, "無效的卡牌ID。");
                return false;
            }
            boolean cardPlayedSuccessfully = casterState.playCardFromHand(cardId, targetStateForEffect);
            if (cardPlayedSuccessfully) {
                checkGameOver();

                // ---> 向出牌者發送確認訊息 <---
                GameMessage playConfirmMsg = new GameMessage();
                playConfirmMsg.setType("playerAction"); // 或者 "playCardSuccess"
                playConfirmMsg.setPlayerId(session.getId()); // 出牌者的ID
                Map<String, Object> actionData = new HashMap<>();
                actionData.put("action", "playCard");
                actionData.put("success", true);
                actionData.put("cardId", cardId); // 該卡牌的ID
                // actionData.put("cardName", actualCardObjectPlayed.getName()); // 可選
                playConfirmMsg.setData(actionData);
                try {
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(playConfirmMsg)));
                    System.out.println("Sent playCard confirmation to " + playerNames.get(session.getId()));
                } catch (IOException e) {
                    System.err.println(
                            "Error sending playCard confirmation to " + session.getId() + ": " + e.getMessage());
                }

                broadcastPlayCardAction(session, cardPlayed); // actualCardObjectPlayed 是 ServerCard
                broadcastRoomState(null);
                return true;
            } else {
                sendError(session, "出牌失敗 (法力不足或卡牌無效)。");
                return false;
            }
        } finally {
            gameLock.unlock();
        }
    }

    // 廣播出牌動作給對手
    private void broadcastPlayCardAction(WebSocketSession playerWhoPlayed, ServerCard card) {
        if (card == null) {
            System.err.println("broadcastPlayCardAction: card is null, cannot broadcast.");
            return;
        }

        GameMessage msg = new GameMessage();
        msg.setType("opponentPlayCard");
        msg.setPlayerId(playerWhoPlayed.getId());
        msg.setData(Map.of(
                "card", Map.of(
                        "id", card.getId(), "name", card.getName(), "cost", card.getCost(),
                        "attack", card.getAttack(), "value", card.getValue(), "effect", card.getEffect(),
                        "cardType", card.getCardType()),
                "playerName", playerNames.get(playerWhoPlayed.getId())));

        WebSocketSession opponent = getOpponentSession(playerWhoPlayed);
        if (opponent != null && opponent.isOpen()) {
            try {
                opponent.sendMessage(new TextMessage(objectMapper.writeValueAsString(msg)));
                System.out.println("Sent opponentPlayCard (" + card.getName() + ") to " + opponent.getId());
            } catch (IOException e) {
                System.err.println(
                        "Error sending opponentPlayCard action to " + opponent.getId() + ": " + e.getMessage());
            }
        }
    }

    public boolean handleEndTurn(WebSocketSession session) {
        gameLock.lock();
        try {
            if (!gameStarted || gameOver) {
                sendError(session, "遊戲未開始或已結束。");
                return false;
            }
            if (!Objects.equals(session.getId(), currentPlayerId)) {
                sendError(session, "現在不是您的回合。");
                return false;
            }

            String previousPlayerId = currentPlayerId;
            PlayerGameState previousPlayerState = gameStates.get(previousPlayerId);
            // TODO: 在回合結束時，可以處理場上生物的「回合結束」效果（如果有的話）

            currentPlayerId = getOpponentSessionId(session.getId());
            if (currentPlayerId == null) { // 理論上在雙人遊戲中，如果遊戲已開始，總能找到對手
                System.err.println("Room " + id + ": Opponent not found for player " + previousPlayerId
                        + " during end turn. This indicates an issue.");
                // 可以將其視為遊戲結束，當前玩家獲勝，因為對手「消失」了
                gameOver = true;
                winnerId = previousPlayerId;
                broadcastRoomState(
                        playerNames.get(previousPlayerId) + " 的對手已離開，" + playerNames.get(previousPlayerId) + " 獲勝！");
                return false;
            }

            PlayerGameState newCurrentPlayerState = gameStates.get(currentPlayerId);
            newCurrentPlayerState.maxMana = Math.min(10, newCurrentPlayerState.maxMana + 1);
            newCurrentPlayerState.mana += 5; // 新回合回滿法力
            newCurrentPlayerState.mana = Math.min(newCurrentPlayerState.mana, newCurrentPlayerState.maxMana); // 確保不超過最大法力
            newCurrentPlayerState.drawCard(); // 新回合抽牌
            // TODO: 在回合開始時，可以處理場上生物的「回合開始」效果

            System.out.println("Room " + id + ": Player " + playerNames.get(previousPlayerId)
                    + " ended turn. New turn for: " + playerNames.get(currentPlayerId));
            checkGameOver(); // 抽牌或回合開始效果可能導致遊戲結束
            broadcastRoomState(null); // 廣播更新後的遊戲狀態 (包含新的currentPlayerId)
            return true;
        } finally {
            gameLock.unlock();
        }
    }

    private void checkGameOver() {
        if (gameOver)
            return; // 如果已經結束，不再重複檢查

        String player1Id = null;
        String player2Id = null;
        if (players.size() == 2) {
            player1Id = players.get(0).getId();
            player2Id = players.get(1).getId();
        } else if (players.size() == 1 && gameStarted) { // 遊戲開始後只剩一人
            gameOver = true;
            winnerId = players.get(0).getId();
            System.out.println("Room " + id + " Game Over. Opponent left. Winner: " + playerNames.get(winnerId));
            return;
        } else { // 玩家不足或遊戲未開始
            return;
        }

        PlayerGameState player1State = gameStates.get(player1Id);
        PlayerGameState player2State = gameStates.get(player2Id);

        if (player1State == null || player2State == null) {
            System.err.println("Room " + id + ": Player state missing during checkGameOver. This is an anomaly.");
            // 可能需要處理這種異常情況，例如結束遊戲
            if (player1State == null && player2State != null) {
                gameOver = true;
                winnerId = player2Id;
            } else if (player2State == null && player1State != null) {
                gameOver = true;
                winnerId = player1Id;
            } else {
                /* 兩者都null，房間可能已在清理中 */ }
            return;
        }

        if (player1State.health <= 0) {
            gameOver = true;
            winnerId = player2Id;
            System.out.println("Room " + id + " Game Over. Player " + playerNames.get(player1Id)
                    + " health is 0. Winner: " + playerNames.get(winnerId));
        } else if (player2State.health <= 0) {
            gameOver = true;
            winnerId = player1Id;
            System.out.println("Room " + id + " Game Over. Player " + playerNames.get(player2Id)
                    + " health is 0. Winner: " + playerNames.get(winnerId));
        }
        // TODO: 處理牌庫抽乾等其他結束條件
    }

    private String getOpponentSessionId(String sessionId) {
        gameLock.lock(); // 確保 players 列表在迭代時不被修改
        try {
            for (WebSocketSession pSession : players) {
                if (!Objects.equals(pSession.getId(), sessionId)) {
                    return pSession.getId();
                }
            }
        } finally {
            gameLock.unlock();
        }
        return null;
    }

    // **確保 broadcastRoomState 正確構建 self 和 opponent 數據**
    public void broadcastRoomState(String contextMessage) {
        gameLock.lock();
        try {
            // 遍歷當前房間內所有有效的玩家session
            List<WebSocketSession> currentPlayersInRoom = new ArrayList<>(this.players); // 創建副本以避免迭代時修改

            for (WebSocketSession playerSession : currentPlayersInRoom) {
                if (playerSession.isOpen()) {
                    PlayerGameState ownPlayerState = gameStates.get(playerSession.getId());
                    String opponentId = getOpponentSessionId(playerSession.getId());
                    PlayerGameState opponentPlayerState = (opponentId != null) ? gameStates.get(opponentId) : null;

                    if (ownPlayerState == null) { // 如果當前玩家狀態為空（可能剛斷線但列表未更新），則跳過
                        System.err.println("Skipping broadcast to session " + playerSession.getId()
                                + " as their game state is null.");
                        continue;
                    }

                    Map<String, Object> dataForThisClient = new HashMap<>();
                    dataForThisClient.put("roomId", id);
                    dataForThisClient.put("players", new HashMap<>(playerNames)); // <sessionId, playerName>
                    dataForThisClient.put("gameStarted", gameStarted);
                    dataForThisClient.put("gameOver", gameOver);
                    if (gameOver)
                        dataForThisClient.put("winnerId", winnerId);
                    dataForThisClient.put("currentPlayerId", currentPlayerId);
                    // dataForThisClient.put("yourPlayerId", playerSession.getId()); //
                    // 客戶端可以通過比較self.playerId

                    dataForThisClient.put("self", ownPlayerState.toMapForClient(true));

                    if (opponentPlayerState != null) {
                        dataForThisClient.put("opponent", opponentPlayerState.toMapForClient(false));
                    } else if (isFull() || (gameStarted && players.size() < 2)) { // 遊戲應有兩人但對手數據缺失(可能斷線)
                        Map<String, Object> placeholderOpponent = new HashMap<>();
                        placeholderOpponent.put("playerId", "opponent_placeholder");
                        placeholderOpponent.put("playerName",
                                (players.size() < 2 && gameStarted) ? "對手已離開" : "等待對手...");
                        placeholderOpponent.put("health", 0);
                        placeholderOpponent.put("mana", 0);
                        placeholderOpponent.put("maxMana", 0);
                        placeholderOpponent.put("hand", new ArrayList<>());
                        placeholderOpponent.put("handCount", 0);
                        placeholderOpponent.put("deckSize", 0);
                        placeholderOpponent.put("field", new ArrayList<>());
                        dataForThisClient.put("opponent", placeholderOpponent);
                    }

                    GameMessage msg = new GameMessage();
                    msg.setType("roomUpdate");
                    if (contextMessage != null && !contextMessage.isEmpty()) {
                        msg.setMessage(contextMessage);
                    } else if (gameOver && winnerId != null && playerNames.containsKey(winnerId)) {
                        msg.setMessage(playerNames.get(winnerId) + " 獲勝！");
                    } else if (gameOver) {
                        msg.setMessage("遊戲結束！");
                    }

                    msg.setData(dataForThisClient);
                    try {
                        playerSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(msg)));
                    } catch (IOException e) {
                        System.err.println(
                                "Error broadcasting room state to " + playerSession.getId() + ": " + e.getMessage());
                        // 可以在此處處理發送失敗，例如將該玩家標記為可能斷線
                    }
                }
            }
            if (gameStarted) {
                System.out
                        .println(
                                "Room " + id + " state broadcasted. Current turn: "
                                        + (currentPlayerId != null && playerNames.containsKey(currentPlayerId)
                                                ? playerNames.get(currentPlayerId)
                                                : "N/A")
                                        +
                                        ", Game Over: " + gameOver
                                        + (gameOver
                                                ? ", Winner: " + (winnerId != null && playerNames.containsKey(winnerId)
                                                        ? playerNames.get(winnerId)
                                                        : "N/A")
                                                : ""));
            } else {
                System.out.println("Room " + id + " state broadcasted (game not started). Players in room: "
                        + playerNames.values());
            }
        } finally {
            gameLock.unlock();
        }
    }

    private WebSocketSession getOpponentSession(WebSocketSession session) {
        gameLock.lock();
        try {
            for (WebSocketSession p : players) {
                if (!Objects.equals(p.getId(), session.getId())) {
                    return p;
                }
            }
        } finally {
            gameLock.unlock();
        }
        return null;
    }

    private void sendError(WebSocketSession session, String errorMessage) {
        GameMessage msg = new GameMessage();
        msg.setType("error");
        msg.setMessage(errorMessage);
        try {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(msg)));
            }
        } catch (IOException e) {
            System.err.println("Error sending error message to " + session.getId() + ": " + e.getMessage());
        }
    }
}