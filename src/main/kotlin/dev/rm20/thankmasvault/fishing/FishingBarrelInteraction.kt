package dev.rm20.thankmasvault.fishing

import com.hypixel.hytale.codec.builder.BuilderCodec
import com.hypixel.hytale.component.CommandBuffer
import com.hypixel.hytale.protocol.InteractionType
import com.hypixel.hytale.protocol.WaitForDataFrom
import com.hypixel.hytale.server.core.entity.InteractionContext
import com.hypixel.hytale.server.core.inventory.ItemStack
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import org.joml.Vector3i
import javax.annotation.Nonnull
import javax.annotation.Nullable

class FishingBarrelInteraction(id: String) : SimpleBlockInteraction(id) {

    constructor() : this("FishingBarrelInteraction")

    @Nonnull
    @Override
    override fun getWaitForDataFrom(): WaitForDataFrom {
        return WaitForDataFrom.Server
    }

    override fun interactWithBlock(
        @Nonnull world: World,
        @Nonnull commandBuffer: CommandBuffer<EntityStore>,
        @Nonnull type: InteractionType,
        @Nonnull context: InteractionContext,
        @Nullable itemInHand: ItemStack?,
        @Nonnull targetBlock: Vector3i,
        @Nonnull cooldownHandler: CooldownHandler
    ) {
        val playerEntityRef = context.entity
        FishingRodService.handleBarrelInteract(playerEntityRef, commandBuffer.store)
    }

    override fun simulateInteractWithBlock(
        @Nonnull type: InteractionType,
        @Nonnull context: InteractionContext,
        @Nullable itemInHand: ItemStack?,
        @Nonnull world: World,
        @Nonnull targetBlock: Vector3i
    ) {
        context.state.state = context.serverState.state
    }

    companion object {
        @JvmField
        val CODEC: BuilderCodec<FishingBarrelInteraction> = BuilderCodec.builder(
            FishingBarrelInteraction::class.java,
            ::FishingBarrelInteraction,
            SimpleBlockInteraction.CODEC
        ).build()
    }
}
