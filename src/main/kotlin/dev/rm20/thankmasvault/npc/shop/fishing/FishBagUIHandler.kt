package dev.rm20.thankmasvault.npc.shop.fishing

import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import dev.rm20.anglersalmanac.Inventory.FishBagComponent
import dev.rm20.thankmasvault.ThankmasVault
import dev.rm20.thankmasvault.npc.VaultNpcComponent
import dev.rm20.thankmasvault.utils.CameraUtil.setFixedCameraFromPoint


object FishBagUIHandler {

    fun openFishMarket(player: Player, npcRef: Ref<EntityStore>?) {
        val accessor = player.getReference()!!.getStore()
        FishBagComponent.ensurePlayerHasFishBag(player)
        val fishBag =
            accessor.getComponent(player.getReference()!!, FishBagComponent.getComponentType())
        if (fishBag != null) {
            val playerRef = accessor.getComponent(player.getReference()!!, PlayerRef.getComponentType())

            if (playerRef != null) {
                if(npcRef != null) {
                    val vaultNpcComponent = npcRef.store.getComponent(npcRef, VaultNpcComponent.componentType)
                    if(vaultNpcComponent != null) {
                        val pointId = vaultNpcComponent.pointId

                        if (pointId.isNotBlank()) {
                            setFixedCameraFromPoint(playerRef, pointId)
                        }

                    }
                    else
                    {
                        ThankmasVault.LOGGER.atWarning().log("Not a npc")
                    }
                }

                val page = FishBagPage(playerRef)
                player.pageManager.openCustomPage(player.getReference()!!, accessor, page)
            }
        }
    }
}