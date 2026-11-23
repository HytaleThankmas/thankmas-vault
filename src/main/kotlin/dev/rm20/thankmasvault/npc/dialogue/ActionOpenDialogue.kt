package dev.rm20.thankmasvault.npc.dialogue

import acorn.thankmasdelivery.component.DeliveryNpc
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.modules.entity.component.DisplayNameComponent
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport
import com.hypixel.hytale.server.npc.corecomponents.ActionBase
import com.hypixel.hytale.server.npc.instructions.ExecutionSupport
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider
import dev.rm20.thankmasvault.ThankmasVault
import dev.rm20.thankmasvault.assets.NpcAsset
import dev.rm20.thankmasvault.assets.VaultShopAsset
import dev.rm20.thankmasvault.economy.EconomyAPI
import dev.rm20.thankmasvault.npc.VaultNpcComponent
import dev.rm20.thankmasvault.npc.player.PlayerNpcAPI
import dev.rm20.thankmasvault.npc.player.PlayerNpcComponent
import dev.rm20.thankmasvault.utils.CameraUtil.setFixedCameraFromPoint
import javax.annotation.Nonnull
import javax.annotation.Nullable

class ActionOpenDialogue(
    builder: BuilderActionOpenDialogue, support: BuilderSupport
) : ActionBase(builder) {

    companion object {
        @JvmStatic
        var dialogueInterceptor: DialogueInterceptor? = null
    }


    override fun canExecute(
        @Nonnull ref: Ref<EntityStore>,
        @Nonnull executionSupport: ExecutionSupport,
        @Nullable sensorInfo: InfoProvider?,
        dt: Double,
        @Nonnull store: Store<EntityStore>
    ): Boolean {
        return super.canExecute(
            ref, executionSupport, sensorInfo, dt, store
        ) && executionSupport.stateSupport.interactionIterationTarget != null
    }


    override fun execute(
        @Nonnull ref: Ref<EntityStore>,
        @Nonnull executionSupport: ExecutionSupport,
        @Nullable sensorInfo: InfoProvider?,
        dt: Double,
        @Nonnull store: Store<EntityStore>
    ): Boolean {
        super.execute(ref, executionSupport, sensorInfo, dt, store)

        val targetRef = executionSupport.stateSupport.interactionIterationTarget ?: return false
        val playerRef = store.getComponent(targetRef, PlayerRef.getComponentType()) ?: return false

        var vaultNpcComponent = store.getComponent(ref, VaultNpcComponent.componentType)
        val deliveryNpc = store.getComponent(ref, DeliveryNpc.getComponentType())
        val displayNameComp = store.getComponent(ref, DisplayNameComponent.getComponentType())

        if (vaultNpcComponent == null) {
            val fallbackName = deliveryNpc?.name() ?: displayNameComp?.displayName?.rawText ?: ""
            if (fallbackName.isNotBlank()) {
                vaultNpcComponent = VaultNpcComponent().apply { this.displayName = fallbackName }
            } else {
                return false
            }
        }

        val npcName = vaultNpcComponent.displayName.ifBlank {
            deliveryNpc?.name() ?: displayNameComp?.displayName?.rawText ?: "Unknown NPC"
        }
        val npcId = (deliveryNpc?.id() ?: npcName).trim()

        // Check and record first-time interaction
        val isFirstTime = PlayerNpcAPI.recordInteraction(playerRef, store, npcId, npcName)
        if (isFirstTime) {
            val coins = if (vaultNpcComponent.firstTimeCoins > 0) vaultNpcComponent.firstTimeCoins else 10
            if (coins > 0) {
                EconomyAPI.deposit(targetRef, store, coins)
                playerRef.sendMessage(Message.raw("You discovered $npcName for the first time! Earned $coins coins."))
            }
        }

        var dialogue: Dialogue? = null
        if (vaultNpcComponent.dialogueId.isNotBlank()) {
            dialogue = NpcAsset.getById(vaultNpcComponent.dialogueId)?.dialogue
        }
        if (dialogue == null && vaultNpcComponent.dialogueText.isNotBlank()) {
            val quickNode = DialogueNode(
                content = listOf(Content(subtitle = vaultNpcComponent.dialogueText)),
                choices = listOf(Choice(title = "Bye", null, null, null, "Doors"))
            )
            dialogue = Dialogue(
                startNode = "start",
                nodes = mapOf("start" to quickNode)
            )
        }
        if (dialogue == null) {
            val name = vaultNpcComponent.displayName
            if (name.isNotBlank()) {
                val id = if (store.getComponent(ref, PlayerNpcComponent.componentType) != null) {
                    "Player_$name"
                } else {
                    "NPC_$name"
                }

                val npcAsset = NpcAsset.getById(id)
                if (npcAsset != null) {
                    dialogue = npcAsset.dialogue
                }
            }
        }

        // Handle first-time custom line
        if (isFirstTime) {
            if (vaultNpcComponent.firstTimeDialogueText.isNotBlank()) {
                val normalStart = dialogue?.startNode?.ifBlank { "start" } ?: "start"
                val hasNormalDialogue = dialogue != null && dialogue.nodes.containsKey(normalStart)
                val choices = if (hasNormalDialogue && dialogue.nodes[normalStart]?.choices?.isNotEmpty() == true) {
                    listOf(Choice(title = "Continue...", next = normalStart, icon = "Speach"))
                } else {
                    listOf(Choice(title = "Nice to meet you!", action = "close", icon = "Doors"))
                }

                val firstTimeNode = DialogueNode(
                    content = listOf(Content(subtitle = vaultNpcComponent.firstTimeDialogueText)),
                    next = if (hasNormalDialogue) normalStart else null,
                    choices = choices
                )

                val map = HashMap(dialogue?.nodes ?: emptyMap())
                map["__first_time__"] = firstTimeNode
                dialogue = Dialogue(startNode = "__first_time__", nodes = map)
            } else if (dialogue != null && dialogue.nodes.containsKey("first_time")) {
                dialogue = Dialogue(startNode = "first_time", nodes = dialogue.nodes)
            }
        }

        if (dialogue == null) {
            val fallbackNode = DialogueNode(
                content = listOf(Content(subtitle = "Hello! Nice to meet you.")),
                choices = listOf(Choice(title = "Bye", action = "close", icon = "Doors"))
            )
            dialogue = Dialogue(startNode = "start", nodes = mapOf("start" to fallbackNode))
        }

        val pointId = vaultNpcComponent.pointId

        val effectiveName = vaultNpcComponent.displayName.ifBlank { npcName }
        val finalDialogue = dialogueInterceptor?.intercept(playerRef, ref, effectiveName, dialogue) ?: dialogue

        val page = DialoguePage(playerRef, ref, finalDialogue, effectiveName)
        val playerRef1 = playerRef.reference ?: return false

        val player = store.getComponent(playerRef1, Player.getComponentType()) ?: return false
        if (pointId.isNotBlank()) {
            setFixedCameraFromPoint(playerRef, pointId, npcRef = ref)
        }
        player.pageManager.openCustomPage(playerRef1, store, page)

        return true
    }
}

