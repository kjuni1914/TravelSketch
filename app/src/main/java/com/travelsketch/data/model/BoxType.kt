package com.travelsketch.data.model

enum class BoxType(val type: String) {
    IMAGE("IMAGE"),
    VIDEO("VIDEO"),
    TEXT("TEXT"),
    RECORD("RECORD"),
    RECEIPT("RECEIPT");

    companion object {
        fun fromString(type: String): BoxType {
            return when (type) {
                "IMAGE" -> IMAGE
                "VIDEO" -> VIDEO
                "RECORD" -> RECORD
                "TEXT" -> TEXT
                "RECEIPT" -> RECEIPT
                else -> throw IllegalArgumentException("Unknown type: $type")
            }
        }
    }
}