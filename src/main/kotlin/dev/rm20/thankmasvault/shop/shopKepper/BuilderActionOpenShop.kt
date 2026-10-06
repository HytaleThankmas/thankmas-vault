package dev.rm20.thankmasvault.shop.shopKepper

import com.google.gson.JsonElement
import com.hypixel.hytale.server.npc.asset.builder.Builder
import com.hypixel.hytale.server.npc.asset.builder.BuilderDescriptorState
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport
import com.hypixel.hytale.server.npc.asset.builder.InstructionType
import com.hypixel.hytale.server.npc.corecomponents.builders.BuilderActionBase
import com.hypixel.hytale.server.npc.instructions.Action
import java.util.*
import javax.annotation.Nonnull


class BuilderActionOpenShop : BuilderActionBase() {

    override fun getShortDescription(): String = "Open the custom shop UI for the interacting player"

    override fun getLongDescription(): String = getShortDescription()

    override fun getBuilderDescriptorState(): BuilderDescriptorState = BuilderDescriptorState.Stable

    override fun build(@Nonnull support: BuilderSupport): Action = ActionOpenShop(this, support)

    override fun readConfig(data: JsonElement?): Builder<Action> {
        this.requireInstructionType(EnumSet.of(InstructionType.Interaction))
        return this
    }
}