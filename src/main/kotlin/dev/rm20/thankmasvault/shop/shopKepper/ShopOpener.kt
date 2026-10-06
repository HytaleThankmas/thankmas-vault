package dev.rm20.thankmasvault.shop.shopKepper

import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import dev.rm20.thankmasvault.shop.ShopAsset
import dev.rm20.thankmasvault.shop.ShopPage
import dev.rm20.thankmasvault.utils.CameraUtil.setFixedCameraFromPoint

object ShopOpener {

    fun openShop(
        playerRef: PlayerRef,
        targetEntityRef: Ref<EntityStore>,
        store: Store<EntityStore>,
        shopAsset: ShopAsset,
        pointId: String? = null
    ) {
        val ref = playerRef.reference ?: return

        val player = store.getComponent(ref, Player.getComponentType()) ?: return

        // camera control
        if (!pointId.isNullOrBlank()) {
            setFixedCameraFromPoint(playerRef, pointId)
        }

        // Open shop
        val page = ShopPage(playerRef, shopAsset)
        player.pageManager.openCustomPage(ref, store, page)
    }
}