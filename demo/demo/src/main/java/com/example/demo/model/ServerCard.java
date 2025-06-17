// com.example.demo.model.ServerCard.java
package com.example.demo.model;

import java.util.Objects;

public class ServerCard {
    private String id; // 卡牌的唯一標識符
    private String name;
    private int cost;
    private int attack; // 攻擊力 (用於造成傷害的卡牌)
    private int value;  // 數值 (例如用於治療的卡牌的治療量)
    private String effect; // 效果描述，例如 "Deal" (造成傷害), "Heal" (治療)
    // 可以根據需要添加更多屬性，例如卡牌描述、圖片URL等
    private String cardType;
    // 建構函數
    public ServerCard(String id, String name, int cost, int attack, int value, String effect, String cardType) {
        this.id = id;
        this.name = name;
        this.cost = cost;
        this.attack = attack;
        this.value = value;
        this.effect = effect;
        this.cardType = cardType; // 【新增】
    }
    public ServerCard(ServerCard other) {
        // 不複製 ID，因為新實例需生成一個新的唯一 ID
        this.name = other.name;
        this.cost = other.cost;
        this.attack = other.attack;
        this.value = other.value;
        this.effect = other.effect;
        this.cardType = other.cardType;
    }
    // 空的建構函數，Jackson 反序列化時可能需要
    public ServerCard() {}

    // Getter 和 Setter
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getCost() { return cost; }
    public void setCost(int cost) { this.cost = cost; }
    public int getAttack() { return attack; }
    public void setAttack(int attack) { this.attack = attack; }
    public int getValue() { return value; }
    public void setValue(int value) { this.value = value; }
    public String getEffect() { return effect; }
    public void setEffect(String effect) { this.effect = effect; }
    public String getCardType() { return cardType; } // 【新增】
    public void setCardType(String cardType) { this.cardType = cardType; } // 【新增】

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServerCard that = (ServerCard) o;
        return Objects.equals(id, that.id); // 假設ID是唯一的
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ServerCard{" +
               "id='" + id + '\'' +
               ", name='" + name + '\'' +
               ", cost=" + cost +
               ", attack=" + attack +
               ", value=" + value +
               ", effect='" + effect + '\'' +
               ", card type='" + cardType + '\'' +
               '}';
    }
}
