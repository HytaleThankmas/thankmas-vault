package dev.rm20.thankmasvault.commands

import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import dev.rm20.thankmasvault.economy.WalletComponent
import javax.annotation.Nonnull

class BalanceCommand : AbstractPlayerCommand("balance", "Check your current coin balance") {
    init {
        addAliases("bal", "coins")
    }
    override fun execute(
        @Nonnull context: CommandContext,
        @Nonnull store: Store<EntityStore>,
        @Nonnull ref: Ref<EntityStore>,
        @Nonnull playerRef: PlayerRef,
        @Nonnull world: World
    ) {
        val wallet = store.getComponent(ref, WalletComponent.componentType) ?: WalletComponent(0)

        context.sendMessage(
            Message.raw("[Economy] You currently have ${wallet.balance} coins.")
        )
    }
}