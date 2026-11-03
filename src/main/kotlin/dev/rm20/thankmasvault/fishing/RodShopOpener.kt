package dev.rm20.thankmasvault.fishing

import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import dev.rm20.thankmasvault.assets.RodProgressionAsset
import dev.rm20.thankmasvault.npc.VaultNpcComponent
import dev.rm20.thankmasvault.utils.CameraUtil.setFixedCameraFromPoint
import javax.annotation.Nonnull

object RodShopOpener {

    fun openRodShop(
        @Nonnull playerRef: PlayerRef,
        targetEntityRef: Ref<EntityStore>? = null,
        progressionAsset: RodProgressionAsset? = null
    ): Boolean {
        val ref = playerRef.reference ?: return false
        val store = ref.store
        val player = store.getComponent(ref, Player.getComponentType()) ?: return false

        val asset = progressionAsset
            ?: RodProgressionAsset.getDefault()
            ?: FishingRodService.getActiveProgressionAsset()

        if (targetEntityRef != null && targetEntityRef.isValid) {
            val vaultNpcComponent = store.getComponent(targetEntityRef, VaultNpcComponent.componentType)
            if (vaultNpcComponent != null && !vaultNpcComponent.pointId.isNullOrBlank()) {
                setFixedCameraFromPoint(playerRef, vaultNpcComponent.pointId, npcRef = targetEntityRef)
            }
        }

        val page = RodShopPage(playerRef, asset, targetEntityRef)
        player.pageManager.openCustomPage(ref, store, page)
        return true
    }
}
