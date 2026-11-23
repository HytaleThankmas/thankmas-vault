package dev.rm20.thankmasvault.npc.shop

import com.hypixel.hytale.server.core.universe.PlayerRef

object ShopItemRegistry {

    interface QuestChecker {
        /**
         * Checks if [questId] is currently active for [playerRef].
         */
        fun isQuestActive(playerRef: PlayerRef, questId: String): Boolean

        /**
         * Returns all active quest IDs for [playerRef].
         */
        fun getActiveQuestIds(playerRef: PlayerRef): Set<String>

        /**
         * Returns extra shop item IDs granted by active quests for [shopId].
         */
        fun getQuestShopItems(playerRef: PlayerRef, shopId: String): List<String> = emptyList()
    }

    private var questChecker: QuestChecker? = null

    @JvmStatic
    fun registerQuestChecker(checker: QuestChecker) {
        questChecker = checker
    }

    @JvmStatic
    fun isQuestActive(playerRef: PlayerRef, questId: String): Boolean {
        return questChecker?.isQuestActive(playerRef, questId) ?: false
    }

    @JvmStatic
    fun getActiveQuestIds(playerRef: PlayerRef): Set<String> {
        return questChecker?.getActiveQuestIds(playerRef) ?: emptySet()
    }

    @JvmStatic
    fun getQuestShopItems(playerRef: PlayerRef, shopId: String): List<String> {
        return questChecker?.getQuestShopItems(playerRef, shopId) ?: emptyList()
    }
}
