package dev.rm20.thankmasvault.commands

import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import dev.rm20.thankmasvault.npc.player.PlayerNpcListPage
import javax.annotation.Nonnull

class PlayerNpcsCommand : AbstractPlayerCommand("playernpcs", "View discovered and undiscovered Player NPCs") {

    init {
        addAliases("playernpc", "npcs", "foundnpcs", "discoverednpcs")
        requireNoPermission()
    }

    override fun execute(
        @Nonnull context: CommandContext,
        @Nonnull store: Store<EntityStore>,
        @Nonnull ref: Ref<EntityStore>,
        @Nonnull playerRef: PlayerRef,
        @Nonnull world: World
    ) {
        val player = store.getComponent(ref, Player.getComponentType()) ?: return
        val page = PlayerNpcListPage(playerRef)
        player.pageManager.openCustomPage(ref, store, page)
    }
}
