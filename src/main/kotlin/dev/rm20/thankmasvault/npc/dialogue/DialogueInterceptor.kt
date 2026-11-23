package dev.rm20.thankmasvault.npc.dialogue

import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore

fun interface DialogueInterceptor {
    fun intercept(
        playerRef: PlayerRef,
        npcRef: Ref<EntityStore>,
        npcName: String,
        originalDialogue: Dialogue
    ): Dialogue
}
