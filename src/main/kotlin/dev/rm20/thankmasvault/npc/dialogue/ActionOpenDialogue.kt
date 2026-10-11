package dev.rm20.thankmasvault.npc.dialogue

import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport
import com.hypixel.hytale.server.npc.corecomponents.ActionBase
import com.hypixel.hytale.server.npc.instructions.ExecutionSupport
import com.hypixel.hytale.server.npc.sensorinfo.InfoProvider
import dev.rm20.thankmasvault.ThankmasVault
import dev.rm20.thankmasvault.assets.NpcAsset
import dev.rm20.thankmasvault.assets.VaultShopAsset
import dev.rm20.thankmasvault.npc.VaultNpcComponent
import dev.rm20.thankmasvault.utils.CameraUtil.setFixedCameraFromPoint
import javax.annotation.Nonnull
import javax.annotation.Nullable

class ActionOpenDialogue(
    builder: BuilderActionOpenDialogue, support: BuilderSupport
) : ActionBase(builder) {


    override fun canExecute(
        @Nonnull ref: Ref<EntityStore>,
        @Nonnull executionSupport: ExecutionSupport,
        @Nullable sensorInfo: InfoProvider?,
        dt: Double,
        @Nonnull store: Store<EntityStore>
    ): Boolean {
        return super.canExecute(
            ref, executionSupport, sensorInfo, dt, store
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

        val vaultNpcComponent = store.getComponent(ref, VaultNpcComponent.componentType) ?: return false

        if (vaultNpcComponent.dialogueId.isEmpty()) return false
        val npcAsset = NpcAsset.getById(vaultNpcComponent.dialogueId) ?: return false

        val dialogue = npcAsset.dialogue ?: return false
        val pointId = vaultNpcComponent.pointId


        val page = DialoguePage(playerRef,ref, dialogue, vaultNpcComponent.displayName)
        val playerRef1 = playerRef.reference?: return false

        val player = store.getComponent(playerRef1, Player.getComponentType()) ?: return false
        if (!pointId.isNullOrBlank()) {
            setFixedCameraFromPoint(playerRef, pointId)
        }
        player.pageManager.openCustomPage(playerRef1, store, page)

        return true
    }
}