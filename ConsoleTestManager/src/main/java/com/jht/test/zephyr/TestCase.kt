package com.jht.test.zephyr

import com.jht.test.testfinder.InstrumentationTest
import org.json.JSONObject

class TestCase(
    val json: JSONObject = JSONObject(),
    private val folderId: Long = 0,
    val name: String = json.optString("name", ""),
    val description: String = json.optString("objective", ""),
    val uuid: String = json.optJSONObject("customFields")?.optString("UUID", "") ?: ""
) : ZephyrObject {

// TestCase(testFolder, test.name, test.description), test)
    override fun addPostData(postData: JSONObject) {
        if(json.has("name")) {
            for(key in json.keys()) {
                postData.put(key, json[key])
            }
        }
        else {
            postData.put("folderId", folderId)
            postData.put("name", name)
            postData.put("objective", description)
            val customFields = JSONObject()
            customFields.put("UUID", uuid)
            postData.put("customFields", customFields)
        }
    }

    override fun addQueryParams(params: HashMap<String?, String?>) {
        //folder.addQueryParams(params)
        //params["maxResults"] = "10"
    }

    fun update(test: InstrumentationTest) {
        json.put("objective", test.description)
        json.put("name", test.name)
        val customFields = json.optJSONObject("customFields") ?: JSONObject()
        customFields.put("UUID", test.uuid)
        json.put("customFields", customFields)
        test.zephyrTestCase = key()
    }

    fun hasRequirement(requirement: String) : Boolean {
        val links = json.optJSONObject("links")
        val webLinks = links?.optJSONArray("webLinks") ?: return false
        for(i in 0 until webLinks.length()) {
            val link = webLinks.optJSONObject(i) ?: continue
            val linkDescription = link.optString("description", "")
            val linkURL = link.optString("url", "")
            if(linkDescription == "Requirement" && linkURL == requirement) {
                return true
            }
        }
        return false
    }

    fun key() : String {
        return json.optString("key", "")
    }

    fun matches(test: InstrumentationTest): Boolean {
        return test.name == name || (test.uuid.isNotEmpty() && test.uuid == uuid)
    }

}