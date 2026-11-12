package com.jht.updated.bundler.CORE

import javax.annotation.Nonnull

/**
 * Data class holding values needed for deploying a software collection to a given club on non production environments
 */
data class COREAssociation(@Nonnull val clubId: Int, val dev: Boolean = false, val qa: Boolean = false, val staging: Boolean = false) {
}
