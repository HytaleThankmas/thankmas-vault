package dev.rm20.thankmasvault.shop.fishing

import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.universe.PlayerRef
import dev.rm20.anglersalmanac.Inventory.FishBagComponent


object FishBagUIHandler {

    fun openFishMarket(player: Player) {
        val accessor = player.getReference()!!.getStore()
        FishBagComponent.ensurePlayerHasFishBag(player)
        val fishBag =
            accessor.getComponent(player.getReference()!!, FishBagComponent.getComponentType())
        if (fishBag != null) {
            val playerRef = accessor.getComponent(player.getReference()!!, PlayerRef.getComponentType())
            if (playerRef != null) {
                val page = FishBagPage(playerRef)
                player.pageManager.openCustomPage(player.getReference()!!, accessor, page)
            }
        }
    }
}