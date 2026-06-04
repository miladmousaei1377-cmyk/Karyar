package com.example.core.utils

import org.json.JSONArray
import org.json.JSONObject

@Suppress("UNCHECKED_CAST")
fun Map<String, Any?>.toJSONObject(): JSONObject {
    val obj = JSONObject()
    forEach { (key, value) ->
        when (value) {
            null -> obj.put(key, JSONObject.NULL)
            is Map<*, *> -> obj.put(key, (value as Map<String, Any?>).toJSONObject())
            is List<*> -> obj.put(key, value.toJSONArray())
            else -> obj.put(key, value)
        }
    }
    return obj
}

fun List<*>.toJSONArray(): JSONArray {
    val arr = JSONArray()
    forEach { value ->
        when (value) {
            null -> arr.put(JSONObject.NULL)
            is Map<*, *> -> @Suppress("UNCHECKED_CAST")
                arr.put((value as Map<String, Any?>).toJSONObject())
            is List<*> -> arr.put(value.toJSONArray())
            else -> arr.put(value)
        }
    }
    return arr
}

fun JSONObject.toMap(): Map<String, Any> {
    val map = mutableMapOf<String, Any>()
    keys().forEach { key ->
        map[key] = when (val v = get(key)) {
            is JSONObject -> v.toMap()
            is JSONArray -> v.toList()
            JSONObject.NULL -> ""
            else -> v
        }
    }
    return map
}

fun JSONArray.toList(): List<Any> {
    return (0 until length()).map { i ->
        when (val v = get(i)) {
            is JSONObject -> v.toMap()
            is JSONArray -> v.toList()
            JSONObject.NULL -> ""
            else -> v
        }
    }
}
