package dev.rm20.thankmasvault.registration

import com.hypixel.hytale.server.core.entity.entities.Player
import dev.rm20.thankmasvault.npc.dialogue.DialogueActionRegistry
import dev.rm20.thankmasvault.npc.shop.fishing.FishBagUIHandler.openFishMarket
import dev.rm20.thankmasvault.npc.shop.shopKepper.ShopOpener.openShop
import dev.rm20.anglersalmanac.Inventory.FishBagUIHandler

object DialogueActionsRegister {

    fun registerDialogueActions() {
        DialogueActionRegistry.register("openShop") { playerRef, npcRef, _ ->
            openShop(playerRef, npcRef)
        }

        DialogueActionRegistry.register("fishMarket") { playerRef, npcRef, _ ->
            if(playerRef.reference == null) return@register
            val player = playerRef.reference!!.store.getComponent(playerRef.reference!!, Player.getComponentType())
            if(player != null) openFishMarket(player, npcRef)
        }

        DialogueActionRegistry.register("fishbag") { playerRef, npcRef, _ ->
            if(playerRef.reference == null) return@register
            val player = playerRef.reference!!.store.getComponent(playerRef.reference!!, Player.getComponentType())
            FishBagUIHandler.openFishBag(player)
        }
    }



}