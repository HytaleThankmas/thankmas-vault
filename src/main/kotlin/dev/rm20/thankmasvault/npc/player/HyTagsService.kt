package dev.rm20.thankmasvault.npc.player

import com.google.gson.JsonParser
import com.hypixel.hytale.protocol.PlayerSkin
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.CompletableFuture

object HyTagsService {

    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(6))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    /**
     * Look up a player's skin from hytags
     *
     * @param username The player username
     * @return PlayerSkin, or null if not found/error
     */
    fun fetchSkinByUsername(username: String): CompletableFuture<PlayerSkin?> {
        val trimmed = username.trim()
        if (trimmed.isEmpty()) return CompletableFuture.completedFuture(null)

        val apiUri = try {
            URI.create("https://hytags.com/api/username/$trimmed")
        } catch (_: Throwable) {
            return CompletableFuture.completedFuture(null)
        }

        val request = HttpRequest.newBuilder()
            .uri(apiUri)
            .header("User-Agent", "ThankmasVault/1.0 (HytaleServerPlugin)")
            .timeout(Duration.ofSeconds(8))
            .GET()
            .build()

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply { response ->
                if (response.statusCode() == 200) {
                    parseSkinFromApiJson(response.body())
                } else {
                    null
                }
            }
            .exceptionally {
                null
            }
    }

    private fun parseSkinFromApiJson(json: String): PlayerSkin? {
        return try {
            val root = JsonParser.parseString(json).asJsonObject
            if (!root.has("skin") || !root.get("skin").isJsonObject) return null
            val s = root.getAsJsonObject("skin")
            val skin = PlayerSkin()

            fun getStr(key: String): String? {
                return if (s.has(key) && !s.get(key).isJsonNull) s.get(key).asString else null
            }

            skin.bodyCharacteristic = getStr("bodyCharacteristic")
            skin.underwear = getStr("underwear")
            skin.face = getStr("face")
            skin.ears = getStr("ears")
            skin.mouth = getStr("mouth")
            skin.haircut = getStr("haircut")
            skin.facialHair = getStr("facialHair")
            skin.eyebrows = getStr("eyebrows")
            skin.eyes = getStr("eyes")
            skin.pants = getStr("pants")
            skin.overpants = getStr("overpants")
            skin.undertop = getStr("undertop")
            skin.overtop = getStr("overtop")
            skin.shoes = getStr("shoes")
            skin.headAccessory = getStr("headAccessory")
            skin.faceAccessory = getStr("faceAccessory")
            skin.earAccessory = getStr("earAccessory")
            skin.skinFeature = getStr("skinFeature")
            skin.gloves = getStr("gloves")
            skin.cape = getStr("cape")

            AppearanceHelper.ensureValidSkin(skin)
        } catch (_: Throwable) {
            null
        }
    }
}
