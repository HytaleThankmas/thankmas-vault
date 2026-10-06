package dev.rm20.thankmasvault.shop.shopKepper

import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport
import com.hypixel.hytale.server.npc.corecomponents.ActionBase
import com.hypixel.hytale.server.npc.instructions.ExecutionSupport
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider
import dev.rm20.thankmasvault.shop.ShopAsset
import javax.annotation.Nonnull
import javax.annotation.Nullable

class ActionOpenShop(
    builder: BuilderActionOpenShop, support: BuilderSupport
) : ActionBase(builder) {


    override fun canExecute(
        @Nonnull ref: Ref<EntityStore>,
        @Nonnull executionSupport: ExecutionSupport,
        @Nullable sensorInfo: InfoProvider?,
        dt: Double,
        @Nonnull store: Store<EntityStore>
    ): Boolean {
        return super.canExecute(
            ref,
            executionSupport,
            sensorInfo,
            dt,
            store
        ) && executionSupport.stateSupport.interactionIterationTarget != null
    }


    override fun execute(
        @Nonnull ref: Ref<EntityStore>,
        @Nonnull executionSupport: ExecutionSupport,
        @Nullable sensorInfo: InfoProvider?,
        dt: Double,
        @Nonnull store: Store<EntityStore>
    ): Boolean {
        super.execute(ref, executionSupport, sensorInfo, dt, store)

        val targetRef = executionSupport.stateSupport.interactionIterationTarget ?: return false
        val playerRef = store.getComponent(targetRef, PlayerRef.getComponentType()) ?: return false

        val shopComponent = store.getComponent(ref, ShopComponent.componentType) ?: return false

        if (shopComponent.shopId.isEmpty()) return false
        val shopAsset = ShopAsset.getById(shopComponent.shopId) ?: return false

        ShopOpener.openShop(playerRef, targetRef, store, shopAsset, shopComponent.pointId)
        return true
    }
}