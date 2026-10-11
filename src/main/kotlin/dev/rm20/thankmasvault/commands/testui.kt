package dev.rm20.thankmasvault.commands

import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import dev.rm20.thankmasvault.economy.WalletComponent
import dev.rm20.thankmasvault.npc.dialogue.Choice
import dev.rm20.thankmasvault.npc.dialogue.Content
import dev.rm20.thankmasvault.npc.dialogue.Dialogue
import dev.rm20.thankmasvault.npc.dialogue.DialogueNode
import dev.rm20.thankmasvault.npc.dialogue.DialoguePage
import javax.annotation.Nonnull

class testui : AbstractPlayerCommand(
    "testui", "Check your current coin balance") {

    override fun execute(
        @Nonnull context: CommandContext,
        @Nonnull store: Store<EntityStore>,
        @Nonnull ref: Ref<EntityStore>,
        @Nonnull playerRef: PlayerRef,
        @Nonnull world: World
    ) {
        val dialogue = Dialogue(
            startNode = "start",
            nodes = mapOf(
                "start" to DialogueNode(
                    content = Content(subtitle = "Hello, traveler! How can I help you?"),
                    choices = listOf(
                        Choice(title = "Tell me more", next = "info"),
                        Choice(title = "Goodbye")
                    )
                ),
                "info" to DialogueNode(
                    content = Content(subtitle = "This is a test dialogue system."),
                    choices = listOf(
                        Choice(title = "Back to start", next = "start"),
                        Choice(title = "Close")
                    )
                )
            )
        )
        val page = DialoguePage(playerRef, ref, dialogue)
        val player = store.getComponent(ref, Player.getComponentType()) ?: return
        player.pageManager.openCustomPage(ref, store, page)
    }
}