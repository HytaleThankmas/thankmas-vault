package dev.rm20.thankmasvault.npc.player

import com.hypixel.hytale.codec.Codec
import com.hypixel.hytale.codec.KeyedCodec
import com.hypixel.hytale.codec.builder.BuilderCodec
import com.hypixel.hytale.component.Component
import com.hypixel.hytale.component.ComponentType
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import java.util.*


class PlayerFoundNpcs : Component<EntityStore> {

    private var found: MutableSet<String> = LinkedHashSet()


    fun add(npcIdOrName: String): Boolean {
        if (npcIdOrName.isBlank()) return false
        return found.add(normalize(npcIdOrName))
    }

    /**
     * Checks if this NPC has already been discovered by the player.
     */
    fun has(npcIdOrName: String): Boolean {
        if (npcIdOrName.isBlank()) return false
        return found.contains(normalize(npcIdOrName))
    }

    fun count(): Int = found.size

    fun getFound(): Set<String> = Collections.unmodifiableSet(found)

    fun clear() {
        found.clear()
    }

    override fun clone(): Component<EntityStore> {
        val copy = PlayerFoundNpcs()
        copy.found = LinkedHashSet(found)
        return copy
    }

    companion object {
        @JvmStatic
        lateinit var componentType: ComponentType<EntityStore, PlayerFoundNpcs>

        @JvmField
        val CODEC: BuilderCodec<PlayerFoundNpcs> = BuilderCodec.builder(
            PlayerFoundNpcs::class.java,
            { PlayerFoundNpcs() }
        )
            .append(
                KeyedCodec("Found", Codec.STRING_ARRAY),
                { c, arr -> c.found = LinkedHashSet(arr.toList()) },
                { c -> c.found.toTypedArray() }
            )
            .add()
            .build()

        @JvmStatic
        fun normalize(id: String): String {
            return id.trim().lowercase().replace(" ", "").replace("_", "").replace("-", "")
        }
    }
}
