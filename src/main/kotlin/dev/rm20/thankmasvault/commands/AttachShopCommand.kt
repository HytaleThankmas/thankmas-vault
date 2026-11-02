package dev.rm20.thankmasvault.commands

import acorn.thankmasdelivery.component.DeliveryNpc
import acorn.thankmasdelivery.npc.DeliveryNpcActionBuilder
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes
import com.hypixel.hytale.server.core.command.system.arguments.types.EntityWrappedArg
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.permissions.provider.HytalePermissionsProvider
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.hypixel.hytale.server.npc.entities.NPCEntity
import com.hypixel.hytale.server.npc.util.IAnnotatedComponent
import com.hypixel.hytale.server.npc.util.IAnnotatedComponentCollection
import dev.rm20.thankmasvault.npc.VaultNpcPage
import dev.rm20.thankmasvault.npc.dialogue.ActionOpenDialogue
import dev.rm20.thankmasvault.npc.shop.shopKepper.ActionOpenShop
import javax.annotation.Nonnull

class AttachShopCommand : AbstractPlayerCommand("attachshop", "Configure Vault NPC settings for the targeted entity via UI") {

    private val entityArg: EntityWrappedArg = withOptionalArg("entity", "The entity to configure", ArgTypes.ENTITY_ID)

    init {
        addAliases("setshop", "editnpc", "vaultnpc")
        setPermissionGroups(HytalePermissionsProvider.GROUP_ADMIN)
    }

    override fun execute(
        @Nonnull context: CommandContext,
        @Nonnull store: Store<EntityStore>,
        @Nonnull ref: Ref<EntityStore>,
        @Nonnull playerRef: PlayerRef,
        @Nonnull world: World
    ) {
        val targetRef = entityArg.get(store, context)
        if (targetRef == null || !targetRef.isValid) {
            context.sendMessage(Message.raw("No valid entity found in view."))
            return
        }

        if (!hasVaultNpcAction(targetRef, store)) {
            context.sendMessage(
                Message.raw("Cannot configure entity: entity is not an NPC or does not have 'OpenVaultShop', 'OpenDialogue', or 'DeliveryNpc'")
            )
            return
        }

        val player = store.getComponent(ref, Player.getComponentType())
        if (player == null) {
            context.sendMessage(Message.raw("Player component not found."))
            return
        }

        val page = VaultNpcPage(playerRef, targetRef)
        player.pageManager.openCustomPage(ref, store, page)
    }

    // used to check if an entity is an NPC or has the shop/dialogue/delivery action
    private fun hasVaultNpcAction(targetRef: Ref<EntityStore>, store: Store<EntityStore>): Boolean {
        val deliveryNpcType = DeliveryNpc.getComponentType()
        if (deliveryNpcType != null && store.getComponent(targetRef, deliveryNpcType) != null) {
            return true
        }

        val npcType = NPCEntity.getComponentType() ?: return false
        val npc = store.getComponent(targetRef, npcType) ?: return false
        val role = npc.role ?: return true

        val interactionInstruction = role.interactionInstruction
        if (interactionInstruction != null && (
            containsAction(interactionInstruction, ActionOpenShop::class.java) ||
            containsAction(interactionInstruction, ActionOpenDialogue::class.java) ||
            containsAction(interactionInstruction, DeliveryNpcActionBuilder::class.java)
        )) {
            return true
        }

        val rootInstruction = role.rootInstruction
        if (rootInstruction != null && (
            containsAction(rootInstruction, ActionOpenShop::class.java) ||
            containsAction(rootInstruction, ActionOpenDialogue::class.java) ||
            containsAction(rootInstruction, DeliveryNpcActionBuilder::class.java)
        )) {
            return true
        }

        return true
    }

    private fun containsAction(component: IAnnotatedComponent, actionClass: Class<*>): Boolean {
        if (actionClass.isInstance(component)) {
            return true
        }

        if (component is IAnnotatedComponentCollection) {
            for (i in 0 until component.componentCount()) {
                val child = component.getComponent(i) ?: continue
                if (containsAction(child, actionClass)) {
                    return true
                }
            }
        }

        return false
    }
}
