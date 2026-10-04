package dev.rm20.thankmasvault.economy

import com.hypixel.hytale.codec.builder.BuilderCodec
import com.hypixel.hytale.component.Component
import com.hypixel.hytale.component.ComponentType
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import dev.rm20.codecannotation.Annotations.CodecAnnotations
import dev.rm20.codecannotation.AutoCodecBuilder
import java.util.concurrent.atomic.AtomicBoolean

class WalletComponent(initialBalance: Int = 0) : Component<EntityStore> {

    @field:CodecAnnotations.Field("Balance")
    var balance: Int = initialBalance
        private set

    val isDirty: AtomicBoolean = AtomicBoolean(false)

    fun addCoins(amount: Int): Boolean {
        if (amount <= 0) return false
        balance += amount
        isDirty.set(true)
        return true
    }

    fun removeCoins(amount: Int): Boolean {
        if (amount <= 0 || balance < amount) return false
        balance -= amount
        isDirty.set(true)
        return true
    }

    fun setCoins(amount: Int) {
        if (amount >= 0 && balance != amount) {
            balance = amount
            isDirty.set(true)
        }
    }

    override fun clone(): Component<EntityStore> {
        val copy = WalletComponent(balance)
        copy.isDirty.set(isDirty.get())
        return copy
    }

    companion object {
        @JvmStatic
        lateinit var componentType: ComponentType<EntityStore, WalletComponent>

        @JvmField
        val CODEC: BuilderCodec<WalletComponent> = AutoCodecBuilder.create(
            WalletComponent::class.java
        ) { WalletComponent() }
    }
}