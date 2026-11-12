package com.jht.data.intent

import android.os.Bundle
import android.util.Log
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule

/*
    Declarative, serializable description of intent extras.

    The point of this file is to let app/screen metadata DECLARE the extras that belong on a start
    intent without holding an android Bundle. A Bundle is Parcel backed, so it has no text form, no
    structural equality and a useless toString() - none of which survives being written to or read
    from a config file. Each type carries its own discriminator (kotlinx writes the "type" field),
    so a value round trips to exactly the putX() call it came from, which a Map<String, Any> cannot
    do (every JSON number decodes as a Double, and Intent extras are strictly typed on read - a
    getIntExtra() against a value stored with putFloat() just returns the default).

    Arbitrary Parcelables are deliberately NOT expressible here. That restriction is what keeps this
    serializable; anything needing a Parcelable extra, or a value only known at runtime, stays in the
    launcher.

    Lists (not arrays) are used throughout so data class equals/hashCode are structural.
 */
private const val TAG = "IntentExtra"

/** A single declared intent extra. */
@Serializable
sealed interface IntentExtra {
    /** extra key, as the receiving activity/app expects it */
    val key: String

    /** writes this extra (and any children) into [bundle] */
    fun applyTo(bundle: Bundle)
}

@Serializable
@SerialName("string")
data class StringExtra(override val key: String, val value: String? = null) : IntentExtra {
    override fun applyTo(bundle: Bundle) = bundle.putString(key, value)
}

@Serializable
@SerialName("bool")
data class BoolExtra(override val key: String, val value: Boolean) : IntentExtra {
    override fun applyTo(bundle: Bundle) = bundle.putBoolean(key, value)
}

@Serializable
@SerialName("int")
data class IntExtra(override val key: String, val value: Int) : IntentExtra {
    override fun applyTo(bundle: Bundle) = bundle.putInt(key, value)
}

/** note: kept separate from [IntExtra] on purpose - a long past 2^53 cannot round trip as a JSON double */
@Serializable
@SerialName("long")
data class LongExtra(override val key: String, val value: Long) : IntentExtra {
    override fun applyTo(bundle: Bundle) = bundle.putLong(key, value)
}

@Serializable
@SerialName("float")
data class FloatExtra(override val key: String, val value: Float) : IntentExtra {
    override fun applyTo(bundle: Bundle) = bundle.putFloat(key, value)
}

@Serializable
@SerialName("stringArray")
data class StringArrayExtra(override val key: String, val values: List<String> = emptyList()) : IntentExtra {
    override fun applyTo(bundle: Bundle) = bundle.putStringArray(key, values.toTypedArray())
}

/** nested bundle extra, such as the panel overrides we hand to our own activities */
@Serializable
@SerialName("bundle")
data class BundleExtra(override val key: String, val children: List<IntentExtra> = emptyList()) : IntentExtra {
    // an empty nested bundle is not harmless: receivers act on its presence, so say nothing instead
    override fun applyTo(bundle: Bundle) {
        if (children.isNotEmpty()) bundle.putBundle(key, children.toBundle())
    }
}

/**
 * Values that are only knowable at launch. A declaration names the SOURCE, never the value, so a
 * config file stays inert text - naming a secret source in config does not put a secret value in it.
 *
 * Deliberately an enum rather than, say, a free form DataMatrix key: it is a whitelist. Config can
 * only ask for values we have decided are safe to hand out. Add a constant here and a branch in the
 * resolver to expose a new one; a name this build does not know is handled by [unknownSources].
 */
@Serializable
enum class DynamicExtraValue {
    /** Boolean: true when the console is not displaying metric units */
    UnitsAreImperial,
    /** String: current branding name */
    BrandingName,
    /** Boolean: true on commercial consoles, entry OMITTED entirely otherwise */
    CommercialFlag,
}

/** Supplies launch time values for [DynamicExtraValue]. Implemented by whoever owns the console state. */
fun interface DynamicExtraResolver {
    /** concrete extra for [key], or null to leave the entry out entirely */
    fun resolve(key: String, source: DynamicExtraValue): IntentExtra?
}

/**
 * Placeholder for a value the launcher fills in - see [resolve].
 *
 * [source] holds the NAME of a [DynamicExtraValue] rather than the enum itself, so a name this build does
 * not know survives decoding verbatim instead of failing the parse or collapsing into an "unknown"
 * constant that cannot say which name it was. Author these through [IntentExtras.dynamic], which
 * takes the enum and stays compile time checked.
 */
@Serializable
@SerialName("dynamic")
data class DynamicExtra(override val key: String, val source: String) : IntentExtra {
    constructor(key: String, source: DynamicExtraValue) : this(key, source.name)
    /** the matching [DynamicExtraValue], or null when this build does not know the name */
    val dynamicExtraValue: DynamicExtraValue? get() = DynamicExtraValue.entries.firstOrNull { it.name == source }

    override fun applyTo(bundle: Bundle) {
        // reaching here means someone skipped resolve() - better a dropped extra and a log than a bogus value
        Log.w(TAG, "unresolved dynamic extra $key ($source)")
    }
}

/**
 * An entry whose "type" discriminator this build does not know, so a newer config can add a subtype
 * without taking out the whole list on an older console - decoding is per list, and one unparseable
 * entry would otherwise fail every entry beside it.
 *
 * [typeName] is kept for the same reason [DynamicExtra.source] is: logs and [unknownTypes] can name
 * what was asked for. Note re-encoding one of these writes "unknown" rather than the original name;
 * these are meant to be read, reported and dropped, not round tripped.
 */
@Serializable
@SerialName("unknown")
data class UnknownExtra(override val key: String = "", val typeName: String = "") : IntentExtra {
    override fun applyTo(bundle: Bundle) {
        Log.w(TAG, "unknown extra type '$typeName' for key $key - entry dropped")
    }
}

/** surrogate so the fallback can still read the one field every entry has */
@Serializable
private data class UnknownExtraSurrogate(val key: String = "")

private class UnknownExtraDeserializer(private val typeName: String) : DeserializationStrategy<IntentExtra> {
    override val descriptor: SerialDescriptor = UnknownExtraSurrogate.serializer().descriptor
    override fun deserialize(decoder: Decoder): IntentExtra = UnknownExtra(UnknownExtraSurrogate.serializer().deserialize(decoder).key, typeName)
}

/** Extra types this build does not know, as "key=type", nested entries included. See [unknownSources]. */
fun List<IntentExtra>?.unknownTypes(): List<String> = this.orEmpty().flatMap { extra ->
    when (extra) {
        is UnknownExtra -> listOf("${extra.key}=${extra.typeName}")
        is BundleExtra -> extra.children.unknownTypes()
        else -> emptyList()
    }
}

/**
 * Replaces [DynamicExtra] entries with concrete ones, dropping the entries [resolver] omits, and with
 * them any [BundleExtra] left with no children. Pure list to list, so launch behavior can be asserted
 * without materializing a Bundle.
 */
fun List<IntentExtra>?.resolve(resolver: DynamicExtraResolver): List<IntentExtra> =
    this?.mapNotNull { extra ->
        when (extra) {
            // a name we do not know is dropped rather than guessed at - see [unknownSources]
            is DynamicExtra -> extra.dynamicExtraValue?.let { resolver.resolve(extra.key, it) }
            // dropped once empty, same choice as an omitted dynamic - prunes upward through nesting
            is BundleExtra -> extra.copy(children = extra.children.resolve(resolver)).takeIf { it.children.isNotEmpty() }
            else -> extra
        }
    } ?: emptyList()

/**
 * Dynamic source names this build does not recognise, as "key=source", nested entries included.
 *
 * Deliberately a query rather than a hard failure: whether an unrecognised source is fatal depends on
 * the entry. A missing display units hint is survivable, an integration hanging on a token is not, so
 * the caller logs this and decides whether to keep the whole app entry. [resolve] always just drops.
 */
fun List<IntentExtra>?.unknownSources(): List<String> = this.orEmpty().flatMap { extra ->
    when (extra) {
        is DynamicExtra -> if (extra.dynamicExtraValue == null) listOf("${extra.key}=${extra.source}") else emptyList()
        is BundleExtra -> extra.children.unknownSources()
        else -> emptyList()
    }
}

/** materializes declared extras into a new Bundle (built fresh per launch, so the declaration stays shareable) */
fun List<IntentExtra>?.toBundle(): Bundle = Bundle().also { bundle ->
    this?.forEach { it.applyTo(bundle) }
}

/**
 * Factories and (de)serialization helpers. Mostly here so java call sites reading like app metadata
 * stay readable, since kotlin call sites can just use the data classes directly.
 */
object IntentExtras {
    private val listSerializer = ListSerializer(IntentExtra.serializer())

    /**
     * Forward compatible on VOCABULARY, not on syntax: an unknown field is ignored and an unknown
     * "type" decodes to [UnknownExtra] rather than failing the whole list, so a newer config adding a
     * subtype still loads on an older console. Malformed JSON still throws from [fromJson] - a broken
     * document is a different thing from one that knows a word we do not.
     */
    private val json = Json {
        ignoreUnknownKeys = true
        serializersModule = SerializersModule {
            polymorphicDefaultDeserializer(IntentExtra::class) { name -> UnknownExtraDeserializer(name ?: "") }
        }
    }

    @JvmStatic fun of(vararg extras: IntentExtra): List<IntentExtra> = extras.toList()

    @JvmStatic fun string(key: String, value: String?): IntentExtra = StringExtra(key, value)
    @JvmStatic fun bool(key: String, value: Boolean): IntentExtra = BoolExtra(key, value)
    @JvmStatic fun integer(key: String, value: Int): IntentExtra = IntExtra(key, value)
    @JvmStatic fun longValue(key: String, value: Long): IntentExtra = LongExtra(key, value)
    @JvmStatic fun floatValue(key: String, value: Float): IntentExtra = FloatExtra(key, value)
    @JvmStatic fun stringArray(key: String, vararg values: String): IntentExtra = StringArrayExtra(key, values.toList())
    @JvmStatic fun bundle(key: String, vararg children: IntentExtra): IntentExtra = BundleExtra(key, children.toList())
    @JvmStatic fun dynamic(key: String, source: DynamicExtraValue): IntentExtra = DynamicExtra(key, source.name)

    /** java friendly aliases of the [resolve] and [toBundle] extensions */
    @JvmStatic fun resolve(extras: List<IntentExtra>?, resolver: DynamicExtraResolver): List<IntentExtra> = extras.resolve(resolver)
    @JvmStatic fun unknownSources(extras: List<IntentExtra>?): List<String> = extras.unknownSources()
    @JvmStatic fun unknownTypes(extras: List<IntentExtra>?): List<String> = extras.unknownTypes()
    @JvmStatic fun toBundle(extras: List<IntentExtra>?): Bundle = extras.toBundle()
    @JvmStatic fun toBundle(extras: List<IntentExtra>?, resolver: DynamicExtraResolver): Bundle = extras.resolve(resolver).toBundle()

    @JvmStatic fun toJson(extras: List<IntentExtra>): String = json.encodeToString(listSerializer, extras)
    @JvmStatic fun fromJson(text: String): List<IntentExtra> = json.decodeFromString(listSerializer, text)
}
