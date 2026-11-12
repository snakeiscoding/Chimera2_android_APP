package com.jht.test.testfinder

import com.google.gson.annotations.SerializedName

data class InstrumentationTest (
    @SerializedName("name") var name: String = "",
    @SerializedName("description") var description: String = "",
    @SerializedName("package") val pkg: String = "",
    @SerializedName("class") val clazz: String = "",
    @SerializedName("function") var function: String = "",
    @SerializedName("runner") var runner: String = "",
    @SerializedName("zephyrTestCase") var zephyrTestCase: String = "",
    @SerializedName("requirement") var requirement: String = "",
    @SerializedName("uuid") var uuid: String = ""
    ) {
    init {
        if(name.isEmpty()) {
            name = "$pkg.$clazz.$function"
        }
    }
}