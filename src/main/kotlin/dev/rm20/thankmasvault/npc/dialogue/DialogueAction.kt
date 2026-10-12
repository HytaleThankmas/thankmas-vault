package dev.rm20.thankmasvault.npc.dialogue

import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import dev.rm20.thankmasvault.ThankmasVault

fun interface DialogueAction {
    fun execute(playerRef: PlayerRef, targetEntityRef: Ref<EntityStore>, choice: Choice)
}

object DialogueActionRegistry {
    data class RegisteredAction(
        val action: DialogueAction,
        val keepCamera: Boolean = false
    )

    private val actions = mutableMapOf<String, RegisteredAction>()

    fun register(id: String, keepCamera: Boolean = false, action: DialogueAction) {
        actions[id.lowercase()] = RegisteredAction(action, keepCamera)
    }

    fun shouldKeepCamera(id: String): Boolean {
        return actions[id.lowercase()]?.keepCamera ?: false
    }

    fun execute(
        id: String,
        playerRef: PlayerRef,
        targetEntityRef: Ref<EntityStore>,
        choice: Choice
    ) {
        val registered = actions[id.lowercase()]
        if (registered != null) {
            registered.action.execute(playerRef, targetEntityRef, choice)
        } else {
            ThankmasVault.LOGGER.atWarning().log("Unrecognized dialogue action: $id")
        }
    }
}