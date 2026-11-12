package com.jht.chimera.lib.core.documents.serialization

import com.jht.chimera.lib.common.datamatrixkeys.SettingKeys.TVSource
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.mapSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject

/**
 * A custom serializer for handling a map of TV sources to their corresponding TV channel documents.
 *
 * This serializer converts between a JSON object where keys are `TVSource` names (case-insensitive)
 * and values are `TvChannelDocument` objects.
 */
object TvChannelDocumentsSerializer : KSerializer<Map<TVSource, CoreDocument>> {
    /**
     * The serial descriptor defining the structure of the serialized data.
     */
    @OptIn(ExperimentalSerializationApi::class)
    override val descriptor: SerialDescriptor = mapSerialDescriptor(
        keyDescriptor = PrimitiveSerialDescriptor("TVSource", PrimitiveKind.STRING),
        valueDescriptor = CoreDocument.serializer().descriptor
    )

    /**
     * Deserializes a JSON object into a map of `TVSource` to `TvChannelDocument`.
     *
     * @throws SerializationException if the decoder is not a `JsonDecoder`.
     */
    override fun deserialize(decoder: Decoder): Map<TVSource, CoreDocument> {
        val jsonDecoder = decoder as? JsonDecoder ?: throw SerializationException("Invalid decoder, expected JsonDecoder")
        val jsonElement = jsonDecoder.decodeJsonElement()

        val result = mutableMapOf<TVSource, CoreDocument>()

        if (jsonElement is JsonObject) {
            val serializer = CoreDocument.serializer()
            val json = jsonDecoder.json

            for((key, value) in jsonElement) {
                val tvSource = TVSource.entries.find { it.name.equals(key, ignoreCase = true) }
                if (tvSource != null && value is JsonObject) {
                    try {
                        val tvChannelList = json.decodeFromJsonElement(serializer, value)
                        result[tvSource] = tvChannelList
                    } catch (e: Exception) {
                        // Skip over invalid entries
                        continue
                    }
                }
            }
        }

        return result
    }

    /**
     * Serializes a map of `TVSource` to `TvChannelDocument` into a JSON object.
     *
     * @throws SerializationException if the encoder is not a `JsonEncoder`.
     */
    override fun serialize(encoder: Encoder, value: Map<TVSource, CoreDocument>) {
        val jsonEncoder = encoder as? JsonEncoder ?: throw SerializationException("Invalid encoder, expected JsonEncoder")
        val json = jsonEncoder.json

        val jsonObject = buildJsonObject {
            value.forEach { (tvSource, tvChannelList) ->
                val tvChannelListJson = json.encodeToJsonElement(CoreDocument.serializer(), tvChannelList)
                put(tvSource.name.lowercase(), tvChannelListJson)
            }
        }

        jsonEncoder.encodeJsonElement(jsonObject)
    }
}