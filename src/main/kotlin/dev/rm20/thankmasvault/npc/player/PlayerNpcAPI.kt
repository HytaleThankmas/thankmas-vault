package dev.rm20.thankmasvault.npc.player

import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.Universe
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import dev.rm20.thankmasvault.assets.PlayerNpcAsset
import dev.rm20.thankmasvault.assets.PlayerNpcEntry
import java.util.*


object PlayerNpcAPI {

    const val PLAYER_ICON_DIR: String = "UI/Custom/Delivery/ModelsGenerated/"
    const val DEFAULT_FALLBACK_MASK: String = "NPC/HiddenPlayerMask.png"

    @JvmStatic
    fun getAllPlayerNpcs(): List<PlayerNpcEntry> {
        return PlayerNpcAsset.getAllPlayerNpcs()
    }

    @JvmStatic
    fun getAllPlayerNpcNames(): List<String> {
        return getAllPlayerNpcs().map { it.name }
    }

    @JvmStatic
    fun isAssetPlayerNpc(npcIdOrName: String): Boolean {
        val normalized = PlayerFoundNpcs.normalize(npcIdOrName)
        return getAllPlayerNpcs().any { PlayerFoundNpcs.normalize(it.name) == normalized }
    }


    @JvmStatic
    fun hasInteracted(playerRef: PlayerRef, npcIdOrName: String): Boolean {
        val ref = playerRef.reference ?: return false
        val store = ref.store
        return hasInteracted(playerRef, store, npcIdOrName)
    }

    @JvmStatic
    fun hasInteracted(playerRef: PlayerRef, store: Store<EntityStore>, npcIdOrName: String): Boolean {
        val ref = playerRef.reference ?: return false
        val foundComp = store.getComponent(ref, PlayerFoundNpcs.componentType) ?: return false
        return foundComp.has(npcIdOrName)
    }

    @JvmStatic
    fun hasInteracted(playerUuid: UUID, npcIdOrName: String): Boolean {
        val player = try {
            Universe.get().getPlayer(playerUuid)
        } catch (_: Throwable) {
            null
        } ?: return false

        val ref = player.reference ?: return false
        val store = ref.store
        val foundComp = store.getComponent(ref, PlayerFoundNpcs.componentType) ?: return false
        return foundComp.has(npcIdOrName)
    }

    @JvmStatic
    fun getFoundNpcs(playerRef: PlayerRef): Set<String> {
        val ref = playerRef.reference ?: return emptySet()
        val store = ref.store
        return getFoundNpcs(playerRef, store)
    }

    @JvmStatic
    fun getFoundNpcs(playerRef: PlayerRef, store: Store<EntityStore>): Set<String> {
        val ref = playerRef.reference ?: return emptySet()
        val foundComp = store.getComponent(ref, PlayerFoundNpcs.componentType) ?: return emptySet()
        return foundComp.getFound()
    }


    @JvmStatic
    fun getFoundCount(playerRef: PlayerRef, store: Store<EntityStore>? = null): Int {
        val ref = playerRef.reference ?: return 0
        val targetStore = store ?: ref.store
        val foundComp = targetStore.getComponent(ref, PlayerFoundNpcs.componentType) ?: return 0
        val assetNpcs = getAllPlayerNpcNames().map { PlayerFoundNpcs.normalize(it) }.toSet()
        return foundComp.getFound().count { assetNpcs.contains(it) }
    }


    @JvmStatic
    fun recordInteraction(
        playerRef: PlayerRef, store: Store<EntityStore>, npcIdOrName: String, displayName: String? = null
    ): Boolean {
        val ref = playerRef.reference ?: return false
        if (!ref.isValid) return false
        val foundComp = store.ensureAndGetComponent(ref, PlayerFoundNpcs.componentType)
        val added = foundComp.add(npcIdOrName)
        if (!displayName.isNullOrBlank()) {
            foundComp.add(displayName)
        }
        return added
    }

    @JvmStatic
    fun resolvePlayerIconPath(npcName: String): String {
        val clean = npcName.trim().removeSuffix(".png")
        return "$PLAYER_ICON_DIR$clean.png"
    }
}
