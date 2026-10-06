package dev.rm20.thankmasvault.registration

import dev.rm20.thankmasvault.ThankmasVault
import dev.rm20.thankmasvault.commands.*

object CommandRegister {

    fun registerCommands(plugin: ThankmasVault) {
        val commandRegistry = plugin.commandRegistry
        commandRegistry.registerCommand(BalanceCommand())
        commandRegistry.registerCommand(AdminEconomyCommand())
        commandRegistry.registerCommand(ShopCommand())
        commandRegistry.registerCommand(AttachShopCommand())
        commandRegistry.registerCommand(RemoveShopCommand())
    }
}