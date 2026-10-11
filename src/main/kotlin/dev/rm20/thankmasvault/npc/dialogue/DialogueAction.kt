package dev.rm20.thankmasvault.npc.dialogue

import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import dev.rm20.thankmasvault.ThankmasVault

fun interface DialogueAction {
    fun execute(playerRef: PlayerRef, targetEntityRef: Ref<EntityStore>, choice: Choice)
}

object DialogueActionRegistry {
    private val actions = mutableMapOf<String, DialogueAction>()

    fun register(id: String, action: DialogueAction) {
        actions[id.lowercase()] = action
    }

    fun execute(
        id: String,
        playerRef: PlayerRef,
        targetEntityRef: Ref<EntityStore>,
        choice: Choice
    ) {
        val handler = actions[id.lowercase()]
        if (handler != null) {
            handler.execute(playerRef, targetEntityRef, choice)
        } else {
            ThankmasVault.LOGGER.atWarning().log("Unrecognized dialogue action: $id")
        }
    }
}