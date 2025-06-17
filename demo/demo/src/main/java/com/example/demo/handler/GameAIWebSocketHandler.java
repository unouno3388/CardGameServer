// com.example.demo.handler.GameAIWebSocketHandler.java
package com.example.demo.handler;

import com.example.demo.model.ServerCard;
import com.example.demo.model.CardCreate;
import com.example.demo.model.GameMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
//import org.h2.tools.Server;
import org.springframework.lang.NonNull;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

// 伺服器端AI遊戲實例，管理單一玩家與AI的遊戲狀態
class ServerAIGameInstance {
    private String playerId; // 對應 WebSocketSession ID
    private int maxHealth = 30; // 玩家和AI的最大生命值
    public int playerHealth = maxHealth;
    public int aiHealth = 5;//maxHealth;
    public int playerMaxMana = 10;
    public int aiMaxMana = 10;
    public int playerMana = playerMaxMana;
    public int aiMana = aiMaxMana;
    public List<ServerCard> playerDeck = new ArrayList<>();
    public List<ServerCard> aiDeck = new ArrayList<>();
    public List<ServerCard> playerHand = new ArrayList<>();
    public List<ServerCard> aiHand = new ArrayList<>();
    // 【新增】AI的場地區域
    public List<ServerCard> aiField = new ArrayList<>(); 

    private Random random = new Random();
    public boolean isPlayerTurn = true;
    private boolean gameOver = false;
    private String winner = null;

    public ServerAIGameInstance(String playerId) {
        this.playerId = playerId;
        initializeDecks(); // 初始化牌組
        // 初始抽牌
        for (int i = 0; i < 5; i++) {
            drawCard(true, false); // 玩家抽牌，不立即發送狀態
            drawCard(false, false); // AI抽牌，不立即發送狀態
        }
        System.out.println("AI Game Instance for " + playerId + " created. Player hand: " + playerHand.size() + ", AI hand: " + aiHand.size());
    }

    private void initializeDecks() {
        // 簡易牌組生成邏輯，您可以根據您的 Card.cs 和 AIManager.cs 中的邏輯來擴展
        /* 
        String[] cardNames = { "火球術", "寒冰箭", "治療波", "暗影箭", "火焰斬", "冰霜護盾" };
        String[] effects = { "Deal", "Heal" }; // Deal: 造成傷害, Heal: 治療

        for (int i = 0; i < 20; i++) { // 每方牌組20張牌
            String name = cardNames[random.nextInt(cardNames.length)];
            String effectType = name.contains("治療") ? "Heal" : "Deal";
            // 【新增】根據卡牌名稱或效果簡單判斷類型
            String cardType = (name.contains("斬") || name.contains("盾")) ? "Minion" : "Spell"; // 簡化示例

            int cost = random.nextInt(5) + 1; // 1-5 費
            int power = random.nextInt(4) + 2; // 2-5 點傷害/治療

            playerDeck.add(new ServerCard(UUID.randomUUID().toString(), name, cost,
                    effectType.equals("Deal") ? power : 0, effectType.equals("Heal") ? power : 0, effectType, cardType));
            aiDeck.add(new ServerCard(UUID.randomUUID().toString(), name + " (AI)", cost,
                    effectType.equals("Deal") ? power : 0, effectType.equals("Heal") ? power : 0, effectType, cardType));
        }
        */
        playerDeck.clear();
        aiDeck.clear();
        int randomIndex;//= random.nextInt(20); // 隨機選擇一個索引
        for (int i = 0; i < 20; i++) 
        {
          randomIndex = random.nextInt(20);
          //從模板獲取卡牌數據
          ServerCard templateCard = CardCreate.Create.cards[randomIndex];   
          //為玩家創一個新卡牌實例
          ServerCard playerCardInstance = new ServerCard(templateCard); // 使用複製構造函數
          playerCardInstance.setId(UUID.randomUUID().toString());      // 給新實例分配唯一 ID
          playerDeck.add(playerCardInstance);

          //為AI創建另一個獨立的新卡牌實例
          ServerCard aiCardInstance = new ServerCard(templateCard);    // 再次使用複製構造函數
          aiCardInstance.setId(UUID.randomUUID().toString());          // 給這個新實例分配另一個唯一 ID
          aiDeck.add(aiCardInstance);
        }
        Collections.shuffle(playerDeck);
        Collections.shuffle(aiDeck);
    }

    public void drawCard(boolean isPlayer, boolean sendUpdate) {
        if (gameOver) return;
        List<ServerCard> deck = isPlayer ? playerDeck : aiDeck;
        List<ServerCard> hand = isPlayer ? playerHand : aiHand;
        if (!deck.isEmpty()) {
            ServerCard drawnCard = deck.remove(0); // 從牌組頂部抽牌
            hand.add(drawnCard);
            System.out.println((isPlayer ? "Player" : "AI") + " drew: " + drawnCard.getName()+ " (ID: " + drawnCard.getId() + ")");
        } else {
            // 牌組抽乾的處理，例如疲勞傷害
            System.out.println((isPlayer ? "Player" : "AI") + " deck is empty!");
            // 可以在此處實現疲勞邏輯
        }
        checkGameOver();
    }

    // 玩家出牌
    public boolean playerPlayCard(String cardId) {
        if (gameOver || !isPlayerTurn) return false;

        ServerCard cardToPlay = playerHand.stream().filter(c -> Objects.equals(c.getId(), cardId)).findFirst().orElse(null);

        if (cardToPlay != null && playerMana >= cardToPlay.getCost()) {
            playerMana -= cardToPlay.getCost();
            playerHand.remove(cardToPlay);
            applyEffect(cardToPlay, true); // true 表示玩家打出的牌
            System.out.println("Player played: " + cardToPlay.getName());
            checkGameOver();
            return true;
        }
        System.out.println("Player failed to play card " + cardId + ". Card found: " + (cardToPlay != null) + ", Mana: " + playerMana + ", Cost: " + (cardToPlay != null ? cardToPlay.getCost() : "N/A"));
        return false;
    }

    // AI 出牌邏輯
    public ServerCard aiPlayCard() {
        if (gameOver || isPlayerTurn) return null;

        List<ServerCard> playableCards = aiHand.stream()
                                             .filter(card -> aiMana >= card.getCost())
                                             .collect(Collectors.toList());
        if (!playableCards.isEmpty()) {
            // 簡單AI：隨機選擇一張可出的牌
            ServerCard cardToPlay = playableCards.get(random.nextInt(playableCards.size()));
            aiMana -= cardToPlay.getCost();
            aiHand.remove(cardToPlay);
            applyEffect(cardToPlay, false); // false 表示AI打出的牌

            // 【新增】如果AI打出的是單位牌 (Minion)，將其加入AI場地
            if ("Minion".equalsIgnoreCase(cardToPlay.getCardType())) {
                aiField.add(cardToPlay);
                System.out.println("AI added " + cardToPlay.getName() + " to its field.");
            }

            System.out.println("AI played: " + cardToPlay.getName());
            checkGameOver();
            return cardToPlay;
        }
        System.out.println("AI has no playable cards.");
        return null;
    }

    private void applyEffect(ServerCard card, boolean byPlayer) {
        if (gameOver) return;
        System.out.println("Applying effect of card: " + card.getName() + " by " + (byPlayer ? "Player" : "AI"));
        if ("Deal".equals(card.getEffect())) {
            int damage = card.getAttack();
            if (byPlayer) {
                aiHealth -= damage;
                System.out.println("Player's " + card.getName() + " deals " + damage + " damage to AI. AI health: " + aiHealth);
            } else {
                playerHealth -= damage;
                System.out.println("AI's " + card.getName() + " deals " + damage + " damage to Player. Player health: " + playerHealth);
            }
        } else if ("Heal".equals(card.getEffect())) {
            int healAmount = card.getValue();
            if (byPlayer) {
                playerHealth += healAmount;
                System.out.println("Player's " + card.getName() + " heals Player for " + healAmount + ". Player health: " + playerHealth);
            } else {
                aiHealth += healAmount;
                System.out.println("AI's " + card.getName() + " heals AI for " + healAmount + ". AI health: " + aiHealth);
            }
        }
        playerHealth = Math.max(0, Math.min(maxHealth, playerHealth)); // 生命值上限maxHealth，下限0
        aiHealth = Math.max(0, Math.min(maxHealth, aiHealth));
    }

    public void endTurn() {
        if (gameOver) return;
        if (isPlayerTurn) { // 玩家結束回合
            System.out.println("Player ends turn.");
            isPlayerTurn = false;
            playerMana += 5;
            playerMana = Math.min(playerMaxMana, playerMana);
            //aiMaxMana = Math.min(10, aiMaxMana + 1); // 法力水晶上限10
            //aiMana = aiMaxMana;
            drawCard(false, false); // AI抽牌
        } else { // AI結束回合
            System.out.println("AI ends turn.");
            isPlayerTurn = true;
            aiMana += 5;
            aiMana = Math.min(aiMaxMana, aiMana);
            //playerMaxMana = Math.min(10, playerMaxMana + 1);
            //playerMana = playerMaxMana;
            drawCard(true, false); // 玩家抽牌
        }
        checkGameOver();
    }
    
    public void checkGameOver() {
        if (gameOver) return; // 如果已經結束，不再檢查
        if (playerHealth <= 0) {
            gameOver = true;
            winner = "AI";
            System.out.println("Game Over. Winner: AI");
        } else if (aiHealth <= 0) {
            gameOver = true;
            winner = "Player";
            System.out.println("Game Over. Winner: Player");
        }
    }

    public boolean isGameOver() {
        return gameOver;
    }
    // 獲取遊戲狀態，供客戶端使用
    public Map<String, Object> getGameStateForPlayer() {
        Map<String, Object> state = new HashMap<>();
        state.put("playerId", playerId);
        state.put("maxHealth", maxHealth);
        state.put("playerHealth", playerHealth);
        state.put("aiHealth", aiHealth);
        state.put("playerMana", playerMana);
        state.put("playerMaxMana", playerMaxMana);
        // 出於遊戲公平性，通常不直接顯示AI的法力水晶和手牌，但可以顯示AI最大法力水晶和手牌數量
        state.put("aiMaxMana", aiMaxMana); // 讓玩家知道AI的法力潛力
        state.put("aiHandCount", aiHand.size()); // AI手牌數量
        // 玩家手牌需要詳細資訊
        state.put("playerHand", playerHand.stream()
            .map(c -> Map.of("id", c.getId(), "name", c.getName(), "cost", c.getCost(), "attack", c.getAttack(), "value", c.getValue(), "effect", c.getEffect()))
            .collect(Collectors.toList()));

        // 【新增】將AI場地上的牌也發送給客戶端
        state.put("aiField", aiField.stream()
            .map(c -> Map.of("id", c.getId(), "name", c.getName(), "cost", c.getCost(), "attack", c.getAttack(), "value", c.getValue(), "effect", c.getEffect(), "cardType", c.getCardType())) // 【新增】傳遞cardType
            .collect(Collectors.toList()));
            
        state.put("isPlayerTurn", isPlayerTurn);
        state.put("gameOver", gameOver);
        if (gameOver) {
            state.put("winner", winner);
        }
        return state;
    }
}

@Component
public class GameAIWebSocketHandler extends TextWebSocketHandler {
    private final ObjectMapper objectMapper = new ObjectMapper();
    // 使用 ConcurrentHashMap 來管理多個遊戲實例 (每個 session 一個)
    private final Map<String, ServerAIGameInstance> activeGames = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws IOException {
        String playerId = session.getId();
        System.out.println("AI 遊戲模式: 新連接 playerId=" + playerId);
        ServerAIGameInstance gameInstance = new ServerAIGameInstance(playerId);
        activeGames.put(playerId, gameInstance);

        GameMessage response = new GameMessage();
        response.setType("gameStart");
        response.setPlayerId(playerId);
        response.setData(gameInstance.getGameStateForPlayer());
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
        System.out.println("Sent gameStart to " + playerId);
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) throws Exception {
        String payload = message.getPayload();
        String playerId = session.getId();
        GameMessage gameMessage = objectMapper.readValue(payload, GameMessage.class);
        ServerAIGameInstance game = activeGames.get(playerId);

        System.out.println("AI Game msg from " + playerId + ": " + payload);

        if (game == null) {
            sendErrorMessage(session, "遊戲實例未找到。");
            return;
        }
        if (game.isGameOver()) {
             sendGameUpdate(session, game);
             return;
        }

        // GameMessage response = new GameMessage(); // 這個 response 在 switch 內部定義更合適
        // response.setPlayerId(playerId); // playerId 應該在每個要發送的訊息中單獨設置

        switch (gameMessage.getType()) {
            case "playCard":
                if (game.isPlayerTurn) {
                    boolean played = game.playerPlayCard(gameMessage.getCardId());
                    if (played) {
                        System.out.println("Player " + playerId + " played card " + gameMessage.getCardId());

                        // --- 【新增】發送 playerAction 確認訊息給客戶端 ---
                        GameMessage playerActionConfirmMsg = new GameMessage();
                        playerActionConfirmMsg.setType("playerAction");
                        playerActionConfirmMsg.setPlayerId(playerId); // 可選，客戶端通常知道自己的ID

                        Map<String, Object> actionData = new HashMap<>();
                        actionData.put("action", "playCard"); // 與前端約定的 action 類型
                        actionData.put("success", true);      // 表示成功
                        actionData.put("cardId", gameMessage.getCardId()); // 告訴前端是哪張牌成功了
                        // 您也可以選擇在這裡附加一些 minimale 的狀態變化，如果前端動畫需要立即知道
                        // 例如：actionData.put("newPlayerMana", game.playerMana);
                        // 但完整的狀態同步還是依賴 gameStateUpdate

                        playerActionConfirmMsg.setData(actionData);
                        try {
                            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(playerActionConfirmMsg)));
                            System.out.println("Sent playerAction confirmation for card: " + gameMessage.getCardId() + " to " + playerId);
                        } catch (IOException e) {
                            System.err.println("Error sending playerAction confirmation to " + playerId + ": " + e.getMessage());
                        }
                        // --- 【新增結束】 ---

                        sendGameUpdate(session, game); // 發送玩家出牌後的完整遊戲狀態
                        //if (!game.isGameOver()) {
                        //    game.endTurn(); // 玩家回合結束，輪到AI
                        //    sendGameUpdate(session, game); // 通知客戶端輪到AI（isPlayerTurn變為false）
                        //    performAiTurn(session, game); // AI執行操作
                        //}
                    } else {
                        // 出牌失敗的錯誤訊息已在 playerPlayCard 方法的 log 中打印，並由 sendErrorMessage 發送
                        sendErrorMessage(session, "出牌失敗 (法力不足或卡牌不存在)。");
                    }
                } else {
                    sendErrorMessage(session, "現在不是您的回合。");
                }
                break;
            case "endTurn":
                if (game.isPlayerTurn) {
                    System.out.println("Player " + playerId + " ended turn.");
                    game.endTurn(); // 玩家回合結束
                    sendGameUpdate(session, game); // 更新狀態，輪到AI
                     if (!game.isGameOver()) {
                        performAiTurn(session, game); // AI執行操作
                    }
                } else {
                    sendErrorMessage(session, "現在不是您的回合。");
                }
                break;
            default:
                sendErrorMessage(session, "未知的訊息類型: " + gameMessage.getType());
        }
    }

    // performAiTurn, sendGameUpdate, sendErrorMessage, afterConnectionClosed, handleTransportError 方法保持不變...
    private void performAiTurn(WebSocketSession session, ServerAIGameInstance game) throws IOException {
        if (game.isGameOver() || game.isPlayerTurn) return;

        System.out.println("AI " + session.getId() + " is thinking...");
        try {
            Thread.sleep(1000 + random.nextInt(1000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        ServerCard aiPlayedCard = game.aiPlayCard();
        GameMessage aiActionResponse = new GameMessage();
        aiActionResponse.setPlayerId(session.getId()); // 這裡的 playerId 是 session ID，與前端的 PlayerId 概念可能不同，但通常一致
        aiActionResponse.setType("aiAction");
        
        Map<String, Object> actionData = new HashMap<>(); // 注意這裡的 actionData 與上面 playerAction 的 actionData 是不同的實例
        if (aiPlayedCard != null) {
            System.out.println("AI " + session.getId() + " played card " + aiPlayedCard.getName());
            actionData.put("actionType", "playCard"); // 後端發送 aiAction 時用 "actionType"
            actionData.put("card", Map.of("id", aiPlayedCard.getId(), "name", aiPlayedCard.getName(), "cost", aiPlayedCard.getCost(), "attack", aiPlayedCard.getAttack(), "value", aiPlayedCard.getValue(), "effect", aiPlayedCard.getEffect()));
        } else {
            System.out.println("AI " + session.getId() + " chose to end turn (no card played).");
            actionData.put("actionType", "endTurn");
        }
        aiActionResponse.setData(actionData);
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(aiActionResponse)));

        if (!game.isGameOver()) {
             game.endTurn();
        }
        sendGameUpdate(session, game);
        System.out.println("AI turn for " + session.getId() + " ended. Player turn: " + game.isPlayerTurn);
    }
    
    private Random random = new Random();

    private void sendGameUpdate(WebSocketSession session, ServerAIGameInstance game) throws IOException {
        GameMessage updateResponse = new GameMessage();
        updateResponse.setPlayerId(session.getId()); // 同上，這是 session ID
        updateResponse.setType("gameStateUpdate");
        updateResponse.setData(game.getGameStateForPlayer());
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(updateResponse)));
        System.out.println("Sent gameStateUpdate to " + session.getId() + ". Player turn: " + game.isPlayerTurn + ", GameOver: " + game.isGameOver());
    }

    private void sendErrorMessage(WebSocketSession session, String message) throws IOException {
        GameMessage errorResponse = new GameMessage();
        errorResponse.setPlayerId(session.getId());
        errorResponse.setType("error");
        errorResponse.setMessage(message);
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(errorResponse)));
        System.out.println("Sent error to " + session.getId() + ": " + message);
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) {
        String playerId = session.getId();
        activeGames.remove(playerId);
        System.out.println("AI 遊戲模式: 連接關閉 playerId=" + playerId + ", 原因: " + status);
    }

    @Override
    public void handleTransportError(@NonNull WebSocketSession session, @NonNull Throwable exception) {
        System.err.println("AI 遊戲模式: 連線異常 playerId=" + session.getId() + ", 錯誤: " + exception.getMessage());
        activeGames.remove(session.getId());
    }
}