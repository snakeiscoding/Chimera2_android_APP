package com.jht.test.zephyr

import org.json.JSONObject

class TestFolder(private val folderId: Long) : ZephyrObject {
    override fun addQueryParams(params: HashMap<String?, String?>) {
        params["folderId"] = "$folderId"
    }

    override fun addPostData(postData: JSONObject) {
        postData.put("folderId", folderId)
    }
}