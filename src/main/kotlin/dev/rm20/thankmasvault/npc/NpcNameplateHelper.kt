package dev.rm20.thankmasvault.npc

import com.hypixel.hytale.component.ComponentAccessor
import com.hypixel.hytale.component.Holder
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.hypixel.hytale.server.npc.role.support.DisplayNameSupport
import java.awt.Color

object NpcNameplateHelper {

    fun formatNameplateText(name: String): String {
        return if (name.startsWith("[NPC] ")) name else "[NPC] $name"
    }

    fun applyNameplate(holder: Holder<EntityStore>, name: String) {
        if (name.isBlank()) return
        DisplayNameSupport.setDisplayName(holder, name)
        val nameplate = holder.ensureAndGetComponent(Nameplate.getComponentType())
        nameplate.setText(formatNameplateText(name))
//        val colour = Color.yellow
//        nameplate.setColor(colour)
    }

    fun applyNameplate(ref: Ref<EntityStore>, name: String, accessor: ComponentAccessor<EntityStore>) {
        if (name.isBlank() || !ref.isValid) return
        DisplayNameSupport.setDisplayName(ref, name, accessor)
        val nameplate = accessor.ensureAndGetComponent(ref, Nameplate.getComponentType())
        nameplate.setText(formatNameplateText(name))
    }
}
