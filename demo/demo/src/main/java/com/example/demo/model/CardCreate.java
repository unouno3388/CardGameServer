package com.example.demo.model;

import java.util.UUID;

public class CardCreate 
{
    private CardCreate() {} // Private constructor to prevent instantiation
    public static CardCreate Create = new CardCreate(); // Singleton instance
    //String[] effects = { "Deal", "Heal" };
    enum EffectType { Deal, Heal };
    enum CardType { Spell, Minion, Weapon, Hero };
    public ServerCard[] cards = new ServerCard[]
    {
        new ServerCard
            (   UUID.randomUUID().toString(),
                "一顆普通的石頭", // An Ordinary Stone
                3, // Cost
                1, // Attack/Effect Value
                0, // Defense/Secondary Value
                EffectType.Deal.toString(), // Effect Type
                CardType.Spell.toString() // Card Type
            ),
        new ServerCard
            (   UUID.randomUUID().toString(),
                "大岩壁", // Great Rock Wall
                4,
                7, // Interpreted as defense/health gain if it were a buff or minion
                0, // For a spell, this could be damage too, or a secondary effect value. Let's stick to 7 damage.
                EffectType.Deal.toString(), // Assuming it's a spell that deals damage by throwing a wall
                CardType.Spell.toString()
            ),
        new ServerCard
            (   UUID.randomUUID().toString(),
                "劇毒箭矢", // Poisonous Arrow
                2,
                3, // Damage
                0, // Could be poison duration/damage later
                EffectType.Deal.toString(),
                CardType.Spell.toString()
            ),
        new ServerCard
            (   UUID.randomUUID().toString(),
                "重修舊好", // Make Up / Reconciliation
                4,
                5, // Healing value
                0,
                EffectType.Heal.toString(), // Changed to Heal based on name
                CardType.Spell.toString()
            ),
        new ServerCard
            (   UUID.randomUUID().toString(),
                "烈焰衝擊", // Flame Impact
                3,
                2, // Damage
                0,
                EffectType.Deal.toString(),
                CardType.Spell.toString()
            ),
        new ServerCard
            (   UUID.randomUUID().toString(),
                "愛情釀的酒", // Love's Brewed Wine
                4,
                0, // No direct attack/damage
                4, // Healing value
                EffectType.Heal.toString(),
                CardType.Spell.toString()
            ),
        new ServerCard
            (   UUID.randomUUID().toString(),
                "孤毒", // Solitary Poison
                2,
                4, // Damage
                0,
                EffectType.Deal.toString(),
                CardType.Spell.toString()
            ),
        // New Cards Start Here (Total 30)
        new ServerCard
            (   UUID.randomUUID().toString(),
                "智慧啟迪", // Wisdom's Enlightenment
                2,
                0, // No direct damage/heal
                2, // Cards to draw
                EffectType.Heal.toString(), // New EffectType
                CardType.Spell.toString()
            ),
        new ServerCard
            (   UUID.randomUUID().toString(),
                "神聖祝福", // Holy Blessing
                3,
                0,
                6, // Heal amount
                EffectType.Heal.toString(),
                CardType.Spell.toString()
            ),
        new ServerCard
            (   UUID.randomUUID().toString(),
                "暗影箭", // Shadow Bolt
                1,
                2, // Damage
                0,
                EffectType.Deal.toString(),
                CardType.Spell.toString()
            ),
        new ServerCard
            (   UUID.randomUUID().toString(),
                "冰霜新星", // Frost Nova
                5,
                3, // Area Damage
                0, // Could be freeze duration
                EffectType.Deal.toString(), // New EffectType (deals damage to multiple targets)
                CardType.Spell.toString()
            ),
        new ServerCard
            (   UUID.randomUUID().toString(),
                "力量祝福", // Blessing of Strength
                2,
                2, // Attack buff amount
                0, // Could be duration or health buff
                EffectType.Deal.toString(), // New EffectType
                CardType.Spell.toString()
            ),
        new ServerCard
            (   UUID.randomUUID().toString(),
                "石膚術", // Stoneskin
                3,
                0,
                5, // Defense buff amount / Shield
                EffectType.Heal.toString(), // New EffectType or Shield
                CardType.Spell.toString()
            ),
        new ServerCard
            (   UUID.randomUUID().toString(),
                "能量汲取", // Energy Siphon
                4,
                0, // Damage dealt
                3, // Health leeched
                EffectType.Heal.toString(), // New EffectType (deal damage and heal for amount)
                CardType.Spell.toString()
            ),
        new ServerCard
            (   UUID.randomUUID().toString(),
                "快速治療", // Quick Heal
                1,
                0,
                2, // Heal amount
                EffectType.Heal.toString(),
                CardType.Spell.toString()
            ),
        new ServerCard
            (   UUID.randomUUID().toString(),
                "魔力湧泉", // Mana Spring
                1,
                0,
                3, // Mana crystals to gain (temporary or permanent)
                EffectType.Heal.toString(), // New EffectType
                CardType.Spell.toString()
            ),
        new ServerCard
            (   UUID.randomUUID().toString(),
                "心靈震爆", // Mind Blast
                5,
                5, // Direct damage (e.g., to enemy hero)
                0,
                EffectType.Deal.toString(), // Could be a specific "DealDirect"
                CardType.Spell.toString()
            ),
        new ServerCard
            (   UUID.randomUUID().toString(),
                "遺忘之風", // Wind of Oblivion
                3,
                0,
                1, // Cards for opponent to discard
                EffectType.Heal.toString(), // New EffectType
                CardType.Spell.toString()
            ),
        new ServerCard
            (   UUID.randomUUID().toString(),
                "閃電鏈", // Chain Lightning
                4,
                2, // Damage, hits multiple targets (e.g., 3)
                0, // Number of targets implied by AreaDamage effect
                EffectType.Deal.toString(),
                CardType.Spell.toString()
            ),
        new ServerCard
            (   UUID.randomUUID().toString(),
                "虛弱詛咒", // Curse of Weakness
                2,
                2, // Attack reduction on an enemy
                0, // Duration or target count
                EffectType.Deal.toString(), // New EffectType (reduce enemy stats)
                CardType.Spell.toString()
            ),
        new ServerCard
            (   UUID.randomUUID().toString(),
                "洞察", // Insight
                1,
                0,
                1, // Cards to draw
                EffectType.Heal.toString(),
                CardType.Spell.toString()
            ),
        new ServerCard
            (   UUID.randomUUID().toString(),
                "再生", // Regeneration
                5,
                0,
                8, // Heal amount over time, or large single heal
                EffectType.Heal.toString(),
                CardType.Spell.toString()
            ),
        new ServerCard
            (   UUID.randomUUID().toString(),
                "火球術", // Fireball
                4,
                6, // Damage
                0,
                EffectType.Deal.toString(),
                CardType.Spell.toString()
            ),
        new ServerCard
            (   UUID.randomUUID().toString(),
                "護盾術", // Shield Spell
                2,
                0,
                4, // Shield amount (temporary health)
                EffectType.Heal.toString(), // New EffectType
                CardType.Spell.toString()
            ),
        new ServerCard
            (   UUID.randomUUID().toString(),
                "群體狂暴", // Mass Frenzy
                6,
                2, // Attack buff to all allied units
                0, // Could be a small health buff too
                EffectType.Deal.toString(), // Needs logic to apply to multiple
                CardType.Spell.toString()
            ),
        new ServerCard
            (   UUID.randomUUID().toString(),
                "復甦之光", // Light of Revival
                7,
                0,
                1, // Number of minions to revive (if you implement minions and graveyard)
                EffectType.Heal.toString(), // New EffectType
                CardType.Spell.toString()
            ),
            /* 
        new ServerCard
            (   UUID.randomUUID().toString(),
                "沉默敕令", // Edict of Silence
                3,
                0, // No direct value change
                0, // Parameter might indicate target type or duration
                EffectType.Silence.toString(), // New EffectType (removes abilities from a target)
                CardType.Spell.toString()
            ),*/
        new ServerCard
            (   UUID.randomUUID().toString(),
                "最終爆發", // Final Outburst
                8,
                10, // High damage
                0,
                EffectType.Deal.toString(),
                CardType.Spell.toString()
            ),
        new ServerCard
            (   UUID.randomUUID().toString(),
                "靈魂交換", // Soul Swap (Potentially complex: swap stats or a friendly/enemy minion)
                5,
                0, // Parameter 1 might be target 1
                0, // Parameter 2 might be target 2
                EffectType.Deal.toString(), // Placeholder; this needs a very custom EffectType like "SwapStats" or "ControlMinion"
                CardType.Spell.toString()
            ),
            /* 
        new ServerCard
            (   UUID.randomUUID().toString(),
                "時間扭曲", // Time Warp (Extremely powerful: extra turn. For simplicity, let's make it massive card draw)
                9,
                0,
                5, // Draw 5 cards
                EffectType.Draw.toString(),
                CardType.Spell.toString()
            )*/
    };
    
    
}
