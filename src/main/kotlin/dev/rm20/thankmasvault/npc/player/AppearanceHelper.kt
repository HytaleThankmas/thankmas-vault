package dev.rm20.thankmasvault.npc.player

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.hypixel.hytale.component.Holder
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.protocol.PlayerSkin
import com.hypixel.hytale.server.core.NameMatching
import com.hypixel.hytale.server.core.asset.type.model.config.Model
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAttachment
import com.hypixel.hytale.server.core.cosmetics.CosmeticsModule
import com.hypixel.hytale.server.core.cosmetics.PlayerSkinPart
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel
import com.hypixel.hytale.server.core.modules.entity.player.PlayerSkinComponent
import com.hypixel.hytale.server.core.universe.Universe
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.hypixel.hytale.server.npc.entities.NPCEntity
import dev.hardaway.wardrobe.impl.player.PlayerWardrobeComponent
import dev.rm20.thankmasvault.npc.VaultNpcComponent
import java.util.concurrent.CompletableFuture
import kotlin.collections.iterator


object  AppearanceHelper {

    private const val DEFAULT_PLAYER_MODEL_NAME = "Player"

    fun isPlayerNpc(ref: Ref<EntityStore>, store: Store<EntityStore>): Boolean {
        if (!ref.isValid) return false

        if (store.getComponent(ref, PlayerSkinComponent.getComponentType()) != null) {
            return true
        }
        val vaultComp = store.getComponent(ref, VaultNpcComponent.componentType)
        if (vaultComp != null && vaultComp.skinJson.isNotBlank()) {
            return true
        }
        val modelComp = store.getComponent(ref, ModelComponent.getComponentType())
        val assetId = modelComp?.model?.modelAssetId
        if (assetId != null && (assetId.equals(DEFAULT_PLAYER_MODEL_NAME, ignoreCase = true) || assetId.contains("Player", ignoreCase = true))) {
            return true
        }

        val npcComponentType = NPCEntity.getComponentType()
        if (npcComponentType != null) {
            val npcEntity = store.getComponent(ref, npcComponentType)
            if (npcEntity != null) {
                val roleName = npcEntity.roleName
                if (roleName != null && roleName.contains("Player", ignoreCase = true)) {
                    return true
                }
                val role = npcEntity.role
                if (role != null) {
                    val appearance = role.appearanceName
                    if (appearance != null && appearance.contains("Player", ignoreCase = true)) {
                        return true
                    }
                }
            }
        }

        return false
    }

    fun ensureValidSkin(skin: PlayerSkin): PlayerSkin {
        val cosmetics = try {
            CosmeticsModule.get()
        } catch (_: Throwable) {
            null
        } ?: return skin

        val registry = cosmetics.registry ?: return skin

        fun isTextureValid(skinPart: PlayerSkinPart, variantId: String?, texture: String): Boolean {
            val gradientSetName = skinPart.gradientSet
            if (gradientSetName != null) {
                val gSet = registry.gradientSets[gradientSetName]
                if (gSet != null) {
                    if (gSet.gradients.containsKey(texture)) return true
                    if (gSet.gradients.keys.any { it.equals(texture, ignoreCase = true) }) return true
                }
            }
            return if (skinPart.variants != null) {
                if (variantId != null) {
                    val variant = skinPart.variants[variantId] ?: return false
                    variant.textures?.containsKey(texture) == true ||
                        variant.textures?.keys?.any { it.equals(texture, ignoreCase = true) } == true
                } else false
            } else {
                skinPart.textures?.containsKey(texture) == true ||
                    skinPart.textures?.keys?.any { it.equals(texture, ignoreCase = true) } == true
            }
        }

        fun getFirstValidTexture(skinPart: PlayerSkinPart, variantId: String?): String? {
            val gradientSetName = skinPart.gradientSet
            if (gradientSetName != null) {
                val gSet = registry.gradientSets[gradientSetName]
                val first = gSet?.gradients?.keys?.firstOrNull()
                if (first != null) return first
            }
            return if (skinPart.variants != null) {
                if (variantId != null) {
                    skinPart.variants[variantId]?.textures?.keys?.firstOrNull()
                } else null
            } else {
                skinPart.textures?.keys?.firstOrNull()
            }
        }

        fun sanitizeAttachment(map: Map<String, PlayerSkinPart>, rawId: String?): String? {
            if (rawId.isNullOrBlank()) return null

            val idParts = rawId.split('.')
            val partName = idParts[0]

            val partKey = if (map.containsKey(partName)) {
                partName
            } else {
                map.keys.firstOrNull {
                    it.equals(partName, ignoreCase = true) ||
                        it.equals(partName.replace(' ', '_'), ignoreCase = true) ||
                        it.replace("[_ ]".toRegex(), "").equals(partName.replace("[_ ]".toRegex(), ""), ignoreCase = true)
                }
            } ?: return null

            val skinPart = map[partKey] ?: return null
            val textureToken = if (idParts.size > 1 && idParts[1].isNotBlank()) idParts[1] else null

            if (skinPart.variants != null && skinPart.variants.isNotEmpty()) {
                val rawVariant = if (idParts.size > 2 && idParts[2].isNotBlank()) idParts[2] else null
                val matchedVariant = if (rawVariant != null) {
                    if (skinPart.variants.containsKey(rawVariant)) rawVariant
                    else skinPart.variants.keys.firstOrNull { it.equals(rawVariant, ignoreCase = true) }
                } else null

                if (matchedVariant != null) {
                    val finalTex = if (textureToken != null && isTextureValid(skinPart, matchedVariant, textureToken)) {
                        textureToken
                    } else {
                        getFirstValidTexture(skinPart, matchedVariant)
                    }
                    return if (finalTex != null) "$partKey.$finalTex.$matchedVariant" else null
                } else {
                    // Variant missing
                    val candidateVariant = if (textureToken != null) {
                        skinPart.variants.keys.firstOrNull { isTextureValid(skinPart, it, textureToken) }
                            ?: skinPart.variants.keys.firstOrNull()
                    } else {
                        skinPart.variants.keys.firstOrNull()
                    } ?: return null

                    val finalTex = if (textureToken != null && isTextureValid(skinPart, candidateVariant, textureToken)) {
                        textureToken
                    } else {
                        getFirstValidTexture(skinPart, candidateVariant)
                    }
                    return if (finalTex != null) "$partKey.$finalTex.$candidateVariant" else null
                }
            } else {
                // No variants
                val finalTex = if (textureToken != null && isTextureValid(skinPart, null, textureToken)) {
                    textureToken
                } else {
                    getFirstValidTexture(skinPart, null)
                }
                return if (finalTex != null) "$partKey.$finalTex" else null
            }
        }

        fun defaultAttachment(map: Map<String, PlayerSkinPart>): String? {
            for ((k, _) in map) {
                val candidate = sanitizeAttachment(map, k)
                if (candidate != null) return candidate
            }
            return null
        }

        fun sanitizeSimple(map: Map<String, *>, id: String?, defaultKeywords: List<String>): String {
            if (!id.isNullOrBlank()) {
                if (map.containsKey(id)) return id
                val matched = map.keys.firstOrNull {
                    it.equals(id, ignoreCase = true) ||
                        it.equals(id.replace(' ', '_'), ignoreCase = true) ||
                        it.replace("[_ ]".toRegex(), "").equals(id.replace("[_ ]".toRegex(), ""), ignoreCase = true)
                }
                if (matched != null) return matched
            }
            for (kw in defaultKeywords) {
                val found = map.keys.firstOrNull { it.contains(kw, ignoreCase = true) }
                if (found != null) return found
            }
            return map.keys.firstOrNull() ?: "Default"
        }

        // required
        skin.face = sanitizeSimple(registry.faces, skin.face, listOf("Neutral", "Default"))
        skin.ears = sanitizeSimple(registry.ears, skin.ears, listOf("Default"))
        skin.mouth = sanitizeSimple(registry.mouths, skin.mouth, listOf("Default"))

        skin.bodyCharacteristic = sanitizeAttachment(registry.bodyCharacteristics, skin.bodyCharacteristic)
            ?: defaultAttachment(registry.bodyCharacteristics) ?: "Default.1"
        skin.underwear = sanitizeAttachment(registry.underwear, skin.underwear)
            ?: defaultAttachment(registry.underwear) ?: "Underwear.1"
        skin.eyes = sanitizeAttachment(registry.eyes, skin.eyes)
            ?: defaultAttachment(registry.eyes) ?: "Eyes.1"

        // repair attachments or removes if invalid
        skin.haircut = sanitizeAttachment(registry.haircuts, skin.haircut)
        skin.eyebrows = sanitizeAttachment(registry.eyebrows, skin.eyebrows)
        skin.pants = sanitizeAttachment(registry.pants, skin.pants)
        skin.overpants = sanitizeAttachment(registry.overpants, skin.overpants)
        skin.undertop = sanitizeAttachment(registry.undertops, skin.undertop)
        skin.overtop = sanitizeAttachment(registry.overtops, skin.overtop)
        skin.shoes = sanitizeAttachment(registry.shoes, skin.shoes)
        skin.headAccessory = sanitizeAttachment(registry.headAccessories, skin.headAccessory)
        skin.faceAccessory = sanitizeAttachment(registry.faceAccessories, skin.faceAccessory)
        skin.earAccessory = sanitizeAttachment(registry.earAccessories, skin.earAccessory)
        skin.gloves = sanitizeAttachment(registry.gloves, skin.gloves)
        skin.facialHair = sanitizeAttachment(registry.facialHairs, skin.facialHair)
        skin.skinFeature = sanitizeAttachment(registry.skinFeatures, skin.skinFeature)
        skin.cape = sanitizeAttachment(registry.capes, skin.cape)

        // Checks skin
        var attempts = 0
        while (attempts < 25) {
            attempts++
            try {
                cosmetics.validateSkin(skin)
                break
            } catch (e: CosmeticsModule.InvalidSkinException) {
                val partType = e.partType?.lowercase() ?: break
                when (partType) {
                    "face" -> skin.face = sanitizeSimple(registry.faces, null, listOf("Neutral", "Default"))
                    "ears" -> skin.ears = sanitizeSimple(registry.ears, null, listOf("Default"))
                    "mouth" -> skin.mouth = sanitizeSimple(registry.mouths, null, listOf("Default"))
                    "body" -> skin.bodyCharacteristic = defaultAttachment(registry.bodyCharacteristics)
                    "underwear" -> skin.underwear = defaultAttachment(registry.underwear)
                    "eyes" -> skin.eyes = defaultAttachment(registry.eyes)
                    "haircut" -> skin.haircut = null
                    "eyebrows" -> skin.eyebrows = null
                    "pants" -> skin.pants = null
                    "overpants" -> skin.overpants = null
                    "undertop" -> skin.undertop = null
                    "overtop" -> skin.overtop = null
                    "shoes" -> skin.shoes = null
                    "head accessory" -> skin.headAccessory = null
                    "face accessory" -> skin.faceAccessory = null
                    "ear accessory" -> skin.earAccessory = null
                    "skin feature" -> skin.skinFeature = null
                    "gloves" -> skin.gloves = null
                    "facial hair" -> skin.facialHair = null
                    "cape" -> skin.cape = null
                    else -> break
                }
            } catch (_: Throwable) {
                break
            }
        }

        return skin
    }

    fun playerSkinToJson(skin: PlayerSkin): String {
        val json = JsonObject()
        skin.bodyCharacteristic?.let { json.addProperty("bodyCharacteristic", it) }
        skin.underwear?.let { json.addProperty("underwear", it) }
        skin.face?.let { json.addProperty("face", it) }
        skin.eyes?.let { json.addProperty("eyes", it) }
        skin.ears?.let { json.addProperty("ears", it) }
        skin.mouth?.let { json.addProperty("mouth", it) }
        skin.facialHair?.let { json.addProperty("facialHair", it) }
        skin.haircut?.let { json.addProperty("haircut", it) }
        skin.eyebrows?.let { json.addProperty("eyebrows", it) }
        skin.pants?.let { json.addProperty("pants", it) }
        skin.overpants?.let { json.addProperty("overpants", it) }
        skin.undertop?.let { json.addProperty("undertop", it) }
        skin.overtop?.let { json.addProperty("overtop", it) }
        skin.shoes?.let { json.addProperty("shoes", it) }
        skin.gloves?.let { json.addProperty("gloves", it) }
        skin.headAccessory?.let { json.addProperty("headAccessory", it) }
        skin.faceAccessory?.let { json.addProperty("faceAccessory", it) }
        skin.earAccessory?.let { json.addProperty("earAccessory", it) }
        skin.cape?.let { json.addProperty("cape", it) }
        skin.skinFeature?.let { json.addProperty("skinFeature", it) }
        return json.toString()
    }

    fun jsonToPlayerSkin(jsonStr: String): PlayerSkin? {
        if (jsonStr.isBlank()) return null
        return try {
            CosmeticsModule.get().parseSkinFromJson(jsonStr)?.let { ensureValidSkin(it) }
        } catch (_: Throwable) {
            null
        }
    }

    fun modelAttachmentsToJson(model: Model): String {
        val root = JsonObject()
        root.addProperty("scale", model.scale)
        model.modelAssetId?.let { root.addProperty("modelAssetId", it) }
        model.model?.let { root.addProperty("model", it) }
        model.texture?.let { root.addProperty("texture", it) }
        model.gradientSet?.let { root.addProperty("gradientSet", it) }
        model.gradientId?.let { root.addProperty("gradientId", it) }

        val randoms = model.randomAttachmentIds
        if (!randoms.isNullOrEmpty()) {
            val randomsObj = JsonObject()
            for ((k, v) in randoms) {
                randomsObj.addProperty(k, v)
            }
            root.add("randomAttachments", randomsObj)
        }

        val attachments = model.attachments
        if (attachments != null && attachments.isNotEmpty()) {
            val arr = JsonArray()
            for (att in attachments) {
                val attObj = JsonObject()
                att.model?.let { attObj.addProperty("model", it) }
                att.texture?.let { attObj.addProperty("texture", it) }
                att.gradientSet?.let { attObj.addProperty("gradientSet", it) }
                att.gradientId?.let { attObj.addProperty("gradientId", it) }
                attObj.addProperty("weight", att.weight)
                arr.add(attObj)
            }
            root.add("attachments", arr)
        }

        return root.toString()
    }

    fun jsonToModelData(jsonStr: String): SavedModelData {
        if (jsonStr.isBlank()) return SavedModelData()
        return try {
            val root = JsonParser.parseString(jsonStr).asJsonObject
            val scale = if (root.has("scale") && !root.get("scale").isJsonNull) root.get("scale").asFloat else 1.0f
            val modelAssetId = if (root.has("modelAssetId") && !root.get("modelAssetId").isJsonNull) root.get("modelAssetId").asString else null
            val modelPath = if (root.has("model") && !root.get("model").isJsonNull) root.get("model").asString else null
            val texture = if (root.has("texture") && !root.get("texture").isJsonNull) root.get("texture").asString else null
            val gradientSet = if (root.has("gradientSet") && !root.get("gradientSet").isJsonNull) root.get("gradientSet").asString else null
            val gradientId = if (root.has("gradientId") && !root.get("gradientId").isJsonNull) root.get("gradientId").asString else null

            val randomMap = if (root.has("randomAttachments") && root.get("randomAttachments").isJsonObject) {
                val obj = root.getAsJsonObject("randomAttachments")
                obj.entrySet().associate { it.key to it.value.asString }
            } else null

            val attachmentsList = if (root.has("attachments") && root.get("attachments").isJsonArray) {
                val arr = root.getAsJsonArray("attachments")
                arr.mapNotNull { elem ->
                    if (!elem.isJsonObject) null
                    else {
                        val obj = elem.asJsonObject
                        val m = if (obj.has("model") && !obj.get("model").isJsonNull) obj.get("model").asString else null
                        val t = if (obj.has("texture") && !obj.get("texture").isJsonNull) obj.get("texture").asString else null
                        val gs = if (obj.has("gradientSet") && !obj.get("gradientSet").isJsonNull) obj.get("gradientSet").asString else null
                        val gi = if (obj.has("gradientId") && !obj.get("gradientId").isJsonNull) obj.get("gradientId").asString else null
                        val w = if (obj.has("weight") && !obj.get("weight").isJsonNull) obj.get("weight").asDouble else 1.0
                        ModelAttachment(m, t, gs, gi, w)
                    }
                }.toTypedArray()
            } else null

            SavedModelData(
                scale = scale,
                modelAssetId = modelAssetId,
                model = modelPath,
                texture = texture,
                gradientSet = gradientSet,
                gradientId = gradientId,
                randomAttachments = randomMap,
                attachments = attachmentsList
            )
        } catch (_: Throwable) {
            SavedModelData()
        }
    }

    fun jsonToModelAttachments(jsonStr: String): Pair<Map<String, String>?, Array<ModelAttachment>?> {
        val data = jsonToModelData(jsonStr)
        return Pair(data.randomAttachments, data.attachments)
    }

    fun buildFinalModel(rawSkin: PlayerSkin?, savedData: SavedModelData): Model? {
        val skin = rawSkin?.let { ensureValidSkin(it) }
        val baseFromSkin = if (skin != null) {
            try {
                CosmeticsModule.get().createModel(skin)
            } catch (_: Throwable) {
                null
            }
        } else null

        val baseModel: Model = baseFromSkin ?: run {
            val assetId = savedData.modelAssetId ?: DEFAULT_PLAYER_MODEL_NAME
            val modelAsset = ModelAsset.getAssetMap().getAsset(assetId)
                ?: ModelAsset.getAssetMap().getAsset(DEFAULT_PLAYER_MODEL_NAME)
                ?: return null
            Model.createScaledModel(modelAsset, if (savedData.scale > 0f) savedData.scale else 1.0f, savedData.randomAttachments)
        }

        // Attachments: if custom attachments were saved (e.g. from Wardrobe or custom models), use them;
        // otherwise default to the attachments created by CosmeticsModule from the skin.
        val finalAttachments = if (savedData.attachments != null && savedData.attachments.isNotEmpty()) {
            savedData.attachments
        } else {
            baseModel.attachments
        }

        val finalRandoms = savedData.randomAttachments ?: baseModel.randomAttachmentIds
        val finalTexture = savedData.texture ?: baseModel.texture
        val finalGradientSet = savedData.gradientSet ?: baseModel.gradientSet
        val finalGradientId = savedData.gradientId ?: baseModel.gradientId
        val finalModelPath = savedData.model ?: baseModel.model
        val finalAssetId = savedData.modelAssetId ?: baseModel.modelAssetId ?: DEFAULT_PLAYER_MODEL_NAME
        val finalScale = if (savedData.scale > 0f) savedData.scale else baseModel.scale

        return Model(
            finalAssetId,
            finalScale,
            finalRandoms,
            finalAttachments,
            baseModel.boundingBox,
            finalModelPath,
            finalTexture,
            finalGradientSet,
            finalGradientId,
            baseModel.eyeHeight,
            baseModel.crouchOffset,
            baseModel.sittingOffset,
            baseModel.sleepingOffset,
            baseModel.animationSetMap,
            baseModel.camera,
            baseModel.light,
            baseModel.particles,
            baseModel.trails,
            baseModel.physicsValues,
            baseModel.detailBoxes,
            baseModel.phobia,
            baseModel.phobiaModelAssetId
        )
    }

    fun copyPlayerAppearanceToNpc(
        playerRef: Ref<EntityStore>,
        npcRef: Ref<EntityStore>,
        store: Store<EntityStore>,
        saveToConfig: Boolean = true
    ): Boolean {
        val playerStore = playerRef.store
        val playerSkinComp = playerStore.getComponent(playerRef, PlayerSkinComponent.getComponentType())
        val playerSkin = playerSkinComp?.playerSkin?.let { ensureValidSkin(it) }

        val playerModelComp = playerStore.getComponent(playerRef, ModelComponent.getComponentType())
        val playerModel = playerModelComp?.model

        val wardrobeCompType = try {
            PlayerWardrobeComponent.getComponentType()
        } catch (_: Throwable) {
            null
        }
        val playerWardrobe = if (wardrobeCompType != null) playerStore.getComponent(playerRef, wardrobeCompType) else null

        if (playerSkin == null && playerModel == null) {
            return false
        }

        store.externalData.world.execute {
            if (!npcRef.isValid) return@execute

            if (playerSkin != null) {
                val newSkinComp = PlayerSkinComponent(playerSkin)
                newSkinComp.setNetworkOutdated()
                store.putComponent(npcRef, PlayerSkinComponent.getComponentType(), newSkinComp)
            }

            val modelToApply = if (playerModel != null) {
                Model(playerModel)
            } else if (playerSkin != null) {
                CosmeticsModule.get().createModel(playerSkin)
            } else {
                null
            }

            if (modelToApply != null) {
                store.putComponent(npcRef, ModelComponent.getComponentType(), ModelComponent(modelToApply))
                store.putComponent(npcRef, PersistentModel.getComponentType(), PersistentModel(modelToApply.toReference()))
            }

            if (wardrobeCompType != null && playerWardrobe != null) {
                try {
                    store.putComponent(npcRef, wardrobeCompType, playerWardrobe.clone()!!)
                } catch (_: Throwable) {
                }
            }

            if (saveToConfig) {
                var config = store.getComponent(npcRef, VaultNpcComponent.componentType)
                if (config == null) {
                    config = VaultNpcComponent()
                }
                if (playerSkin != null) {
                    config.skinJson = playerSkinToJson(playerSkin)
                }
                if (modelToApply != null) {
                    config.attachmentsJson = modelAttachmentsToJson(modelToApply)
                }
                store.putComponent(npcRef, VaultNpcComponent.componentType, config)
            }
        }
        return true
    }

    fun applyPlayerSkinToNpc(
        skin: PlayerSkin,
        npcRef: Ref<EntityStore>,
        store: Store<EntityStore>,
        saveToConfig: Boolean = true
    ) {
        val validSkin = ensureValidSkin(skin)
        store.externalData.world.execute {
            if (!npcRef.isValid) return@execute

            val newSkinComp = PlayerSkinComponent(validSkin)
            newSkinComp.setNetworkOutdated()
            store.putComponent(npcRef, PlayerSkinComponent.getComponentType(), newSkinComp)

            val newModel = CosmeticsModule.get().createModel(validSkin)
            if (newModel != null) {
                store.putComponent(npcRef, ModelComponent.getComponentType(), ModelComponent(newModel))
                store.putComponent(npcRef, PersistentModel.getComponentType(), PersistentModel(newModel.toReference()))
            }

            if (saveToConfig) {
                var config = store.getComponent(npcRef, VaultNpcComponent.componentType)
                if (config == null) {
                    config = VaultNpcComponent()
                }
                config.skinJson = playerSkinToJson(validSkin)
                if (newModel != null) {
                    config.attachmentsJson = modelAttachmentsToJson(newModel)
                }
                store.putComponent(npcRef, VaultNpcComponent.componentType, config)
            }
        }
    }

    fun copyPlayerAppearanceByUsername(
        username: String,
        npcRef: Ref<EntityStore>,
        store: Store<EntityStore>,
        saveToConfig: Boolean = true
    ): CompletableFuture<Boolean> {
        val trimmed = username.trim()
        if (trimmed.isBlank()) return CompletableFuture.completedFuture(false)

        val universe = try {
            Universe.get()
        } catch (_: Throwable) {
            null
        }

        val targetPlayerRef = universe?.getPlayerByUsername(trimmed, NameMatching.EXACT_IGNORE_CASE)
        val playerEntityRef = targetPlayerRef?.reference
        if (targetPlayerRef != null && playerEntityRef != null && playerEntityRef.isValid) {
            val copied = copyPlayerAppearanceToNpc(playerEntityRef, npcRef, store, saveToConfig)
            return CompletableFuture.completedFuture(copied)
        }

        return HyTagsService.fetchSkinByUsername(trimmed).thenApply { offlineSkin ->
            if (offlineSkin != null) {
                applyPlayerSkinToNpc(offlineSkin, npcRef, store, saveToConfig)
                true
            } else {
                false
            }
        }
    }

    fun applySavedAppearance(
        npcRef: Ref<EntityStore>,
        store: Store<EntityStore>,
        config: VaultNpcComponent
    ) {
        val skinJson = config.skinJson
        val attachmentsJson = config.attachmentsJson

        store.externalData.world.execute {
            if (!npcRef.isValid) return@execute

            val skin = if (skinJson.isNotBlank()) jsonToPlayerSkin(skinJson) else null
            if (skin != null) {
                val skinComp = PlayerSkinComponent(skin)
                skinComp.setNetworkOutdated()
                store.putComponent(npcRef, PlayerSkinComponent.getComponentType(), skinComp)
            }

            val savedData = jsonToModelData(attachmentsJson)
            val finalModel = buildFinalModel(skin, savedData)
            if (finalModel != null) {
                store.putComponent(npcRef, ModelComponent.getComponentType(), ModelComponent(finalModel))
                store.putComponent(npcRef, PersistentModel.getComponentType(), PersistentModel(finalModel.toReference()))
            }
        }
    }

    fun applySavedAppearanceToHolder(
        holder: Holder<EntityStore>,
        config: VaultNpcComponent
    ) {
        val skinJson = config.skinJson
        val attachmentsJson = config.attachmentsJson

        val skin = if (skinJson.isNotBlank()) jsonToPlayerSkin(skinJson) else null
        if (skin != null) {
            val skinComp = PlayerSkinComponent(skin)
            skinComp.setNetworkOutdated()
            holder.putComponent(PlayerSkinComponent.getComponentType(), skinComp)
        }

        val savedData = jsonToModelData(attachmentsJson)
        val finalModel = buildFinalModel(skin, savedData)
        if (finalModel != null) {
            holder.putComponent(ModelComponent.getComponentType(), ModelComponent(finalModel))
            holder.putComponent(PersistentModel.getComponentType(), PersistentModel(finalModel.toReference()))
        }
    }
}
