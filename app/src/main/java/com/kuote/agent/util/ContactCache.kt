package com.kuote.agent.util

import android.content.Context
import android.provider.ContactsContract

object ContactCache {
    private val contacts = HashSet<String>()
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        
        val contentResolver = context.contentResolver
        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
            null, null, null
        )

        cursor?.use {
            val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (it.moveToNext()) {
                val number = it.getString(numberIndex)
                // Normalize number if necessary, simple approach: remove non-digits
                if (number != null) {
                    contacts.add(number.replace(Regex("[^0-9]"), ""))
                }
            }
        }
        isInitialized = true
    }

    fun isContact(phoneNumber: String): Boolean {
        // Normalize the input number to match cache format
        val normalizedNumber = phoneNumber.replace(Regex("[^0-9]"), "")
        // Check if normalized number ends with the cached number (simple approach for country code handling)
        return contacts.any { cachedNumber ->
            normalizedNumber.endsWith(cachedNumber) || cachedNumber.endsWith(normalizedNumber)
        }
    }
}
