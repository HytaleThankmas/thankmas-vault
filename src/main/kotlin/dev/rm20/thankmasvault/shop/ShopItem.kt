package dev.rm20.thankmasvault.shop

import com.hypixel.hytale.codec.Codec
import com.hypixel.hytale.codec.KeyedCodec
import com.hypixel.hytale.codec.builder.BuilderCodec
import dev.rm20.codecannotation.Annotations.CodecAnnotations

class ShopItem(
    @field:CodecAnnotations.Field("ItemId")
    var itemId: String = "",

    @field:CodecAnnotations.Field("Cost")
    var cost: Int = 0,

    @field:CodecAnnotations.Field("Quantity")
    var quantity: Int = 1,

    @field:CodecAnnotations.Field("DisplayName")
    var displayName: String? = null
) {
    companion object {
        @JvmField
        val CODEC: BuilderCodec<ShopItem> = BuilderCodec.builder(ShopItem::class.java, ::ShopItem)
            .append(
                KeyedCodec("ItemId", Codec.STRING),
                { item, id -> item.itemId = id ?: "" },
                { item -> item.itemId }
            ).add()
            .append(
                KeyedCodec("Cost", Codec.INTEGER),
                { item, cost -> item.cost = cost ?: 0 },
                { item -> item.cost }
            ).add()
            .append(
                KeyedCodec("Quantity", Codec.INTEGER),
                { item, qty -> item.quantity = qty ?: 1 },
                { item -> item.quantity }
            ).add()
            .append(
                KeyedCodec("DisplayName", Codec.STRING),
                { item, name -> item.displayName = name },
                { item -> item.displayName }
            ).add()
            .build()
    }
}
