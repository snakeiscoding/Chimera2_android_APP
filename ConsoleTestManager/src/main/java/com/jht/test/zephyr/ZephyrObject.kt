package com.jht.test.zephyr

import org.json.JSONObject

interface ZephyrObject {
    fun addPostData(postData: JSONObject)
    fun addQueryParams(params: HashMap<String?, String?>)
}