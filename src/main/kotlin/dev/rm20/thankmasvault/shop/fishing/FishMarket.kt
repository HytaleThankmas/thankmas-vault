package dev.rm20.thankmasvault.shop.fishing

import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.inventory.ItemStack
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.util.NotificationUtil
import dev.rm20.anglersalmanac.AnglersAlmanac
import dev.rm20.anglersalmanac.Inventory.FishBagComponent
import dev.rm20.anglersalmanac.Models.FishLootManager
import java.awt.Color
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object FishMarket {
    @JvmOverloads
    fun sellAllFish(player: Player, commandContext: CommandContext? = null) {
        val accessor = player.getReference()!!.getStore()
        val fishBag =
            accessor.getComponent(player.getReference()!!, FishBagComponent.getComponentType()) ?: return

        val container = fishBag.getInventory()
        var totalEarned = 0
        var fishSold = 0

        for (i in 0..<container.capacity) {
            val stack = container.getItemStack(i.toShort())

            if (stack != null) {
                totalEarned += getFishValue(stack, stack.getQuantity())
                fishSold += stack.getQuantity()
            }
        }

        if (AnglersAlmanac.getInstance().ECONOMY_HOOK != null && AnglersAlmanac.getInstance().ECONOMY_HOOK.enabled) {
            AnglersAlmanac.getInstance().ECONOMY_HOOK.ensureWallet(player.getReference(), accessor, 0)
            if (!AnglersAlmanac.getInstance().ECONOMY_HOOK.deposit(player.getReference(), accessor, totalEarned)) {
                AnglersAlmanac.LOGGER.atWarning().log("Failed to deposit coins")
                return
            }
        } else {
            AnglersAlmanac.LOGGER.atWarning().log("Economy plugin not available to deposit coins.")
            return
        }
        if (commandContext == null) {
            if (fishSold > 0) {
                val playerRef = player.getReference() ?: return
                val playerRef1 = playerRef.getStore().getComponent(playerRef, PlayerRef.getComponentType()) ?: return
                val titleMessage = Message.join(Message.raw("You earned $totalEarned coins!"))
                titleMessage.color(Color.RED)
                val subtitleMessage = Message.translation("by selling $fishSold fish")
                try {
                    val packetHandler = playerRef1.packetHandler
                    NotificationUtil.sendNotification(
                        packetHandler, titleMessage, subtitleMessage, NotificationStyle.Success
                    )
                } catch (e: Exception) {
                    AnglersAlmanac.LOGGER.atWarning()
                        .log("Failed to send notification to " + playerRef1.username + ": " + e.message)
                }
            }
        } else {
            if (fishSold > 0) {
                commandContext.sendMessage(Message.raw("You sold $fishSold fish for $totalEarned coins!"))
            } else {
                commandContext.sendMessage(Message.raw("Your fish bag is empty!"))
            }
        }

        container.clear()
    }

    fun getFishValue(itemStack: ItemStack, qty: Int = 1): Int {
        val fishData: FishLootManager = FishLootManager.getInternalFishData(itemStack.getItemId()) ?: return 0

        // TODO: move to pre determined amount (need to add field to AA)
        val clampedDiff = min(fishData.getMinigameStats().difficulty.toDouble(), 8.0)
        val clampedStam = min(fishData.getMinigameStats().stamina.toDouble(), 200.0)

        val normDiff = (clampedDiff / 8.0) * 100.0
        val normStam = (clampedStam / 200.0) * 100.0
        val combinedStats = (normDiff + normStam) / 2.0

        val weightModifier: Double
        if (fishData.getWeight() > 10.0) {
            weightModifier = max(1.0 - (fishData.getWeight() / 500.0), 0.7)
        } else {
            weightModifier = max(1.0 - ((10.0 - fishData.getWeight()) / 20.0), 0.85)
        }

        val rarity = fishData.getRarity().lowercase(Locale.getDefault())

        val rarityMultiplier = when (rarity) {
            "junk" -> 0.5
            "common" -> 1.0
            "uncommon" -> 1.1
            "rare" -> 1.2
            "epic" -> 1.3
            "legendary" -> 1.5
            else -> 1.0
        }
        AnglersAlmanac.LOGGER.atInfo().log((combinedStats * weightModifier * rarityMultiplier * qty).toString())
        return (combinedStats * weightModifier * rarityMultiplier * qty).roundToInt()
    }
}
