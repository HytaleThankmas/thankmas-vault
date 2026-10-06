package dev.rm20.thankmasvault.commands

import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes
import com.hypixel.hytale.server.core.command.system.arguments.types.EntityWrappedArg
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractWorldCommand
import com.hypixel.hytale.server.core.permissions.provider.HytalePermissionsProvider
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.hypixel.hytale.server.npc.entities.NPCEntity
import com.hypixel.hytale.server.npc.util.IAnnotatedComponent
import com.hypixel.hytale.server.npc.util.IAnnotatedComponentCollection
import dev.rm20.thankmasvault.shop.VaultShopAsset
import dev.rm20.thankmasvault.shop.shopKepper.ActionOpenShop
import dev.rm20.thankmasvault.shop.shopKepper.ShopComponent
import javax.annotation.Nonnull

class AttachShopCommand : AbstractWorldCommand("attachshop", "Attaches a shop to the targeted entity") {

    private val shopIdArg = withRequiredArg("shopId", "The ID of the shop to attach", ArgTypes.STRING)
    private val pointIdArg = withOptionalArg("pointId", "The optional point ID to attach", ArgTypes.STRING)
    private val entityArg: EntityWrappedArg = withOptionalArg("entity", "The entity to attach the shop to", ArgTypes.ENTITY_ID)

    init {
        addAliases("setshop")
        setPermissionGroups(HytalePermissionsProvider.GROUP_ADMIN)
    }

    override fun execute(
        @Nonnull context: CommandContext, @Nonnull world: World, @Nonnull store: Store<EntityStore>
    ) {
        val shopId = shopIdArg.get(context)
        val pointId = if (pointIdArg.provided(context)) pointIdArg.get(context) else null
        val vaultShopAsset = VaultShopAsset.getById(shopId)

        if (vaultShopAsset == null) {
            context.sendMessage(
                Message.raw("Shop '$shopId' not found")
            )
            return
        }

        val targetRef = entityArg.get(store, context)
        if (targetRef == null || !targetRef.isValid) {
            context.sendMessage(Message.raw("No valid entity"))
            return
        }

        if (!hasOpenVaultShopAction(targetRef, store)) {
            context.sendMessage(
                Message.raw("Cannot attach shop: entity does not have 'OpenVaultShop' action")
            )
            return
        }

        var shopComp = store.getComponent(targetRef, ShopComponent.componentType)
        if (shopComp == null) {
            shopComp = ShopComponent()
        }

        shopComp.shopId = shopId
        if (pointId != null) {
            shopComp.pointId = pointId
        }

        store.putComponent(targetRef, ShopComponent.componentType, shopComp)
        val pointMsg = if (!pointId.isNullOrEmpty()) " and point '$pointId'" else ""

        context.sendMessage(
            Message.raw("Successfully attached shop '$shopId'$pointMsg.")
        )
    }

    // used to check if an entity has the shop action
    private fun hasOpenVaultShopAction(targetRef: Ref<EntityStore>, store: Store<EntityStore>): Boolean {
        val npcType = NPCEntity.getComponentType() ?: return false
        val npc = store.getComponent(targetRef, npcType) ?: return false
        val role = npc.role ?: return false

        val interactionInstruction = role.interactionInstruction
        if (interactionInstruction != null && containsAction(interactionInstruction, ActionOpenShop::class.java)) {
            return true
        }

        val rootInstruction = role.rootInstruction
        return rootInstruction != null && containsAction(rootInstruction, ActionOpenShop::class.java)
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