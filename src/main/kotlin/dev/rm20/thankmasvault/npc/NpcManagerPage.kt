package dev.rm20.thankmasvault.npc

import acorn.thankmasdelivery.ThankmasDeliveryPlugin
import acorn.thankmasdelivery.component.DeliveryNpc
import com.hypixel.hytale.codec.Codec
import com.hypixel.hytale.codec.KeyedCodec
import com.hypixel.hytale.codec.builder.BuilderCodec
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.component.query.Query
import com.hypixel.hytale.math.vector.Rotation3f
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.entity.UUIDComponent
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate
import com.hypixel.hytale.server.core.modules.entity.component.DisplayNameComponent
import com.hypixel.hytale.server.core.modules.entity.component.PersistentDisplayName
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport
import com.hypixel.hytale.server.core.ui.builder.EventData
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.Universe
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.hypixel.hytale.server.npc.entities.NPCEntity
import com.hypixel.hytale.server.npc.util.IAnnotatedComponent
import com.hypixel.hytale.server.npc.util.IAnnotatedComponentCollection
import dev.rm20.thankmasvault.npc.player.AppearanceHelper
import dev.rm20.thankmasvault.npc.player.PlayerNpcComponent
import org.joml.Vector3d
import java.util.*
import java.util.function.BiConsumer
import javax.annotation.Nonnull

class NpcManagerPage(
    playerRef: PlayerRef
) : InteractiveCustomUIPage<NpcManagerPage.NpcListPageData>(
    playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, NpcListPageData.CODEC
) {

    data class NpcEntry(
        val entityRef: Ref<EntityStore>,
        val uuid: UUID,
        val id: String,
        val name: String,
        val type: String,
        val isDeliveryRecipient: Boolean,
        val deliveryRole: String,
        val hasShop: Boolean,
        val shopId: String,
        val position: Vector3d,
        val worldName: String
    )

    enum class SortField {
        TYPE, ID, DELIVERY, SHOP
    }

    private val allNpcs = mutableListOf<NpcEntry>()
    private val displayedNpcs = mutableListOf<NpcEntry>()
    private var currentSortField: SortField = SortField.TYPE
    private var sortAscending: Boolean = true

    override fun build(
        @Nonnull ref: Ref<EntityStore>,
        @Nonnull commandBuilder: UICommandBuilder,
        @Nonnull eventBuilder: UIEventBuilder,
        @Nonnull store: Store<EntityStore>
    ) {
        commandBuilder.append("NPC/NpcManagerPage.ui")

        val world = store.externalData.world
        collectNpcs(store, world)
        sortNpcList()

        updateHeader(commandBuilder)

        // Event bindings for sort buttons
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating, "#SortTypeButton", EventData.of("Action", "sort_type"), false
        )
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating, "#SortIdButton", EventData.of("Action", "sort_id"), false
        )
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating, "#SortDeliveryButton", EventData.of("Action", "sort_delivery"), false
        )
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating, "#SortShopButton", EventData.of("Action", "sort_shop"), false
        )
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating, "#CloseButton", EventData.of("Action", "close"), false
        )


        commandBuilder.clear("#NpcList")
        renderNpcRows(commandBuilder, eventBuilder)
    }

    override fun handleDataEvent(
        @Nonnull ref: Ref<EntityStore>, @Nonnull store: Store<EntityStore>, @Nonnull data: NpcListPageData
    ) {
        when (data.action?.lowercase()) {
            "sort_type" -> {
                if (currentSortField == SortField.TYPE) {
                    sortAscending = !sortAscending
                } else {
                    currentSortField = SortField.TYPE
                    sortAscending = true
                }
                sortNpcList()
                refreshList(store)
            }

            "sort_id" -> {
                if (currentSortField == SortField.ID) {
                    sortAscending = !sortAscending
                } else {
                    currentSortField = SortField.ID
                    sortAscending = true
                }
                sortNpcList()
                refreshList(store)
            }

            "sort_delivery" -> {
                if (currentSortField == SortField.DELIVERY) {
                    sortAscending = !sortAscending
                } else {
                    currentSortField = SortField.DELIVERY
                    sortAscending = true
                }
                sortNpcList()
                refreshList(store)
            }

            "sort_shop" -> {
                if (currentSortField == SortField.SHOP) {
                    sortAscending = !sortAscending
                } else {
                    currentSortField = SortField.SHOP
                    sortAscending = true
                }
                sortNpcList()
                refreshList(store)
            }

            "tp" -> {
                val targetIndex = data.target?.toIntOrNull() ?: return
                val targetNpc = displayedNpcs.getOrNull(targetIndex) ?: return
                val player = store.getComponent(ref, Player.getComponentType()) ?: return

                val targetPos = if (targetNpc.entityRef.isValid) {
                    val liveTransform = store.getComponent(targetNpc.entityRef, TransformComponent.getComponentType())
                    liveTransform?.position ?: targetNpc.position
                } else {
                    targetNpc.position
                }

                val playerTransform = store.getComponent(ref, TransformComponent.getComponentType())
                val rotation = playerTransform?.rotation ?: Rotation3f()

                val targetWorld = Universe.get().getWorld(targetNpc.worldName) ?: store.externalData.world
                val teleport = Teleport(targetWorld, targetPos, rotation)
                store.putComponent(ref, Teleport.getComponentType(), teleport)

                playerRef.sendMessage(
                    Message.raw(
                        "Teleported to ${targetNpc.name} [${targetNpc.type}] at ${targetPos.x.toInt()}, ${targetPos.y.toInt()}, ${targetPos.z.toInt()}"
                    )
                )
                close()
            }

            "close", "back" -> {
                close()
            }
        }
    }

    private fun refreshList(store: Store<EntityStore>) {
        val commandBuilder = UICommandBuilder()
        val eventBuilder = UIEventBuilder()

        updateHeader(commandBuilder)
        commandBuilder.clear("#NpcList")
        renderNpcRows(commandBuilder, eventBuilder)

        sendUpdate(commandBuilder, eventBuilder, false)
    }

    private fun updateHeader(commandBuilder: UICommandBuilder) {
        commandBuilder.set("#SummaryText.Text", "NPCs: ${displayedNpcs.size}")

        val typeArrow = if (currentSortField == SortField.TYPE) (if (sortAscending) " ▲" else " ▼") else ""
        val idArrow = if (currentSortField == SortField.ID) (if (sortAscending) " ▲" else " ▼") else ""
        val deliveryArrow = if (currentSortField == SortField.DELIVERY) (if (sortAscending) " ▲" else " ▼") else ""
        val shopArrow = if (currentSortField == SortField.SHOP) (if (sortAscending) " ▲" else " ▼") else ""

        commandBuilder.set("#SortTypeButton.Text", "Type$typeArrow")
        commandBuilder.set("#SortIdButton.Text", "ID$idArrow")
        commandBuilder.set("#SortDeliveryButton.Text", "Delivery$deliveryArrow")
        commandBuilder.set("#SortShopButton.Text", "Shop$shopArrow")
    }

    private fun renderNpcRows(commandBuilder: UICommandBuilder, eventBuilder: UIEventBuilder) {
        for (i in displayedNpcs.indices) {
            val npc = displayedNpcs[i]
            val selector = "#NpcList[$i]"

            commandBuilder.append("#NpcList", "NPC/NpcManagerRow.ui")
            commandBuilder.set("$selector #NpcName.Text", npc.name)
            commandBuilder.set("$selector #NpcId.Text", "ID: ${npc.id}")
            commandBuilder.set("$selector #NpcType.Text", npc.type)

            val deliveryText = if (npc.isDeliveryRecipient) {
                "✓ Recipient"
            } else if (npc.deliveryRole.isNotBlank() && !npc.deliveryRole.equals("none", ignoreCase = true)) {
                "✓ ${npc.deliveryRole.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }}"
            } else {
                "—"
            }
            commandBuilder.set("$selector #NpcDelivery.Text", deliveryText)

            val shopText = if (npc.hasShop) {
                if (npc.shopId.isNotBlank() && !npc.shopId.equals("Shop", ignoreCase = true)) {
                    "✓ ${npc.shopId}"
                } else {
                    "✓ Yes"
                }
            } else {
                "—"
            }
            commandBuilder.set("$selector #NpcShop.Text", shopText)

            val coordsText = "${npc.position.x.toInt()}, ${npc.position.y.toInt()}, ${npc.position.z.toInt()}"
            commandBuilder.set("$selector #NpcCoords.Text", coordsText)

            val tpData = EventData.of("Action", "tp").put("Target", i.toString())
            eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating, "$selector #TpButton", tpData, false
            )
        }
    }

    private fun sortNpcList() {
        val dir = if (sortAscending) 1 else -1
        displayedNpcs.sortWith { a, b ->
            val result = when (currentSortField) {
                SortField.TYPE -> a.type.compareTo(b.type, ignoreCase = true)
                SortField.ID -> a.id.compareTo(b.id, ignoreCase = true)
                SortField.DELIVERY -> {
                    b.isDeliveryRecipient.compareTo(a.isDeliveryRecipient)
                }

                SortField.SHOP -> {
                    b.hasShop.compareTo(a.hasShop)
                }
            }
            if (result != 0) {
                result * dir
            } else {
                a.name.compareTo(b.name, ignoreCase = true)
            }
        }
    }

    private fun collectNpcs(store: Store<EntityStore>, world: World) {
        allNpcs.clear()
        displayedNpcs.clear()

        val npcCompType = NPCEntity.getComponentType()
        val query = if (npcCompType != null) {
            Query.or(
                PlayerNpcComponent.componentType, VaultNpcComponent.componentType, npcCompType
            )
        } else {
            Query.or(
                PlayerNpcComponent.componentType, VaultNpcComponent.componentType
            )
        }

        val deliveryService = try {
            ThankmasDeliveryPlugin.get()?.deliveryService()
        } catch (_: Throwable) {
            null
        }
        val deliveryStore = deliveryService?.deliveryStore()

        val seenUuids = mutableSetOf<UUID>()

        store.forEachChunk(query, BiConsumer { chunk, _ ->
            for (i in 0 until chunk.size()) {
                // Skip real players
                if (chunk.getComponent(i, Player.getComponentType()) != null) continue

                val entityRef = chunk.getReferenceTo(i)
                if (!entityRef.isValid) continue

                val transform = chunk.getComponent(i, TransformComponent.getComponentType()) ?: continue
                val uuidComp = chunk.getComponent(i, UUIDComponent.getComponentType())
                val uuid = uuidComp?.uuid ?: UUID.randomUUID()
                if (!seenUuids.add(uuid)) continue

                val vaultComp = chunk.getComponent(i, VaultNpcComponent.componentType)
                val playerNpcComp = chunk.getComponent(i, PlayerNpcComponent.componentType)
                val deliveryComp = try {
                    val dType = DeliveryNpc.getComponentType()
                    if (dType != null) chunk.getComponent(i, dType) else null
                } catch (_: Throwable) {
                    null
                }
                val npcEntity = if (npcCompType != null) chunk.getComponent(i, npcCompType) else null

                // Resolve Display Name
                val nameplate = chunk.getComponent(i, Nameplate.getComponentType())
                val dispNameComp = chunk.getComponent(i, DisplayNameComponent.getComponentType())
                val pDispNameComp = chunk.getComponent(i, PersistentDisplayName.getComponentType())

                val rawName = vaultComp?.displayName?.takeIf { it.isNotBlank() } ?: deliveryComp?.name()
                    ?.takeIf { it.isNotBlank() } ?: nameplate?.text?.takeIf { it.isNotBlank() }
                ?: dispNameComp?.displayName?.rawText?.takeIf { it.isNotBlank() }
                ?: pDispNameComp?.displayName?.rawText?.takeIf { it.isNotBlank() }
                ?: npcEntity?.roleName?.takeIf { it.isNotBlank() } ?: "NPC #${uuid.toString().substring(0, 6)}"

                // Resolve ID
                val rawId =
                    deliveryComp?.id()?.takeIf { it.isNotBlank() } ?: vaultComp?.pointId?.takeIf { it.isNotBlank() }
                    ?: npcEntity?.roleName?.takeIf { it.isNotBlank() } ?: uuid.toString().substring(0, 8)

                // Resolve Type
                val isPlayerNpc =
                    playerNpcComp != null || (vaultComp != null && vaultComp.skinJson.isNotBlank()) || AppearanceHelper.isPlayerNpc(
                        entityRef,
                        store
                    )
                val rawType = when {
                    vaultComp?.isStatue == true -> "Statue"
                    isPlayerNpc -> "Player NPC"
                    !npcEntity?.roleName.isNullOrBlank() -> npcEntity.roleName
                    deliveryComp != null -> "Delivery ${
                        deliveryComp.kind().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                    }"

                    else -> "NPC"
                }

                // Resolve Delivery status
                val isRecipientInStore = if (deliveryStore != null) {
                    val dId = deliveryComp?.id() ?: rawId
                    deliveryStore.recipient(dId) != null
                } else false

                val isRecipient = (deliveryComp != null && deliveryComp.recipient()) || isRecipientInStore
                val deliveryRole = deliveryComp?.kind() ?: if (isRecipient) "recipient" else ""

                // Resolve Shop status
                var hasShop = false
                var shopId = ""
                if (vaultComp != null && vaultComp.shopId.isNotBlank()) {
                    hasShop = true
                    shopId = vaultComp.shopId
                } else if (npcEntity != null) {
                    val role = npcEntity.role
                    if (role != null) {
                        val root = role.rootInstruction
                        val inter = role.interactionInstruction
                        if ((root != null && hasShopAction(root)) || (inter != null && hasShopAction(inter))) {
                            hasShop = true
                            shopId = findShopIdInAction(inter) ?: findShopIdInAction(root) ?: "Shop"
                        }
                    }
                }

                allNpcs.add(
                    NpcEntry(
                        entityRef = entityRef,
                        uuid = uuid,
                        id = rawId,
                        name = rawName,
                        type = rawType,
                        isDeliveryRecipient = isRecipient,
                        deliveryRole = deliveryRole,
                        hasShop = hasShop,
                        shopId = shopId,
                        position = Vector3d(transform.position),
                        worldName = world.name
                    )
                )
            }
        })

        displayedNpcs.addAll(allNpcs)
    }

    private fun hasShopAction(component: IAnnotatedComponent): Boolean {
        val className = component.javaClass.simpleName
        if (className.contains("Shop", ignoreCase = true) || className.contains("Barter", ignoreCase = true)) {
            return true
        }
        if (component is IAnnotatedComponentCollection) {
            for (i in 0 until component.componentCount()) {
                val child = component.getComponent(i) ?: continue
                if (hasShopAction(child)) return true
            }
        }
        return false
    }

    private fun findShopIdInAction(component: IAnnotatedComponent?): String? {
        if (component == null) return null
        try {
            val field = component.javaClass.getDeclaredField("shopId")
            field.isAccessible = true
            val value = field.get(component) as? String
            if (!value.isNullOrBlank()) return value
        } catch (_: Throwable) {
        }

        if (component is IAnnotatedComponentCollection) {
            for (i in 0 until component.componentCount()) {
                val child = component.getComponent(i) ?: continue
                val found = findShopIdInAction(child)
                if (found != null) return found
            }
        }
        return null
    }

    class NpcListPageData {
        var action: String? = null
        var target: String? = null

        companion object {
            @JvmField
            val CODEC: BuilderCodec<NpcListPageData> = BuilderCodec.builder(
                NpcListPageData::class.java, ::NpcListPageData
            ).append(KeyedCodec("Action", Codec.STRING), { data, s -> data.action = s }, { it.action ?: "" }).add()
                .append(KeyedCodec("Target", Codec.STRING), { data, s -> data.target = s }, { it.target ?: "" }).add()
                .build()
        }
    }
}
