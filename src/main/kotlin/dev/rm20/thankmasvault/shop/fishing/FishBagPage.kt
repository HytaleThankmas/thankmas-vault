package dev.rm20.thankmasvault.shop.fishing

import com.hypixel.hytale.codec.Codec
import com.hypixel.hytale.codec.KeyedCodec
import com.hypixel.hytale.codec.builder.BuilderCodec
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage
import com.hypixel.hytale.server.core.ui.builder.EventData
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import dev.rm20.anglersalmanac.Inventory.FishBagComponent
import dev.rm20.thankmasvault.shop.VaultShopAsset
import dev.rm20.thankmasvault.shop.shopKepper.voiceline.voicelineInfo
import javax.annotation.Nonnull

class FishBagPage(
    playerRef: PlayerRef
) : InteractiveCustomUIPage<FishBagPage.FishBagEventData>(
    playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, FishBagEventData.CODEC
) {
    override fun build(
        @Nonnull ref: Ref<EntityStore?>,
        @Nonnull commandBuilder: UICommandBuilder,
        @Nonnull eventBuilder: UIEventBuilder,
        @Nonnull store: Store<EntityStore?>
    ) {
        commandBuilder.append("Shop/FishBagPage.ui")

        val playerEntityRef = playerRef.getReference()
        if (playerEntityRef == null) return

        // Fetch the Fish Bag inventory
        val fishBag = store.getComponent<FishBagComponent?>(playerEntityRef, FishBagComponent.getComponentType())

        commandBuilder.clear("#FishGrid")

        if (fishBag != null) {
            val bagInventory = fishBag.getInventory()
            val capacity = bagInventory.getCapacity().toInt()
            var itemsShown = 0
            var totalValue = 0
            // Iterate through the fish bag slots and populate the grid
            for (i in 0..<capacity) {
                val stack = bagInventory.getItemStack(i.toShort())
                if (stack != null && !stack.isEmpty()) {
                    val selector = "#FishGrid[" + itemsShown + "]"

                    commandBuilder.append("#FishGrid", "Shop/FishBagSlot.ui")
                    commandBuilder.set(selector + " #FishIcon.ItemId", stack.getItemId())

                    val qty = stack.getQuantity()
                    commandBuilder.set(selector + " #FishQuantity.Text", if (qty > 1) qty.toString() else "")


                    val coinValue = FishMarket.getFishValue(stack, qty )
                    totalValue += coinValue

                    val tooltipMessage = Message.translation(stack.getItem().getTranslationKey())
                        .getAnsiMessage() + ": Sell Value: " + coinValue + " Coins"

                    // Apply the message to the root Group's Tooltip property
                    commandBuilder.set(selector + " #HoverCatcher.TooltipText", tooltipMessage)
                    itemsShown++
                }
            }
            commandBuilder.set("#SellAllButton.Text", "Sell all: " + totalValue + " Coins")

            commandBuilder.set("#SellAllButton.Disabled", totalValue <= 0)
        }


        // Bind the Sell All button event
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#SellAllButton",
            EventData.of(FishBagEventData.ACTION, "sell_all"),
            false
        )
    }

    override fun handleDataEvent(
        @Nonnull ref: Ref<EntityStore?>, @Nonnull store: Store<EntityStore?>, @Nonnull data: FishBagEventData
    ) {
        if ("sell_all" == data.action) {
            val playerEntityRef = playerRef.getReference()
            if (playerEntityRef == null) return

            val player = store.getComponent<Player?>(playerEntityRef, Player.getComponentType())
            if (player != null) {
                // Call your FishMarket logic
                FishMarket.sellAllFish(player)

                // Refresh the UI so the bag appears empty
                refreshUI(ref, store)
            }
        }
    }

    private fun refreshUI(@Nonnull ref: Ref<EntityStore?>, @Nonnull store: Store<EntityStore?>) {
        val commandBuilder = UICommandBuilder()
        val eventBuilder = UIEventBuilder()
        build(ref, commandBuilder, eventBuilder, store)
        sendUpdate(commandBuilder, eventBuilder, true)
    }

    /**
     * Event data capturing custom actions from the UI
     */
    class FishBagEventData {
        var action: String? = ""
            private set

        companion object {
            const val ACTION: String = "Action"

            val CODEC: BuilderCodec<FishBagEventData> =
                BuilderCodec.builder(FishBagEventData::class.java, ::FishBagEventData)
                    .append(KeyedCodec(FishBagEventData.ACTION, Codec.STRING), { data, s ->
                        data.action = s ?: ""
                    }, { data -> data.action }).add().build()
        }
    }
}