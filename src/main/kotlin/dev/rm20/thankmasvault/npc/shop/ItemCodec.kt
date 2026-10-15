package dev.rm20.thankmasvault.npc.shop

import com.hypixel.hytale.codec.Codec
import com.hypixel.hytale.codec.ExtraInfo
import com.hypixel.hytale.codec.schema.SchemaContext
import com.hypixel.hytale.codec.schema.config.Schema
import com.hypixel.hytale.codec.util.RawJsonReader
import com.hypixel.hytale.codec.validation.ValidatableCodec
import com.hypixel.hytale.server.core.asset.type.item.config.Item
import org.bson.BsonValue

object ItemCodec : Codec<String>, ValidatableCodec<String> {
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
        Item.VALIDATOR_CACHE.validator.accept(t, extraInfo.validationResults)
    }

    override fun validateDefaults(extraInfo: ExtraInfo, tested: MutableSet<Codec<*>>) {
    }

    override fun toSchema(context: SchemaContext): Schema {
        val schema = Codec.STRING.toSchema(context)
        Item.VALIDATOR_CACHE.validator.updateSchema(context, schema)
        return schema
    }
}
