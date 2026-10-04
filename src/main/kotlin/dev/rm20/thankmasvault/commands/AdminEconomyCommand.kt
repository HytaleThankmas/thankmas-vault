package dev.rm20.thankmasvault.commands

import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.command.system.AbstractCommand
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection
import com.hypixel.hytale.server.core.permissions.provider.HytalePermissionsProvider
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.worldgen.climate.ClimateType.color
import dev.rm20.thankmasvault.economy.EconomyAPI
import dev.rm20.thankmasvault.economy.WalletComponent
import java.util.concurrent.CompletableFuture
import javax.annotation.Nonnull

class AdminEconomyCommand : AbstractCommandCollection("eco", "Admin economy management") {

    init {
        addAliases("economy")
        setPermissionGroups(HytalePermissionsProvider.GROUP_ADMIN)

        addSubCommand(AddSubCommand())
        addSubCommand(RemoveSubCommand())
        addSubCommand(SetSubCommand())
        addSubCommand(GetSubCommand())
    }

    private class AddSubCommand : AbstractCommand("add", "Add coins to a player's balance") {
        private val playerArg = withRequiredArg("player", "Target player", ArgTypes.PLAYER_REF)
        private val amountArg = withRequiredArg("amount", "Amount of coins to add", ArgTypes.INTEGER)

        init {
            setPermissionGroups(HytalePermissionsProvider.GROUP_ADMIN)
        }

        override fun execute(@Nonnull context: CommandContext): CompletableFuture<Void>? {
            val targetRef: PlayerRef = playerArg.get(context)
            val amount: Int = amountArg.get(context)

            if (amount <= 0) {
                context.sendMessage(Message.raw("[Economy] Amount must be greater than 0."))
                return CompletableFuture.completedFuture(null)
            }

            val ref = targetRef.reference
            if (ref == null || !ref.isValid) {
                context.sendMessage(Message.raw("[Economy] Player is not in a valid world."))
                return CompletableFuture.completedFuture(null)
            }

            val store = ref.store
            val world = store.externalData.world

            world.execute {
                EconomyAPI.deposit(ref, store, amount)
                val newBal = store.getComponent(ref, WalletComponent.componentType)?.balance ?: 0

                context.sendMessage(
                    Message.raw("[Economy] Added $amount coins to ${targetRef.username}. New balance: $newBal")
                )
                targetRef.sendMessage(
                    Message.raw("[Economy] +$amount coins have been added to your wallet! (Total: $newBal)")
                )
            }

            return CompletableFuture.completedFuture(null)
        }
    }


    private class RemoveSubCommand : AbstractCommand("remove", "Remove coins from a player's balance") {
        private val playerArg = withRequiredArg("player", "Target player", ArgTypes.PLAYER_REF)
        private val amountArg = withRequiredArg("amount", "Amount of coins to remove", ArgTypes.INTEGER)

        init {
            addAliases("take")
            setPermissionGroups(HytalePermissionsProvider.GROUP_ADMIN)
        }

        override fun execute(@Nonnull context: CommandContext): CompletableFuture<Void>? {
            val targetRef: PlayerRef = playerArg.get(context)
            val amount: Int = amountArg.get(context)

            if (amount <= 0) {
                context.sendMessage(Message.raw("[Economy] Amount must be greater than 0."))
                return CompletableFuture.completedFuture(null)
            }

            val ref = targetRef.reference
            if (ref == null || !ref.isValid) {
                context.sendMessage(Message.raw("[Economy] Player is not in a valid world."))
                return CompletableFuture.completedFuture(null)
            }

            val store = ref.store
            val world = store.externalData.world

            world.execute {
                val success = EconomyAPI.withdraw(ref, store, amount)
                val newBal = store.getComponent(ref, WalletComponent.componentType)?.balance ?: 0

                if (success) {
                    context.sendMessage(
                        Message.raw("[Economy] Removed $amount coins from ${targetRef.username}. New balance: $newBal")
                    )
                    targetRef.sendMessage(
                        Message.raw("[Economy] -$amount coins were deducted from your wallet! (Total: $newBal)")
                    )
                } else {
                    context.sendMessage(
                        Message.raw("[Economy] Could not remove $amount coins; ${targetRef.username} only has $newBal coins.")
                    )
                }
            }

            return CompletableFuture.completedFuture(null)
        }
    }


    private class SetSubCommand : AbstractCommand("set", "Set a player's coin balance") {
        private val playerArg = withRequiredArg("player", "Target player", ArgTypes.PLAYER_REF)
        private val amountArg = withRequiredArg("amount", "New balance amount", ArgTypes.INTEGER)

        init {
            setPermissionGroups(HytalePermissionsProvider.GROUP_ADMIN)
        }

        override fun execute(@Nonnull context: CommandContext): CompletableFuture<Void>? {
            val targetRef: PlayerRef = playerArg.get(context)
            val amount: Int = amountArg.get(context)

            if (amount < 0) {
                context.sendMessage(Message.raw("[Economy] Balance cannot be negative."))
                return CompletableFuture.completedFuture(null)
            }

            val ref = targetRef.reference
            if (ref == null || !ref.isValid) {
                context.sendMessage(Message.raw("[Economy] Player is not in a valid world."))
                return CompletableFuture.completedFuture(null)
            }

            val store = ref.store
            val world = store.externalData.world

            world.execute {
                var wallet = store.getComponent(ref, WalletComponent.componentType)
                if (wallet == null) {
                    wallet = WalletComponent(amount)
                } else {
                    wallet.setCoins(amount)
                }
                store.putComponent(ref, WalletComponent.componentType, wallet)

                context.sendMessage(
                    Message.raw("[Economy] Set ${targetRef.username}'s balance to $amount coins.")
                )
                targetRef.sendMessage(
                    Message.raw("[Economy] Your balance was set to $amount coins by an administrator.")
                )
            }

            return CompletableFuture.completedFuture(null)
        }
    }


    private class GetSubCommand : AbstractCommand("get", "View a player's coin balance") {
        private val playerArg = withRequiredArg("player", "Target player", ArgTypes.PLAYER_REF)

        init {
            addAliases("view", "bal")
            setPermissionGroups(HytalePermissionsProvider.GROUP_ADMIN)
        }

        override fun execute(@Nonnull context: CommandContext): CompletableFuture<Void>? {
            val targetRef: PlayerRef = playerArg.get(context)
            val ref = targetRef.reference
            if (ref == null || !ref.isValid) {
                context.sendMessage(Message.raw("§c[Economy] Player is not in a valid world."))
                return CompletableFuture.completedFuture(null)
            }

            val store = ref.store
            val wallet = store.getComponent(ref, WalletComponent.componentType) ?: WalletComponent(0)

            context.sendMessage(
                Message.raw("[Economy] ${targetRef.username} has ${wallet.balance} coins.")
            )

            return CompletableFuture.completedFuture(null)
        }
    }
}