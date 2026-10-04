package dev.rm20.thankmasvault.economy

import com.hypixel.hytale.component.ComponentAccessor
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore

object EconomyAPI {
    @JvmStatic
    fun ensureWallet(ref: Ref<EntityStore>, accessor: ComponentAccessor<EntityStore>, initialBalance: Int = 0) {
        if (accessor.getComponent(ref, WalletComponent.componentType) == null) {
            accessor.putComponent(
                ref,
                WalletComponent.componentType,
                WalletComponent(initialBalance)
            )
        }
    }

    @JvmStatic
    fun getBalance(ref: Ref<EntityStore>, accessor: ComponentAccessor<EntityStore>): Int {
        ensureWallet(ref, accessor)
        val wallet = accessor.getComponent(ref, WalletComponent.componentType) ?: return 0
        return wallet.balance
    }

    @JvmStatic
    fun deposit(ref: Ref<EntityStore>, accessor: ComponentAccessor<EntityStore>, amount: Int): Boolean {
        ensureWallet(ref, accessor)
        val wallet = accessor.getComponent(ref, WalletComponent.componentType) ?: return false
        val success = wallet.addCoins(amount)
        if (success) {
            syncWalletUI(ref, accessor, wallet)
        }
        return success
    }

    @JvmStatic
    fun withdraw(ref: Ref<EntityStore>, accessor: ComponentAccessor<EntityStore>, amount: Int): Boolean {
        ensureWallet(ref, accessor)
        val wallet = accessor.getComponent(ref, WalletComponent.componentType) ?: return false
        val success = wallet.removeCoins(amount)
        if (success) {
            syncWalletUI(ref, accessor, wallet)
        }
        return success
    }

    @JvmStatic
    fun hasEnough(ref: Ref<EntityStore>, accessor: ComponentAccessor<EntityStore>, amount: Int): Boolean {
        return getBalance(ref, accessor) >= amount
    }

    private fun syncWalletUI(ref: Ref<EntityStore>, accessor: ComponentAccessor<EntityStore>, wallet: WalletComponent) {
        if (wallet.isDirty.compareAndSet(true, false)) {
            accessor.putComponent(ref, WalletComponent.componentType, wallet)
        }
    }
}

fun Ref<EntityStore>.ensureWallet(accessor: ComponentAccessor<EntityStore>, initialBalance: Int = 0) = EconomyAPI.ensureWallet(this, accessor, initialBalance)
fun Ref<EntityStore>.getCoins(accessor: ComponentAccessor<EntityStore>): Int = EconomyAPI.getBalance(this, accessor)
fun Ref<EntityStore>.addCoins(accessor: ComponentAccessor<EntityStore>, amount: Int): Boolean = EconomyAPI.deposit(this, accessor, amount)
fun Ref<EntityStore>.takeCoins(accessor: ComponentAccessor<EntityStore>, amount: Int): Boolean = EconomyAPI.withdraw(this, accessor, amount)