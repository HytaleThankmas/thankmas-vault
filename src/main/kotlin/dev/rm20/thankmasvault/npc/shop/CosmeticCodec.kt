package dev.rm20.thankmasvault.npc.shop

import com.hypixel.hytale.assetstore.AssetKeyValidator
import com.hypixel.hytale.codec.Codec
import com.hypixel.hytale.codec.ExtraInfo
import com.hypixel.hytale.codec.schema.SchemaContext
import com.hypixel.hytale.codec.schema.config.Schema
import com.hypixel.hytale.codec.util.RawJsonReader
import com.hypixel.hytale.codec.validation.ValidatableCodec
import com.hypixel.hytale.codec.validation.ValidatorCache
import dev.hardaway.wardrobe.impl.cosmetic.CosmeticAsset
import org.bson.BsonValue

object CosmeticCodec : Codec<String>, ValidatableCodec<String> {
    val VALIDATOR_CACHE: ValidatorCache<String> = ValidatorCache(AssetKeyValidator<String> { CosmeticAsset.ASSET_STORE.get() })

    override fun decode(bsonValue: BsonValue, extraInfo: ExtraInfo): String {
        val s = Codec.STRING.decode(bsonValue, extraInfo) ?: ""
        validate(s, extraInfo)
        return s
    }

    override fun decodeJson(reader: RawJsonReader, extraInfo: ExtraInfo): String {
        val s = Codec.STRING.decodeJson(reader, extraInfo) ?: ""
        validate(s, extraInfo)
        return s
    }

    override fun encode(value: String, extraInfo: ExtraInfo): BsonValue {
        return Codec.STRING.encode(value, extraInfo)
    }

    override fun validate(t: String, extraInfo: ExtraInfo) {
        if (t.isNotEmpty()) {
            VALIDATOR_CACHE.validator.accept(t, extraInfo.validationResults)
        }
    }

    override fun validateDefaults(extraInfo: ExtraInfo, tested: MutableSet<Codec<*>>) {
    }

    override fun toSchema(context: SchemaContext): Schema {
        val schema = Codec.STRING.toSchema(context)
        VALIDATOR_CACHE.validator.updateSchema(context, schema)
        return schema
    }
}
