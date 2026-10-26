package dev.rm20.thankmasvault.npc.shop.shopKepper

import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.protocol.SoundCategory
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.SoundUtil
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import dev.rm20.thankmasvault.npc.shop.ShopPage
import dev.rm20.thankmasvault.assets.VaultShopAsset
import dev.rm20.thankmasvault.npc.VaultNpcComponent
import dev.rm20.thankmasvault.npc.shop.shopKepper.voiceline.voicelineInfo
import dev.rm20.thankmasvault.utils.CameraUtil.setFixedCameraFromPoint
import javax.annotation.Nonnull

object ShopOpener {

    fun openShop(
        playerRef: PlayerRef,
        targetEntityRef: Ref<EntityStore>,
        store: Store<EntityStore>,
        vaultShopAsset: VaultShopAsset,
        pointId: String? = null,
        voiceLine: voicelineInfo? = null
    ) {
        val ref = playerRef.reference ?: return

        val player = store.getComponent(ref, Player.getComponentType()) ?: return

        // camera control
        if (!pointId.isNullOrBlank()) {
            setFixedCameraFromPoint(playerRef, pointId)
        }

        // Open shop
        val page = ShopPage(playerRef, vaultShopAsset,targetEntityRef, voiceLine)
        player.pageManager.openCustomPage(ref, store, page)
    }


    fun openShop(
        @Nonnull playerRef: PlayerRef,
        @Nonnull targetRef: Ref<EntityStore>,
    ): Boolean {


        val vaultNpcComponent = targetRef.store.getComponent(targetRef, VaultNpcComponent.componentType) ?: return false

        if (vaultNpcComponent.shopId.isEmpty()) return false
        val vaultShopAsset = VaultShopAsset.getById(vaultNpcComponent.shopId) ?: return false


        // TODO: fix overlapping when spamming open shop
        var voiceLine: voicelineInfo? = null
        if (vaultShopAsset.shopOpenVoicelLine.isNotEmpty()) {
            voiceLine = vaultShopAsset.shopOpenVoicelLine.random()
            val soundIndex = SoundEvent.getAssetMap().getIndex(voiceLine.soundEventId)
            if (soundIndex != SoundEvent.EMPTY_ID) {
                SoundUtil.playSoundEvent2dToPlayer(playerRef, soundIndex, SoundCategory.Voice)
            }
        }

        openShop(playerRef, targetRef, targetRef.store, vaultShopAsset, vaultNpcComponent.pointId, voiceLine)
        return true
    }
}