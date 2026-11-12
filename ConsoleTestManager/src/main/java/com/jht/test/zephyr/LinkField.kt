package com.jht.test.zephyr

import org.json.JSONObject


class LinkField(
                private val id: Long,
                private val self: String
)  {
    constructor(obj: JSONObject) : this(obj.optLong("id", 0), obj.optString("self", "")) {
    }
    fun toJSON(): JSONObject {
        val obj = JSONObject()
        obj.put("id", id)
        obj.put("self", self)
        return obj
    }

}