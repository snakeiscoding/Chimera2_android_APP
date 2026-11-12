package com.jht.test.zephyr

import org.json.JSONArray
import org.json.JSONObject

class TestCycle  (
    private val folder: TestFolder? = null,
    private val name: String = "",
    var key: String = ""
) : ZephyrObject {

    override fun addPostData(postData: JSONObject) {
        folder?.addPostData(postData)
        postData.put("name", name)
        val customFields = JSONObject()
        customFields.put("Purpose", "Acceptance")
        customFields.put("Machine", "All")
        val frames = JSONArray()
        frames.put("None")
        customFields.put("Frame", frames)
        postData.put("customFields", customFields)
    }

    override fun addQueryParams(params: HashMap<String?, String?>) {}
}