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
import com.hypixel.hytale.server.core.ui.builder.EventData
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import dev.rm20.thankmasvault.economy.EconomyAPI
import dev.rm20.thankmasvault.utils.CameraUtil
import javax.annotation.Nonnull

// TODO: Make the ui similar to warframe's shop so npc on the right with shop on the left

class ShopPage(
    playerRef: PlayerRef, val shopAsset: ShopAsset
) : InteractiveCustomUIPage<ShopPage.ShopEventData>(
    playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, ShopEventData.CODEC
) {
    override fun build(
        @Nonnull ref: Ref<EntityStore>,
        @Nonnull commandBuilder: UICommandBuilder,
        @Nonnull eventBuilder: UIEventBuilder,
        @Nonnull store: Store<EntityStore>
    ) {
        commandBuilder.append("Shop/ShopPage.ui")
        val title = shopAsset.getDisplayName()
        commandBuilder.set("#ShopTitle.Text", title)
        val balance = EconomyAPI.getBalance(ref, store)
        commandBuilder.set("#PlayerBalance.Text", "$balance Coins")
        commandBuilder.clear("#ItemsGrid")
        val items = shopAsset.items
        for (i in items.indices) {
            val item = items[i]
            val selector = "#ItemsGrid[$i]"
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

            eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "$selector #BuyButton",
                EventData.of(ShopEventData.ITEM_INDEX, i.toString()),
                false
            )
        }
    }

    override fun handleDataEvent(
        @Nonnull ref: Ref<EntityStore>, @Nonnull store: Store<EntityStore>, @Nonnull data: ShopEventData
    ) {

        val index = data.itemIndex
        if (index < 0 || index >= shopAsset.items.size) return
        val item = shopAsset.items[index]
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

        // Failour with Economy
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


        // Refresh UI
        refreshUI(ref, store)
    }

    private fun refreshUI(@Nonnull ref: Ref<EntityStore>, @Nonnull store: Store<EntityStore>) {
        val commandBuilder = UICommandBuilder()
        val eventBuilder = UIEventBuilder()
        build(ref, commandBuilder, eventBuilder, store)
        sendUpdate(commandBuilder, eventBuilder, true)
    }

    class ShopEventData {
        var itemIndex: Int = -1

        companion object {
            const val ITEM_INDEX = "ItemIndex"

            @JvmField
            val CODEC: BuilderCodec<ShopEventData> = BuilderCodec.builder(ShopEventData::class.java, ::ShopEventData)
                .append(KeyedCodec(ITEM_INDEX, Codec.STRING), { data, s ->
                    data.itemIndex = s?.toIntOrNull() ?: -1
                }, { data -> data.itemIndex.toString() }).add().build()
        }
    }


    override fun onDismiss(ref: Ref<EntityStore>, store: Store<EntityStore>) {
        super.onDismiss(ref, store)
        // Resets camera back
        CameraUtil.resetCamera(playerRef)
    }
}