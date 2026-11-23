package dev.rm20.thankmasvault.npc.player

import com.hypixel.hytale.codec.builder.BuilderCodec
import com.hypixel.hytale.component.Component
import com.hypixel.hytale.component.ComponentType
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import gg.ginco.jellyparty.codec.annotations.SerialName
import gg.ginco.jellyparty.codec.annotations.SerializableObject
import java.util.*

@SerializableObject
class PlayerFoundNpcs : Component<EntityStore> {

    @SerialName("Found")
    var found: List<String> = emptyList()

    fun add(npcIdOrName: String): Boolean {
        if (npcIdOrName.isBlank()) return false
        val normalized = normalize(npcIdOrName)
        if (found.contains(normalized)) return false
        found = found + normalized
        return true
    }

    /**
     * Checks if this NPC has already been discovered by the player.
     */
    fun has(npcIdOrName: String): Boolean {
        if (npcIdOrName.isBlank()) return false
        return found.contains(normalize(npcIdOrName))
    }

    fun count(): Int = found.size

    fun getFound(): Set<String> = Collections.unmodifiableSet(found.toSet())

    fun clear() {
        found = emptyList()
    }

    override fun clone(): Component<EntityStore> {
        val copy = PlayerFoundNpcs()
        copy.found = ArrayList(found)
        return copy
    }

    companion object {
        @JvmStatic
        lateinit var componentType: ComponentType<EntityStore, PlayerFoundNpcs>

        @JvmField
        val CODEC: BuilderCodec<PlayerFoundNpcs> = PlayerFoundNpcsCodec

        @JvmStatic
        fun normalize(id: String): String {
            return id.trim().lowercase().replace(" ", "").replace("_", "").replace("-", "")
        }
    }
}
