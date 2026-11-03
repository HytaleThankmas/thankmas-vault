package dev.rm20.thankmasvault.fishing

import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.entity.ItemUtils
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.inventory.InventoryComponent
import com.hypixel.hytale.server.core.inventory.ItemStack
import com.hypixel.hytale.server.core.inventory.container.ItemContainer
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import dev.rm20.anglersalmanac.Models.MinigameRodStats
import dev.rm20.thankmasvault.assets.RodProgressionAsset
import dev.rm20.thankmasvault.assets.RodTierInfo

object FishingRodService {

    const val DEFAULT_STARTING_ROD = "AA_Rod_Crude"

    private val FALLBACK_ASSET: RodProgressionAsset by lazy {
        RodProgressionAsset().apply {
            title = "Rod Upgrades"
            tiers = arrayOf(
                RodTierInfo("AA_Rod_Crude", 0, "Crude Fishing Rod", "A basic fishing rod made of crude wood and twine."),
                RodTierInfo("AA_Rod_Iron", 100, "Iron Fishing Rod", "Reinforced with iron fittings for improved control and durability.")
            )
        }
    }

    fun getActiveProgressionAsset(): RodProgressionAsset {
        return RodProgressionAsset.getDefault() ?: FALLBACK_ASSET
    }

    fun getProgression(playerEntityRef: Ref<EntityStore>, store: Store<EntityStore>): PlayerRodProgressionComponent {
        var comp = store.getComponent(playerEntityRef, PlayerRodProgressionComponent.componentType)
        if (comp == null) {
            comp = PlayerRodProgressionComponent()
            store.externalData.world.execute {
                store.addComponent(playerEntityRef, PlayerRodProgressionComponent.componentType, comp)
            }
        }
        if (!comp.isUnlocked(DEFAULT_STARTING_ROD)) {
            comp.unlockRod(DEFAULT_STARTING_ROD)
        }
        return comp
    }

    fun getHighestUnlockedRod(
        progression: PlayerRodProgressionComponent,
        asset: RodProgressionAsset = getActiveProgressionAsset()
    ): RodTierInfo {
        val tiers = asset.tiers
        if (tiers.isEmpty()) {
            return FALLBACK_ASSET.tiers.first()
        }
        for (i in tiers.indices.reversed()) {
            val tier = tiers[i]
            if (progression.isUnlocked(tier.itemId)) {
                return tier
            }
        }
        return tiers.first()
    }

    fun isFishingRod(itemId: String?, asset: RodProgressionAsset = getActiveProgressionAsset()): Boolean {
        if (itemId == null) return false
        if (itemId.startsWith("AA_Rod_")) return true
        return asset.tiers.any { it.itemId == itemId }
    }

    data class RodSyncResult(
        val rodCountBefore: Int,
        val removedRodsCount: Int,
        val gaveRod: Boolean,
        val targetRodId: String,
        val targetRodName: String
    )

    fun syncPlayerRods(
        playerEntityRef: Ref<EntityStore>,
        store: Store<EntityStore>,
        giveIfNone: Boolean,
        asset: RodProgressionAsset = getActiveProgressionAsset()
    ): RodSyncResult {
        val progression = getProgression(playerEntityRef, store)
        val highestTier = getHighestUnlockedRod(progression, asset)
        val targetRodId = highestTier.itemId
        val targetRodName = highestTier.resolveName()

        val combinedInventory: ItemContainer = InventoryComponent.getCombined(
            store, playerEntityRef, *InventoryComponent.HOTBAR_FIRST
        )

        var rodCountBefore = 0
        var removedCount = 0
        var keptTargetRod = false

        val capacity = combinedInventory.capacity
        for (slot in 0 until capacity) {
            val stack = combinedInventory.getItemStack(slot.toShort()) ?: continue
            if (isFishingRod(stack.itemId, asset)) {
                rodCountBefore += stack.quantity
                if (stack.itemId == targetRodId && !keptTargetRod) {
                    // Keep at most 1 of the player's highest unlocked rod
                    if (stack.quantity > 1) {
                        val extra = stack.quantity - 1
                        combinedInventory.removeItemStackFromSlot(slot.toShort(), extra)
                        removedCount += extra
                    }
                    keptTargetRod = true
                } else {
                    // Remove all rods player should not have (wrong tiers or duplicates)
                    combinedInventory.removeItemStackFromSlot(slot.toShort())
                    removedCount += stack.quantity
                }
            }
        }

        var gaveRod = false
        if (!keptTargetRod && giveIfNone) {
            val newRod = ItemStack(targetRodId, 1)
            val transaction = combinedInventory.addItemStack(newRod)
            val remainder = transaction.remainder
            if (remainder != null && !remainder.isEmpty) {
                ItemUtils.dropItem(playerEntityRef, remainder, store)
            } else {
                Player.notifyPickupItem(playerEntityRef, newRod, null, store)
            }
            gaveRod = true
        }

        return RodSyncResult(
            rodCountBefore = rodCountBefore,
            removedRodsCount = removedCount,
            gaveRod = gaveRod,
            targetRodId = targetRodId,
            targetRodName = targetRodName
        )
    }

    fun handleBarrelInteract(playerEntityRef: Ref<EntityStore>, store: Store<EntityStore>) {
        val playerRef = store.getComponent(playerEntityRef, PlayerRef.getComponentType())
        val result = syncPlayerRods(playerEntityRef, store, giveIfNone = true)

        if (playerRef != null) {
            when {
                result.gaveRod -> {
                    playerRef.sendMessage(Message.raw("You grabbed a ${result.targetRodName} from the barrel"))
                }
                else -> {
                    playerRef.sendMessage(Message.raw("You already have your fishing rod (${result.targetRodName})!"))
                }
            }
        }
    }

    fun getRodStatsFormatted(rodId: String): String? {
        return runCatching {
            val stats = MinigameRodStats.getRodStatsFromRodId(rodId)
            if (stats != null) {
                "Control: ${"%.1f".format(stats.control)}  |  Difficulty: ${"%.1f".format(stats.difficulty)}\n" +
                "Forgiveness: ${"%.1f".format(stats.forgiveness)}  |  Stamina: ${"%.1f".format(stats.stamina)}"
            } else null
        }.getOrNull()
    }
}
