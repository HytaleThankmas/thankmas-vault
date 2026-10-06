package dev.rm20.thankmasvault.commands

import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import dev.rm20.thankmasvault.shop.VaultShopAsset
import dev.rm20.thankmasvault.shop.ShopPage
import javax.annotation.Nonnull

// TODO: remove this and use NPC
class ShopCommand : AbstractPlayerCommand("shop", "Open a shop interface") {

    private val shopArg = withOptionalArg("shop", "ID of the shop to open", ArgTypes.STRING)

    override fun execute(
        @Nonnull context: CommandContext,
        @Nonnull store: Store<EntityStore>,
        @Nonnull ref: Ref<EntityStore>,
        @Nonnull playerRef: PlayerRef,
        @Nonnull world: World
    ) {
        val player = store.getComponent(ref, Player.getComponentType())
        if (player == null) {
            return
        }

        val availableShops = VaultShopAsset.getAssetMap().assetMap.keys
        if (availableShops.isEmpty()) {
            context.sendMessage(Message.raw("No shops available."))
            return
        }

        val shopId = if (shopArg.provided(context)) {
            shopArg.get(context)
        } else {
            if (availableShops.size == 1) {
                availableShops.first()
            } else {
                context.sendMessage(
                    Message.raw("No shop")
                )
                return
            }
        }

        val vaultShopAsset = VaultShopAsset.getById(shopId)
        if (vaultShopAsset == null) {
            context.sendMessage(
                Message.raw("'$shopId' not found")
            )
            return
        }

        val page = ShopPage(playerRef, vaultShopAsset)
        player.pageManager.openCustomPage(ref, store, page)
    }
}
