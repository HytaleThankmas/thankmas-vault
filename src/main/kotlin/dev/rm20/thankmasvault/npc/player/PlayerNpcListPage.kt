package dev.rm20.thankmasvault.npc.player

import com.hypixel.hytale.codec.Codec
import com.hypixel.hytale.codec.KeyedCodec
import com.hypixel.hytale.codec.builder.BuilderCodec
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import javax.annotation.Nonnull

class PlayerNpcListPage(
    playerRef: PlayerRef
) : InteractiveCustomUIPage<PlayerNpcListPage.PlayerNpcPageData>(
    playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, PlayerNpcPageData.CODEC
) {

    override fun build(
        @Nonnull ref: Ref<EntityStore>,
        @Nonnull commandBuilder: UICommandBuilder,
        @Nonnull eventBuilder: UIEventBuilder,
        @Nonnull store: Store<EntityStore>
    ) {
        commandBuilder.append("NPC/PlayerNpcListPage.ui")

        val discoveredNpcs = PlayerNpcAPI.getFoundNpcs(playerRef, store)
        val allPlayerNpcs = PlayerNpcAPI.getAllPlayerNpcs()

        val foundCount = allPlayerNpcs.count {
            discoveredNpcs.contains(PlayerFoundNpcs.normalize(it.name))
        }
        val totalCount = allPlayerNpcs.size

        commandBuilder.set("#FoundSummary.Text", "Discovered: $foundCount / $totalCount")
        commandBuilder.clear("#NpcGrid")

        for (i in allPlayerNpcs.indices) {
            val entry = allPlayerNpcs[i]
            val selector = "#NpcGrid[$i]"
            val isFound = discoveredNpcs.contains(PlayerFoundNpcs.normalize(entry.name))
            val displayName = entry.getEffectiveDisplayName()
            val imagePath =
                if (!entry.icon.isNullOrBlank()) entry.icon!! else PlayerNpcAPI.resolvePlayerIconPath(entry.name)

            commandBuilder.append("#NpcGrid", "NPC/PlayerNpcCard.ui")

            if (isFound) {
                commandBuilder.set("$selector #PlayerImage.AssetPath", imagePath)
                commandBuilder.set("$selector #PlayerImage.Visible", true)
                commandBuilder.set("$selector #MaskPlayerImage.Visible", false)
                commandBuilder.set("$selector #PlayerName.Text", displayName)
            } else {
                commandBuilder.set("$selector #MaskPlayerImage.AssetPath", imagePath)
                commandBuilder.set("$selector #MaskPlayerImage.Visible", true)
                commandBuilder.set("$selector #PlayerImage.Visible", false)
                commandBuilder.set("$selector #PlayerName.Text", "Not Found")
            }
        }
    }

    override fun handleDataEvent(
        @Nonnull ref: Ref<EntityStore>, @Nonnull store: Store<EntityStore>, @Nonnull data: PlayerNpcPageData
    ) {
        if ("close".equals(data.action, ignoreCase = true) || "back".equals(data.action, ignoreCase = true)) {
            close()
        }
    }

    class PlayerNpcPageData {
        var action: String? = null

        companion object {
            @JvmField
            val CODEC: BuilderCodec<PlayerNpcPageData> = BuilderCodec.builder(
                PlayerNpcPageData::class.java, { PlayerNpcPageData() })
                .append(KeyedCodec("Action", Codec.STRING), { data, s -> data.action = s }, { it.action ?: "" }).add()
                .build()
        }
    }
}
