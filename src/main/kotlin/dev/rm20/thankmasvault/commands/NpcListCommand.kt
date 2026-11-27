package dev.rm20.thankmasvault.commands

import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.permissions.provider.HytalePermissionsProvider
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import dev.rm20.thankmasvault.npc.NpcManagerPage
import javax.annotation.Nonnull

class NpcListCommand : AbstractPlayerCommand(
    "npclist", "Open UI to view, sort, and teleport to NPCs"
) {

    init {
        addAliases("allnpcs", "listnpcs", "npcui", "tpnpc", "npctp", "npcmenu")
        setPermissionGroups(HytalePermissionsProvider.GROUP_ADMIN)
    }

    override fun execute(
        @Nonnull context: CommandContext,
        @Nonnull store: Store<EntityStore>,
        @Nonnull ref: Ref<EntityStore>,
        @Nonnull playerRef: PlayerRef,
        @Nonnull world: World
    ) {
        val player = store.getComponent(ref, Player.getComponentType()) ?: return
        val page = NpcManagerPage(playerRef)
        player.pageManager.openCustomPage(ref, store, page)
    }
}
