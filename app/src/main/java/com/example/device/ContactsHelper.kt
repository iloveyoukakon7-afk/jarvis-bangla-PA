package com.example.device

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import android.telephony.PhoneNumberUtils
import androidx.core.content.ContextCompat
import com.example.data.DeviceContact

object ContactsHelper {

    fun loadContacts(context: Context): List<DeviceContact> {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return emptyList()
        }

        val contacts = linkedMapOf<String, DeviceContact>()
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )

        runCatching {
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )?.use { cursor ->
                val nameIdx =
                    cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numIdx =
                    cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (cursor.moveToNext()) {
                    val name = cursor.getString(nameIdx)?.trim().orEmpty()
                    val number = cursor.getString(numIdx)?.trim().orEmpty()
                    if (name.isNotEmpty() && number.isNotEmpty() && !contacts.containsKey(name)) {
                        contacts[name] = DeviceContact(name = name, phoneNumber = number)
                    }
                }
            }
        }
        return contacts.values.toList()
    }

    fun findBestContact(contacts: List<DeviceContact>, query: String): DeviceContact? {
        if (query.isBlank() || contacts.isEmpty()) return null
        val normalizedQuery = query.trim().lowercase()

        // 1. Exact match
        contacts.firstOrNull { it.name.lowercase() == normalizedQuery }?.let { return it }
        // 2. StartsWith or Contains match
        contacts.firstOrNull { it.name.lowercase().contains(normalizedQuery) }?.let { return it }
        contacts.firstOrNull { normalizedQuery.contains(it.name.lowercase()) }?.let { return it }
        // 3. Token overlap
        val tokens = normalizedQuery.split(" ").filter { it.length >= 2 }
        if (tokens.isNotEmpty()) {
            contacts.firstOrNull { contact ->
                val lower = contact.name.lowercase()
                tokens.any { lower.contains(it) }
            }?.let { return it }
        }
        return null
    }

    fun lookupContactNameByNumber(context: Context, incomingNumber: String): String {
        if (incomingNumber.isBlank()) return ""
        val contacts = loadContacts(context)
        return contacts.firstOrNull {
            @Suppress("DEPRECATION")
            PhoneNumberUtils.compare(it.phoneNumber, incomingNumber)
        }?.name.orEmpty()
    }
}
