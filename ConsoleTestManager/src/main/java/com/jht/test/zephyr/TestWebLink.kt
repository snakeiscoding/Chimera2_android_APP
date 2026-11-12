package com.jht.test.zephyr

import org.json.JSONObject

class TestWebLink(val id: Int, val url: String, val description: String) : ZephyrObject {
    override fun addPostData(postData: JSONObject) {
        postData.put("description", description)
        postData.put("url", url)
    }

    override fun addQueryParams(params: HashMap<String?, String?>) {
    }
}