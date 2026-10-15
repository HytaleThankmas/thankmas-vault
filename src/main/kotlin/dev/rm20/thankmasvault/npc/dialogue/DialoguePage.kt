package dev.rm20.thankmasvault.npc.dialogue

import com.hypixel.hytale.codec.Codec
import com.hypixel.hytale.codec.KeyedCodec
import com.hypixel.hytale.codec.builder.BuilderCodec
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.protocol.packets.interface_.CustomPage
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage
import com.hypixel.hytale.server.core.ui.Anchor
import com.hypixel.hytale.server.core.ui.PatchStyle
import com.hypixel.hytale.server.core.ui.Value
import com.hypixel.hytale.server.core.ui.builder.EventData
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.hypixel.hytale.protocol.SoundCategory
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent
import com.hypixel.hytale.server.core.universe.world.SoundUtil
import dev.rm20.thankmasvault.npc.VaultNpcComponent
import dev.rm20.thankmasvault.utils.CameraUtil
import javax.annotation.Nonnull

class DialoguePage(
    playerRef: PlayerRef,
    val targetEntityRef: Ref<EntityStore>,
    val dialogue: Dialogue,
    val name: String? = null,
    private val onActionTriggered: ((action: String, ref: Ref<EntityStore>, store: Store<EntityStore>) -> Unit)? = null
) : InteractiveCustomUIPage<DialoguePage.DialoguePageData>(
    playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, DialoguePageData.CODEC
) {

    private var currentNodeId: String = dialogue.startNode
    private var currentHoveredSelector: String? = null
    private var isDismissed: Boolean = false
    private var keepCameraOnDismiss: Boolean = false

    override fun build(
        @Nonnull ref: Ref<EntityStore>,
        @Nonnull commandBuilder: UICommandBuilder,
        @Nonnull eventBuilder: UIEventBuilder,
        @Nonnull store: Store<EntityStore>
    ) {
        commandBuilder.append("NPC/Dialogue.ui")

        val node = dialogue.nodes[currentNodeId] ?: run {
            close()
            return
        }

        val content = node.getRandomContent()

        if (content != null && content.soundEventId.isNotEmpty()) {
            val soundIndex = SoundEvent.getAssetMap().getIndex(content.soundEventId)
            if (soundIndex != SoundEvent.EMPTY_ID) {
                SoundUtil.playSoundEvent2dToPlayer(playerRef, soundIndex, SoundCategory.Voice)
            }
        }

        commandBuilder.append("#Subtitle", "Shop/ShopSubtitle.ui")
        commandBuilder.set("#ShopkeeperSubtitleText.Text", "\"${content?.subtitle ?: ""}\"")
        if (!name.isNullOrBlank())
        {
            commandBuilder.set("#ShopkeeperName.Text", "[${name}]: ")
        }
        commandBuilder.clear("#ChoiceGrid")

        val choices = node.choices
        if (!choices.isNullOrEmpty()) {
            for (i in choices.indices) {
                val choice = choices[i]
                val selector = "#ChoiceGrid[$i]"

                commandBuilder.append("#ChoiceGrid", "NPC/ChoiceSlot.ui")
                commandBuilder.set("$selector #ChoiceText.Text", choice.title)

                eventBuilder.addEventBinding(
                    CustomUIEventBindingType.Activating, "$selector #ChoiceButton", EventData(
                        mapOf(
                            DialoguePageData.CHOICE_INDEX to i.toString(),
                            DialoguePageData.ACTION to "select",
                            DialoguePageData.SELECTOR to selector
                        )
                    ), false
                )

                eventBuilder.addEventBinding(
                    CustomUIEventBindingType.MouseEntered, "$selector #ChoiceButton", EventData(
                        mapOf(
                            DialoguePageData.CHOICE_INDEX to i.toString(),
                            DialoguePageData.ACTION to "hover_enter",
                            DialoguePageData.SELECTOR to selector
                        )
                    ), false
                )

                eventBuilder.addEventBinding(
                    CustomUIEventBindingType.MouseExited, "$selector #ChoiceButton", EventData(
                        mapOf(
                            DialoguePageData.CHOICE_INDEX to i.toString(),
                            DialoguePageData.ACTION to "hover_exit",
                            DialoguePageData.SELECTOR to selector
                        )
                    ), false
                )
            }
        } else if (node.next != null) {
            commandBuilder.append("#ChoiceGrid", "NPC/ChoiceSlot.ui")
            commandBuilder.set("#ChoiceGrid[0] #ChoiceText.Text", "Continue...")

            eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating, "#ChoiceGrid[0] #ChoiceButton", EventData(
                    mapOf(
                        DialoguePageData.CHOICE_INDEX to "0",
                        DialoguePageData.ACTION to "continue",
                        DialoguePageData.SELECTOR to "#ChoiceGrid[0]"
                    )
                ), false
            )
        } else {
            commandBuilder.append("#ChoiceGrid", "NPC/ChoiceSlot.ui")
            commandBuilder.set("#ChoiceGrid[0] #ChoiceText.Text", "Close")

            eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating, "#ChoiceGrid[0] #ChoiceButton", EventData(
                    mapOf(
                        DialoguePageData.ACTION to "close", DialoguePageData.SELECTOR to "#ChoiceGrid[0]"
                    )
                ), false
            )
        }
    }

    override fun handleDataEvent(
        @Nonnull ref: Ref<EntityStore>, @Nonnull store: Store<EntityStore>, @Nonnull data: DialoguePageData
    ) {
        val node = dialogue.nodes[currentNodeId] ?: return

        if (isDismissed) return
        when (data.action) {
            "select" -> {
                val choices = node.choices
                if (choices.isNullOrEmpty() || data.choiceIndex !in choices.indices) return
                val choice = choices[data.choiceIndex]


                if (choice.next != null && dialogue.nodes.containsKey(choice.next)) {
                    currentNodeId = choice.next!!
                    currentHoveredSelector = null
                    refreshUI(ref, store)
                } else {
                    //close()
                }

                choice.action?.let { actionId ->
                    keepCameraOnDismiss = DialogueActionRegistry.shouldKeepCamera(actionId)
                    try {
                        DialogueActionRegistry.execute(actionId, playerRef, targetEntityRef, choice)
                        onActionTriggered?.invoke(actionId, ref, store)
                    } finally {
                        if (!isDismissed) {
                            keepCameraOnDismiss = false
                        }
                    }
                }
            }

            "continue" -> {
                node.next?.let { nextId ->
                    if (dialogue.nodes.containsKey(nextId)) {
                        currentNodeId = nextId
                        currentHoveredSelector = null
                        refreshUI(ref, store)
                    } else {
                        close()
                    }
                } ?: close()
            }

            "close" -> close()
            "hover_enter" -> handleMouseEnter(data)
            "hover_exit" -> handleMouseExit(data)
        }
    }

    private fun handleMouseEnter(data: DialoguePageData) {
        if (isDismissed || !data.selector.startsWith("#ChoiceSlot")) return
        val commandBuilder = UICommandBuilder()

        if (currentHoveredSelector != null && currentHoveredSelector != data.selector) {
            commandBuilder.setObject("${currentHoveredSelector} #CardBorder.Anchor", DEFAULT_CARD_ANCHOR)
        }
        currentHoveredSelector = data.selector

        if (data.selector.isNotEmpty()) {
            commandBuilder.setObject("${data.selector} #CardBorder.Anchor", HOVERED_CARD_ANCHOR)
        }

        sendUpdate(commandBuilder, false)
    }

    private fun handleMouseExit(data: DialoguePageData) {
        if (isDismissed || !data.selector.startsWith("#ChoiceSlot")) return
        val commandBuilder = UICommandBuilder()

        if (data.selector.isNotEmpty()) {
            commandBuilder.setObject("${data.selector} #CardBorder.Anchor", DEFAULT_CARD_ANCHOR)
        }
        if (currentHoveredSelector == data.selector) {
            currentHoveredSelector = null
        }

        sendUpdate(commandBuilder, false)
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

    private fun refreshUI(ref: Ref<EntityStore>, store: Store<EntityStore>) {
        val commandBuilder = UICommandBuilder()
        val eventBuilder = UIEventBuilder()
        build(ref, commandBuilder, eventBuilder, store)
        sendUpdate(commandBuilder, eventBuilder, true)
    }

    override fun onDismiss(ref: Ref<EntityStore>, store: Store<EntityStore>) {
        super.onDismiss(ref, store)
        isDismissed = true
        if (!keepCameraOnDismiss) {
            CameraUtil.resetCamera(playerRef)
        }
    }

    class DialoguePageData {
        var choiceIndex: Int = -1
        var action: String = ""
        var selector: String = ""

        companion object {
            const val CHOICE_INDEX = "ChoiceIndex"
            const val ACTION = "Action"
            const val SELECTOR = "Selector"

            @JvmField
            val CODEC: BuilderCodec<DialoguePageData> =
                BuilderCodec.builder(DialoguePageData::class.java, ::DialoguePageData).append(
                        KeyedCodec(CHOICE_INDEX, Codec.STRING),
                        { data, s -> data.choiceIndex = s?.toIntOrNull() ?: -1 },
                        { data -> data.choiceIndex.toString() }).add().append(
                        KeyedCodec(ACTION, Codec.STRING),
                        { data, s -> data.action = s ?: "" },
                        { data -> data.action }).add().append(
                        KeyedCodec(SELECTOR, Codec.STRING),
                        { data, s -> data.selector = s ?: "" },
                        { data -> data.selector }).add().build()
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