package com.jht.firsttimesetup

import com.jht.firsttimesetup.viewmodels.DigitalProduct

object StringFormatter {

    // Digital Product names don't need to be translated since they're the same in each language
    var formattedDigitalProductsMap: HashMap<String, DigitalProduct> =
        HashMap<String, DigitalProduct>().apply {
            put("FACILITY_CONCIERGE", DigitalProduct("Facility Concierge", false))
            put("ENGAGE_360", DigitalProduct("Engage 360", false))
            put("IFIT", DigitalProduct("iFit", false))
            put("COMMUNITY_360", DigitalProduct("Community 360", false))
            put("ASSET_MANAGEMENT", DigitalProduct("AM4", false))
            put("YMCA360", DigitalProduct("YMCA 360", false))
        }

    /**
     * Returns a one line address containing the city and state/country
     */
    fun getShortAddress(city: String?, state: String?, country: String?): String {
        return "${city}, " + (if (!state.isNullOrEmpty()) state else country)
    }

    /**
     * Returns a three line address containing the street, city, state, postal code, and country
     */
    fun getLongAddress(
        street1: String?,
        city: String?,
        postalCode: String?,
        state: String?,
        country: String?
    ): String {
        return street1 + "\n" +
                city + (if (!state.isNullOrEmpty()) ", $state" else "") + ", $postalCode\n" +
                country
    }

    /**
     *  Returns a list of DigitalProduct with an updated name for the UI and a boolean that is true
     *  if the Digital Product name is within a map
     *  @param [digitalProducts] list of Digital Product names
     *  @return list of DigitalProduct objects
     */
    fun getDigitalProductList(digitalProducts: List<String>): List<DigitalProduct> {

        // Map is reset for subsequent calls so old information isn't persisted
        resetDigitalProductMap()

        digitalProducts.forEach { digitalProduct ->
            formattedDigitalProductsMap.computeIfPresent(digitalProduct) { _, product ->
                product.copy(
                    isAvailable = true
                )
            }
        }

        return formattedDigitalProductsMap.map { it.value }
    }

    /**
     * Returns the formatted Digital Product name
     * @param [digitalProduct] unformatted name of the Digital Product (ex. ASSET_MANAGEMENT)
     * @return formatted Digital Product name, null otherwise
     */
    fun getFormattedDigitalProduct(digitalProduct: String): DigitalProduct? {
        return formattedDigitalProductsMap[digitalProduct]
    }

    /**
     * Resets the Digital Products map so that all isAvailable values are false
     */
    private fun resetDigitalProductMap() {
        formattedDigitalProductsMap.forEach { (key, value) ->
            formattedDigitalProductsMap[key] = value.copy(isAvailable = false)
        }
    }
}