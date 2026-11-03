package dev.rm20.thankmasvault.fishing

import com.hypixel.hytale.codec.Codec
import com.hypixel.hytale.codec.KeyedCodec
import com.hypixel.hytale.codec.builder.BuilderCodec
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.protocol.packets.interface_.CustomPage
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage
import com.hypixel.hytale.server.core.ui.Anchor
import com.hypixel.hytale.server.core.ui.Value
import com.hypixel.hytale.server.core.ui.builder.EventData
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import dev.rm20.thankmasvault.assets.RodProgressionAsset
import dev.rm20.thankmasvault.assets.RodTierInfo
import dev.rm20.thankmasvault.economy.EconomyAPI
import dev.rm20.thankmasvault.utils.CameraUtil
import javax.annotation.Nonnull

class RodShopPage(
    playerRef: PlayerRef,
    val rodProgressionAsset: RodProgressionAsset,
    val npcRef: Ref<EntityStore>? = null
) : InteractiveCustomUIPage<RodShopPage.RodShopEventData>(
    playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, RodShopEventData.CODEC
) {
    private val tiers: Array<RodTierInfo> get() = rodProgressionAsset.tiers
    private var isDismissed = false
    private var currentHoveredSelector: String? = null

    override fun build(
        @Nonnull ref: Ref<EntityStore>,
        @Nonnull commandBuilder: UICommandBuilder,
        @Nonnull eventBuilder: UIEventBuilder,
        @Nonnull store: Store<EntityStore>
    ) {
        commandBuilder.append("Shop/ShopPage.ui")

        val title = rodProgressionAsset.getDisplayName()
        commandBuilder.set("#ShopTitle.Text", title)

        val balance = EconomyAPI.getBalance(ref, store)
        commandBuilder.set("#PlayerBalance.Text", "$balance Coins")
        commandBuilder.clear("#ItemsGrid")

        val progression = FishingRodService.getProgression(ref, store)

        val firstTier = tiers.firstOrNull()
        if (firstTier != null && currentHoveredSelector == null) {
            val isUnlocked = progression.isUnlocked(firstTier.itemId)
            updateItemInfoPreview(commandBuilder, firstTier, isUnlocked, !isUnlocked, null)
        }

        for (i in tiers.indices) {
            val tier = tiers[i]
            val selector = "#ItemsGrid[$i]"
            val name = tier.resolveName()
            val isUnlocked = progression.isUnlocked(tier.itemId)
            val canUpgrade = !isUnlocked && (i == 0 || progression.isUnlocked(tiers[i - 1].itemId))
            val prevTierName = if (i > 0) tiers[i - 1].resolveName() else null

            if (currentHoveredSelector == selector) {
                updateItemInfoPreview(commandBuilder, tier, isUnlocked, canUpgrade, prevTierName)
            }

            commandBuilder.append("#ItemsGrid", "Shop/ShopItemCard.ui")

            commandBuilder.set("$selector #ItemIcon.Visible", true)
            commandBuilder.set("$selector #CosmeticIcon.Visible", false)
            commandBuilder.set("$selector #ItemIcon.ItemId", tier.itemId)
            commandBuilder.set("$selector #ItemQuantity.Text", "")
            commandBuilder.set("$selector #ItemName.Text", name)

            when {
                isUnlocked -> {
                    commandBuilder.set("$selector #ItemCost.Text", "Unlocked")
                    commandBuilder.set("$selector #ItemCost.Style.TextColor", "#81c784")
                    commandBuilder.set("$selector #BuyButton.Disabled", true)
                }
                canUpgrade -> {
                    val cost = tier.cost
                    val canAfford = balance >= cost
                    commandBuilder.set("$selector #ItemCost.Text", "$cost Coins")
                    val costColor = if (canAfford) "#a0c8e0" else "#e57373"
                    commandBuilder.set("$selector #ItemCost.Style.TextColor", costColor)
                    commandBuilder.set("$selector #BuyButton.Disabled", !canAfford)
                }
                else -> {
                    commandBuilder.set("$selector #ItemCost.Text", "Locked")
                    commandBuilder.set("$selector #ItemCost.Style.TextColor", "#888888")
                    commandBuilder.set("$selector #BuyButton.Disabled", true)
                }
            }

            eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "$selector #BuyButton",
                EventData(mapOf(
                    RodShopEventData.ITEM_INDEX to i.toString(),
                    RodShopEventData.ACTION to "buy",
                    RodShopEventData.SELECTOR to selector
                )),
                false
            )

            eventBuilder.addEventBinding(
                CustomUIEventBindingType.MouseEntered,
                "$selector #BuyButton",
                EventData(mapOf(
                    RodShopEventData.ITEM_INDEX to i.toString(),
                    RodShopEventData.ACTION to "hover_enter",
                    RodShopEventData.SELECTOR to selector
                )),
                false
            )

            eventBuilder.addEventBinding(
                CustomUIEventBindingType.MouseExited,
                "$selector #BuyButton",
                EventData(mapOf(
                    RodShopEventData.ITEM_INDEX to i.toString(),
                    RodShopEventData.ACTION to "hover_exit",
                    RodShopEventData.SELECTOR to selector
                )),
                false
            )
        }
    }

    private fun updateItemInfoPreview(
        commandBuilder: UICommandBuilder,
        tier: RodTierInfo,
        isUnlocked: Boolean,
        canUpgrade: Boolean,
        prevTierName: String?
    ) {
        commandBuilder.set("#ItemInfoContainer #ItemIcon.Visible", true)
        commandBuilder.set("#ItemInfoContainer #CosmeticIcon.Visible", false)
        commandBuilder.set("#ItemInfoContainer #ItemIcon.ItemId", tier.itemId)
        commandBuilder.set("#ItemInfoContainer #ItemName.Text", tier.resolveName())

        when {
            isUnlocked -> {
                commandBuilder.set("#ItemInfoContainer #ItemCost.Text", "Unlocked")
                commandBuilder.set("#ItemInfoContainer #ItemCost.Style.TextColor", "#81c784")
            }
            canUpgrade -> {
                commandBuilder.set("#ItemInfoContainer #ItemCost.Text", "${tier.cost} Coins")
                commandBuilder.set("#ItemInfoContainer #ItemCost.Style.TextColor", "#f0c040")
            }
            else -> {
                val reqMsg = if (prevTierName != null) "Locked (Requires $prevTierName)" else "Locked"
                commandBuilder.set("#ItemInfoContainer #ItemCost.Text", reqMsg)
                commandBuilder.set("#ItemInfoContainer #ItemCost.Style.TextColor", "#888888")
            }
        }

        val baseDesc = tier.resolveDescription()
        val stats = FishingRodService.getRodStatsFormatted(tier.itemId)

        commandBuilder.set("#ItemInfoContainer #ItemDescription.Text", baseDesc)
    }

    override fun handleDataEvent(
        @Nonnull ref: Ref<EntityStore>,
        @Nonnull store: Store<EntityStore>,
        @Nonnull data: RodShopEventData
    ) {
        if (isDismissed) return

        val index = data.itemIndex
        if (index < 0 || index >= tiers.size) return
        val tier = tiers[index]

        when (data.action) {
            "buy" -> handlePurchase(ref, store, index, tier)
            "hover_enter" -> handleMouseEnter(ref, store, index, data)
            "hover_exit" -> handleMouseExit(ref, store, data)
        }
    }

    private fun handlePurchase(
        ref: Ref<EntityStore>,
        store: Store<EntityStore>,
        tierIndex: Int,
        tier: RodTierInfo
    ) {
        val progression = FishingRodService.getProgression(ref, store)

        if (progression.isUnlocked(tier.itemId)) {
            playerRef.sendMessage(Message.raw("You already own this fishing rod!"))
            return
        }

        if (tierIndex > 0 && !progression.isUnlocked(tiers[tierIndex - 1].itemId)) {
            val prevName = tiers[tierIndex - 1].resolveName()
            playerRef.sendMessage(Message.raw("You must unlock the $prevName first!"))
            return
        }

        val balance = EconomyAPI.getBalance(ref, store)
        if (balance < tier.cost) {
            playerRef.sendMessage(Message.raw("You cannot afford this rod upgrade!"))
            return
        }

        if (!EconomyAPI.withdraw(ref, store, tier.cost)) {
            playerRef.sendMessage(Message.raw("Transaction failed."))
            return
        }

        // Unlock this tier
        progression.unlockRod(tier.itemId)

        // Give new rod and remove old / unauthorized rods
        FishingRodService.syncPlayerRods(ref, store, giveIfNone = true, rodProgressionAsset)

        playerRef.sendMessage(Message.raw("Upgraded fishing rod to ${tier.resolveName()}!"))

        refreshUI(ref, store)
    }

    private fun handleMouseEnter(
        @Nonnull ref: Ref<EntityStore>,
        @Nonnull store: Store<EntityStore>,
        tierIndex: Int,
        @Nonnull data: RodShopEventData
    ) {
        if (isDismissed || !data.selector.startsWith("#ItemsGrid")) return
        val commandBuilder = UICommandBuilder()
        val tier = tiers.getOrNull(tierIndex) ?: return
        val progression = FishingRodService.getProgression(ref, store)

        if (currentHoveredSelector != null && currentHoveredSelector != data.selector) {
            commandBuilder.set("$currentHoveredSelector #CardBorder.Background", "#14202c")
            commandBuilder.setObject("$currentHoveredSelector #CardBorder.Anchor", DEFAULT_CARD_ANCHOR)
        }
        currentHoveredSelector = data.selector

        val isUnlocked = progression.isUnlocked(tier.itemId)
        val canUpgrade = !isUnlocked && (tierIndex == 0 || progression.isUnlocked(tiers[tierIndex - 1].itemId))
        val prevTierName = if (tierIndex > 0) tiers[tierIndex - 1].resolveName() else null

        updateItemInfoPreview(commandBuilder, tier, isUnlocked, canUpgrade, prevTierName)

        if (data.selector.isNotEmpty()) {
            commandBuilder.set("${data.selector} #CardBorder.Background", "#24394e")
            commandBuilder.setObject("${data.selector} #CardBorder.Anchor", HOVERED_CARD_ANCHOR)
        }

        sendUpdate(commandBuilder, false)
    }

    private fun handleMouseExit(
        @Nonnull ref: Ref<EntityStore>,
        @Nonnull store: Store<EntityStore>,
        @Nonnull data: RodShopEventData
    ) {
        if (isDismissed || !data.selector.startsWith("#ItemsGrid")) return
        val commandBuilder = UICommandBuilder()

        if (data.selector.isNotEmpty()) {
            commandBuilder.set("${data.selector} #CardBorder.Background", "#14202c")
            commandBuilder.setObject("${data.selector} #CardBorder.Anchor", DEFAULT_CARD_ANCHOR)
        }
        if (currentHoveredSelector == data.selector) {
            currentHoveredSelector = null
        }

        sendUpdate(commandBuilder, false)
    }

    override fun sendUpdate(commandBuilder: UICommandBuilder?, clear: Boolean) {
        sendUpdate(commandBuilder, null, clear)
    }

    override fun sendUpdate(commandBuilder: UICommandBuilder?, eventBuilder: UIEventBuilder?, clear: Boolean) {
        if (isDismissed) return
        val ref = playerRef.reference ?: return
        val store = ref.store
        val world = store.externalData.world

        world.execute {
            if (!ref.isValid || isDismissed) return@execute
            val playerComponent = store.getComponent(ref, Player.getComponentType()) ?: return@execute
            if (playerComponent.pageManager.customPage !== this) return@execute

            playerComponent.pageManager.updateLegacyCustomPage(
                CustomPage(
                    javaClass.name, false, clear, lifetime,
                    commandBuilder?.commands ?: UICommandBuilder.EMPTY_COMMAND_ARRAY,
                    eventBuilder?.events ?: UIEventBuilder.EMPTY_EVENT_BINDING_ARRAY
                )
            )
        }
    }

    private fun refreshUI(@Nonnull ref: Ref<EntityStore>, @Nonnull store: Store<EntityStore>) {
        val commandBuilder = UICommandBuilder()
        val eventBuilder = UIEventBuilder()
        build(ref, commandBuilder, eventBuilder, store)
        sendUpdate(commandBuilder, eventBuilder, true)
    }

    override fun onDismiss(ref: Ref<EntityStore>, store: Store<EntityStore>) {
        super.onDismiss(ref, store)
        isDismissed = true
        CameraUtil.resetCamera(playerRef)
    }

    class RodShopEventData {
        var itemIndex: Int = -1
        var action: String = ""
        var selector: String = ""

        companion object {
            const val ITEM_INDEX = "ItemIndex"
            const val ACTION = "Action"
            const val SELECTOR = "Selector"

            @JvmField
            val CODEC: BuilderCodec<RodShopEventData> = BuilderCodec.builder(RodShopEventData::class.java, ::RodShopEventData)
                .append(KeyedCodec(ITEM_INDEX, Codec.STRING),
                    { data, s -> data.itemIndex = s?.toIntOrNull() ?: -1 },
                    { data -> data.itemIndex.toString() }
                ).add()
                .append(KeyedCodec(ACTION, Codec.STRING),
                    { data, s -> data.action = s ?: "" },
                    { data -> data.action }
                ).add()
                .append(KeyedCodec(SELECTOR, Codec.STRING),
                    { data, s -> data.selector = s ?: "" },
                    { data -> data.selector }
                ).add()
                .build()
        }
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
