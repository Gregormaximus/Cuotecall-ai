package com.kuote.agent.util

import android.content.Context
import android.provider.ContactsContract
import android.util.Log

object ContactCache {
    private val contacts = HashSet<String>()
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        
        try {
            val contentResolver = context.contentResolver
            val cursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                null, null, null
            )

            cursor?.use {
                val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                if (numberIndex >= 0) {
                    while (it.moveToNext()) {
                        val number = it.getString(numberIndex)
                        if (!number.isNullOrBlank()) {
                            val cleanNumber = number.replace(Regex("[^0-9]"), "")
                            if (cleanNumber.length >= 7) {
                                contacts.add(cleanNumber)
                            }
                        }
                    }
                }
            }
            isInitialized = true
            Log.d("ContactCache", "ContactCache initialized with ${contacts.size} contacts.")
        } catch (e: Exception) {
            Log.e("ContactCache", "Failed to query contacts safely", e)
        }
    }

    fun isContact(phoneNumber: String): Boolean {
        if (phoneNumber.isBlank()) return false
        val normalizedNumber = phoneNumber.replace(Regex("[^0-9]"), "")
        if (normalizedNumber.length < 7) return false

        return contacts.any { cachedNumber ->
            cachedNumber.length >= 7 && (normalizedNumber.endsWith(cachedNumber) || cachedNumber.endsWith(normalizedNumber))
        }
    }
}

