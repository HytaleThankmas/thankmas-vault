package dev.rm20.thankmasvault.registration

import dev.rm20.thankmasvault.ThankmasVault
import dev.rm20.thankmasvault.commands.AdminEconomyCommand
import dev.rm20.thankmasvault.commands.BalanceCommand
import dev.rm20.thankmasvault.commands.ShopCommand

object CommandRegister {

    fun registerCommands(plugin: ThankmasVault) {
        val commandRegistry = plugin.commandRegistry
        commandRegistry.registerCommand(BalanceCommand())
        commandRegistry.registerCommand(AdminEconomyCommand())
        commandRegistry.registerCommand(ShopCommand())
    }
}