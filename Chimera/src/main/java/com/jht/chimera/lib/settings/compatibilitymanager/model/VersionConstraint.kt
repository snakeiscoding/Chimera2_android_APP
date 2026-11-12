package com.jht.chimera.lib.settings.compatibilitymanager.model

/**
 * Represents a constraint for an integer based version number.
 *
 * Use [appliesTo] to check whether a provided version satisfies the constraint.
 */
sealed class VersionConstraint {
    private data object Any : VersionConstraint()
    private data class Before(val version: Int, val inclusive: Boolean = false) : VersionConstraint()
    private data class After(val version: Int, val inclusive: Boolean = false) : VersionConstraint()
    private data class Range(val min: Int? = null, val max: Int? = null) : VersionConstraint()
    private data class Versions(val versions: Set<Int>) : VersionConstraint()
    private data class Version(val version: Int) : VersionConstraint()

    companion object {
        /**
         * Creates a version constraint that accepts any version.
         */
        fun any(): VersionConstraint = Any

        /**
         * Creates a version constraint that accepts only versions preceding the specified version.
         *
         * @param version the threshold version.
         * @param inclusive indicates whether the threshold version should be included in the constraint.
         */
        fun before(version: Int, inclusive: Boolean = false): VersionConstraint = Before(version, inclusive)

        /**
         * Creates a version constraint that accepts only versions following the specified version.
         *
         * @param version the threshold version.
         * @param inclusive indicates whether the threshold version should be included in the constraint.
         */
        fun after(version: Int, inclusive: Boolean = false): VersionConstraint = After(version, inclusive)

        /**
         * Creates a version constraint that accepts only versions within the specified range.
         *
         * @param min the minimum version.
         * @param max the maximum version.
         */
        fun range(min: Int? = null, max: Int? = null): VersionConstraint = Range(min, max)

        /**
         * Creates a version constraint that accepts only the specified versions.
         *
         * @param versions the versions for which the constraint is met.
         */
        fun of(vararg versions: Int): VersionConstraint = Versions(versions.toSet())

        /**
         * Creates a version constraint that accepts only the specified versions from the given collection.
         *
         * @param versions the collection containing the versions for which the constraint is met.
         */
        fun of(versions: Collection<Int>): VersionConstraint = Versions(versions.toSet())

        /**
         * Creates a version constraint that accepts only the specified version.
         *
         * @param version the version for which the constraint is met.
         */
        fun of(version: Int): VersionConstraint = Version(version)
    }

    /**
     * Checks if the provided version satisfies the constraint.
     *
     * @param version the version to test.
     * @return true if the version meets the constraint; false otherwise.
     */
    fun appliesTo(version: Int): Boolean = when (this) {
        is Any -> true
        is Before -> version <= (if (inclusive) this.version else this.version - 1)
        is After -> version >= (if (inclusive) this.version else this.version + 1)
        is Range -> version in (min ?: Int.MIN_VALUE)..(max ?: Int.MAX_VALUE)
        is Versions -> version in versions
        is Version -> this.version == version
    }
}