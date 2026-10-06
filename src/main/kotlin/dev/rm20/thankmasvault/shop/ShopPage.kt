package dev.rm20.thankmasvault.shop

import com.hypixel.hytale.codec.Codec
import com.hypixel.hytale.codec.KeyedCodec
import com.hypixel.hytale.codec.builder.BuilderCodec
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.entity.ItemUtils
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage
import com.hypixel.hytale.server.core.inventory.InventoryComponent
import com.hypixel.hytale.server.core.inventory.ItemStack
import com.hypixel.hytale.server.core.inventory.container.ItemContainer
import com.hypixel.hytale.server.core.ui.Anchor
import com.hypixel.hytale.server.core.ui.Value
import com.hypixel.hytale.server.core.ui.builder.EventData
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import dev.rm20.thankmasvault.economy.EconomyAPI
import dev.rm20.thankmasvault.shop.shopKepper.ActionOpenShop
import dev.rm20.thankmasvault.shop.shopKepper.voiceline.voicelineInfo
import dev.rm20.thankmasvault.utils.CameraUtil
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import javax.annotation.Nonnull

// TODO: Make the ui similar to warframe's shop so npc on the right with shop on the left

class ShopPage(
    playerRef: PlayerRef, val vaultShopAsset: VaultShopAsset, val voiceLine: voicelineInfo? = null
) : InteractiveCustomUIPage<ShopPage.ShopEventData>(
    playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, ShopEventData.CODEC
) {

    private var isDismissed = false
    private var hasScheduledSubtitleHide = false
    private var currentHoveredSelector: String? = null

    override fun build(
        @Nonnull ref: Ref<EntityStore>,
        @Nonnull commandBuilder: UICommandBuilder,
        @Nonnull eventBuilder: UIEventBuilder,
        @Nonnull store: Store<EntityStore>
    ) {
        commandBuilder.append("Shop/ShopPage.ui")

        if (voiceLine != null && !isDismissed) {
            commandBuilder.append("#Subtitle","Shop/ShopSubtitle.ui")

            // TODO: allow for custom names on the NPC

            commandBuilder.set("#ShopkeeperSubtitleText.Text", "\"${voiceLine.subtitle}\"")
            if (!hasScheduledSubtitleHide) {
                hasScheduledSubtitleHide = true
                scheduleSubtitleHide(voiceLine.durationMs.toLong())
            }
        }

        val title = vaultShopAsset.getDisplayName()
        commandBuilder.set("#ShopTitle.Text", title)
        val balance = EconomyAPI.getBalance(ref, store)
        commandBuilder.set("#PlayerBalance.Text", "$balance Coins")
        commandBuilder.clear("#ItemsGrid")
        val items = vaultShopAsset.items

        val item = items[0]
        if(currentHoveredSelector == null)
        {
            commandBuilder.set("#ItemInfoContainer #ItemIcon.ItemId", item.itemId)
            val name = item.displayName ?: item.itemId.replace('_', ' ')
            commandBuilder.set("#ItemInfoContainer #ItemName.Text", name)
            commandBuilder.set("#ItemInfoContainer #ItemCost.Text", "${item.cost} Coins")
            val description = item.description ?: ""
            commandBuilder.set("#ItemInfoContainer #ItemDescription.Text", description)
        }


        for (i in items.indices) {
            val item = items[i]
            val selector = "#ItemsGrid[$i]"
            if(currentHoveredSelector == selector)
            {
                commandBuilder.set("#ItemInfoContainer #ItemIcon.ItemId", item.itemId)
                val name = item.displayName ?: item.itemId.replace('_', ' ')
                commandBuilder.set("#ItemInfoContainer #ItemName.Text", name)
                commandBuilder.set("#ItemInfoContainer #ItemCost.Text", "${item.cost} Coins")
                val description = item.description ?: ""
                commandBuilder.set("#ItemInfoContainer #ItemDescription.Text", description)
            }
            commandBuilder.append("#ItemsGrid", "Shop/ShopItemCard.ui")
            commandBuilder.set("$selector #ItemIcon.ItemId", item.itemId)
            val qty = item.quantity
            commandBuilder.set("$selector #ItemQuantity.Text", if (qty > 1) qty.toString() else "")
            val name = item.displayName ?: item.itemId.replace('_', ' ')
            commandBuilder.set("$selector #ItemName.Text", name)
            commandBuilder.set("$selector #ItemCost.Text", "${item.cost} Coins")
            val canAfford = balance >= item.cost
            commandBuilder.set("$selector #BuyButton.Disabled", !canAfford)
            //commandBuilder.set("$selector HoveredBackground", if (canAfford) "#2a5a3a" else "#5a2a2a")
            val costColor = if (canAfford) "#a0c8e0" else "#e57373"
            commandBuilder.set("$selector #ItemCost.Style.TextColor", costColor)


            eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "$selector #BuyButton",
                EventData(mapOf(
                    ShopEventData.ITEM_INDEX to i.toString(),
                    ShopEventData.ACTION to "buy",
                    ShopEventData.SELECTOR to selector
                )),
                false
            )

            eventBuilder.addEventBinding(
                CustomUIEventBindingType.MouseEntered,
                "$selector #BuyButton",
                EventData(mapOf(
                    ShopEventData.ITEM_INDEX to i.toString(),
                    ShopEventData.ACTION to "hover_enter",
                    ShopEventData.SELECTOR to selector
                )),
                false
            )

            eventBuilder.addEventBinding(
                CustomUIEventBindingType.MouseExited,
                "$selector #BuyButton",
                EventData(mapOf(
                    ShopEventData.ITEM_INDEX to i.toString(),
                    ShopEventData.ACTION to "hover_exit",
                    ShopEventData.SELECTOR to selector
                )),
                false
            )
        }
    }

    override fun handleDataEvent(@Nonnull ref: Ref<EntityStore>, @Nonnull store: Store<EntityStore>, @Nonnull data: ShopEventData) {
        val index = data.itemIndex
        if (index < 0 || index >= vaultShopAsset.items.size) return
        val item = vaultShopAsset.items[index]

        when (data.action) {
            "buy" -> handlePurchase(ref, store, item)
            "hover_enter" -> handleMouseEnter(ref, store, data)
            "hover_exit" -> handleMouseExit(ref, store, data)
        }
    }

    private fun handlePurchase(ref: Ref<EntityStore>, store: Store<EntityStore>, item: ShopItem) {
        val playerEntityRef = playerRef.reference ?: return

        val balance = EconomyAPI.getBalance(ref, store)
        if (balance < item.cost) {
            return
        }

        val combinedInventory: ItemContainer = InventoryComponent.getCombined(
            store, playerEntityRef, *InventoryComponent.HOTBAR_FIRST
        )
        val stack = ItemStack(item.itemId, item.quantity)

        // full inv
        if (!combinedInventory.canAddItemStack(stack)) {
            playerRef.sendMessage(Message.raw("Your inventory is full!"))
            return
        }

        // Failure with Economy
        if (!EconomyAPI.withdraw(ref, store, item.cost)) {
            playerRef.sendMessage(Message.raw("Transaction failed"))
            return
        }

        val transaction = combinedInventory.addItemStack(stack)
        val remainder = transaction.remainder
        if (remainder != null && !remainder.isEmpty) {
            val addedQty = stack.quantity - remainder.quantity
            if (addedQty > 0) {
                val notifyStack = stack.withQuantity(addedQty) ?: stack
                Player.notifyPickupItem(playerEntityRef, notifyStack, null, store)
            }
            ItemUtils.dropItem(playerEntityRef, remainder, store)
        } else {
            Player.notifyPickupItem(playerEntityRef, stack, null, store)
        }

        refreshUI(ref, store)
    }

    private fun handleMouseEnter(@Nonnull ref: Ref<EntityStore>, @Nonnull store: Store<EntityStore>, @Nonnull data: ShopEventData)
    {
        val commandBuilder = UICommandBuilder()
        val item = vaultShopAsset.items[data.itemIndex]

        if (currentHoveredSelector != null && currentHoveredSelector != data.selector) {
            commandBuilder.set("$currentHoveredSelector #CardBorder.Background", "#14202c")
            commandBuilder.setObject("$currentHoveredSelector #CardBorder.Anchor", DEFAULT_CARD_ANCHOR)
        }
        currentHoveredSelector = data.selector

        commandBuilder.set("#ItemInfoContainer #ItemIcon.ItemId", item.itemId)
        val name = item.displayName ?: item.itemId.replace('_', ' ')
        commandBuilder.set("#ItemInfoContainer #ItemName.Text", name)
        commandBuilder.set("#ItemInfoContainer #ItemCost.Text", "${item.cost} Coins")
        val description = item.description ?: ""
        commandBuilder.set("#ItemInfoContainer #ItemDescription.Text", description)

        // move button slightly to the right
        if (data.selector.isNotEmpty()) {
            commandBuilder.set("${data.selector} #CardBorder.Background", "#24394e")

            commandBuilder.setObject("${data.selector} #CardBorder.Anchor", HOVERED_CARD_ANCHOR)
        }

        sendUpdate(commandBuilder, false)
    }

    private fun handleMouseExit(@Nonnull ref: Ref<EntityStore>, @Nonnull store: Store<EntityStore>, @Nonnull data: ShopEventData)
    {
        val commandBuilder = UICommandBuilder()

        // reset button
        if (data.selector.isNotEmpty()) {
            commandBuilder.set("${data.selector} #CardBorder.Background", "#14202c")
            commandBuilder.setObject("${data.selector} #CardBorder.Anchor", DEFAULT_CARD_ANCHOR)
        }
        if (currentHoveredSelector == data.selector) {
            currentHoveredSelector = null
        }

        sendUpdate(commandBuilder, false)
    }

    private fun refreshUI(@Nonnull ref: Ref<EntityStore>, @Nonnull store: Store<EntityStore>) {
        val commandBuilder = UICommandBuilder()
        val eventBuilder = UIEventBuilder()
        build(ref, commandBuilder, eventBuilder, store)
        sendUpdate(commandBuilder, eventBuilder, true)
    }

    class ShopEventData {
        var itemIndex: Int = -1
        var action: String = ""
        var selector: String = ""

        companion object {
            const val ITEM_INDEX = "ItemIndex"
            const val ACTION = "Action"
            const val SELECTOR = "Selector"

            @JvmField
            val CODEC: BuilderCodec<ShopEventData> = BuilderCodec.builder(ShopEventData::class.java, ::ShopEventData)
                .append(KeyedCodec(ITEM_INDEX, Codec.STRING),
                    { data, s -> data.itemIndex = s?.toIntOrNull() ?: -1
                }, { data -> data.itemIndex.toString() }).add()

                .append(KeyedCodec(ACTION, Codec.STRING),
                    { data, s -> data.action = s ?: ""
                }, { data -> data.action }).add()

                .append(KeyedCodec(SELECTOR, Codec.STRING),
                    { data, s -> data.selector = s ?: ""
                    }, { data -> data.selector }).add()
                .build()

        }
    }

    private fun scheduleSubtitleHide(durationMs: Long) {
        CompletableFuture.delayedExecutor(durationMs, TimeUnit.MILLISECONDS).execute {
            if (!isDismissed) {
                isDismissed = true
                val commandBuilder = UICommandBuilder()
                commandBuilder.set("#ShopkeeperSubtitleContainer.Visible", false)
                sendUpdate(commandBuilder, false)
            }
        }
    }


    override fun onDismiss(ref: Ref<EntityStore>, store: Store<EntityStore>) {
        super.onDismiss(ref, store)
        isDismissed = true
        // Resets camera back
        CameraUtil.resetCamera(playerRef)
    }

    companion object {

        private val HOVERED_CARD_ANCHOR = Anchor().apply {
            setLeft(Value.of(8))
            setRight(Value.of(0))
            setTop(Value.of(0))
            setBottom(Value.of(0))
        }

        private val DEFAULT_CARD_ANCHOR = Anchor().apply {
            setFull(Value.of(0))
        }
    }
}