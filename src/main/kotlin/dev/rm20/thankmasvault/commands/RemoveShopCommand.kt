package dev.rm20.thankmasvault.commands

import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes
import com.hypixel.hytale.server.core.command.system.arguments.types.EntityWrappedArg
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractWorldCommand
import com.hypixel.hytale.server.core.permissions.provider.HytalePermissionsProvider
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import dev.rm20.thankmasvault.shop.shopKepper.ShopComponent
import javax.annotation.Nonnull

class RemoveShopCommand : AbstractWorldCommand("removeshop", "Removes the attached shop from the targeted entity") {

    private val entityArg: EntityWrappedArg =
        withOptionalArg("entity", "The entity to remove the shop from", ArgTypes.ENTITY_ID)

    init {
        addAliases("detachshop", "clearshop")
        setPermissionGroups(HytalePermissionsProvider.GROUP_ADMIN)
    }

    override fun execute(
        @Nonnull context: CommandContext, @Nonnull world: World, @Nonnull store: Store<EntityStore>
    ) {
        val targetRef = entityArg.get(store, context)
        if (targetRef == null || !targetRef.isValid) {
            context.sendMessage(Message.raw("No valid entity found in view."))
            return
        }

        val removed = store.removeComponentIfExists(targetRef, ShopComponent.componentType)
        if (removed) {
            context.sendMessage(Message.raw("Successfully removed shop"))
        } else {
            context.sendMessage(Message.raw("Target entity does not have an shop"))
        }
    }
}
