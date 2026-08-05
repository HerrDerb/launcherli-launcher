package com.herrderb.launcherli.data

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ContactInfo(
    val contactId: Long,
    val lookupKey: String,
    val displayName: String,
    val phoneNumber: String?,
    val whatsAppDataId: Long?
)

class ContactsRepository(private val context: Context) {

    companion object {
        private const val WHATSAPP_PROFILE_MIMETYPE =
            "vnd.android.cursor.item/vnd.com.whatsapp.profile"
        private const val MAX_RESULTS = 8
    }

    /**
     * Returns contacts matching [query] by name, or an empty list when the
     * query is blank or READ_CONTACTS is not (or no longer) granted — the
     * drawer degrades silently, same as StationLocator does for location.
     */
    suspend fun search(query: String): List<ContactInfo> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return@withContext emptyList()
        }
        try {
            val matches = queryMatchingContacts(query)
            if (matches.isEmpty()) return@withContext emptyList()

            val details = queryDetails(matches.map { it.contactId })
            matches.map { contact ->
                contact.copy(
                    phoneNumber = details[contact.contactId]?.phoneNumber,
                    whatsAppDataId = details[contact.contactId]?.whatsAppDataId
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun queryMatchingContacts(query: String): List<ContactInfo> {
        val uri = Uri.withAppendedPath(
            ContactsContract.Contacts.CONTENT_FILTER_URI,
            Uri.encode(query)
        )
        val results = mutableListOf<ContactInfo>()
        context.contentResolver.query(
            uri,
            arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.LOOKUP_KEY,
                ContactsContract.Contacts.DISPLAY_NAME_PRIMARY
            ),
            null,
            null,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY
        )?.use { cursor ->
            while (cursor.moveToNext() && results.size < MAX_RESULTS) {
                val name = cursor.getString(2) ?: continue
                results.add(
                    ContactInfo(
                        contactId = cursor.getLong(0),
                        lookupKey = cursor.getString(1),
                        displayName = name,
                        phoneNumber = null,
                        whatsAppDataId = null
                    )
                )
            }
        }
        return results
    }

    private data class ContactDetails(
        var phoneNumber: String? = null,
        var whatsAppDataId: Long? = null
    )

    /**
     * One batched Data-table query for phone numbers and WhatsApp profile rows
     * across all matched contacts. Sorted so the (super-)primary phone number
     * comes first per contact; first row per kind wins.
     */
    private fun queryDetails(contactIds: List<Long>): Map<Long, ContactDetails> {
        val details = mutableMapOf<Long, ContactDetails>()
        val placeholders = contactIds.joinToString(",") { "?" }
        context.contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(
                ContactsContract.Data._ID,
                ContactsContract.Data.CONTACT_ID,
                ContactsContract.Data.MIMETYPE,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            "${ContactsContract.Data.CONTACT_ID} IN ($placeholders) AND " +
                "${ContactsContract.Data.MIMETYPE} IN (?, ?)",
            (contactIds.map(Long::toString) + listOf(
                ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE,
                WHATSAPP_PROFILE_MIMETYPE
            )).toTypedArray(),
            "${ContactsContract.Data.IS_SUPER_PRIMARY} DESC, ${ContactsContract.Data.IS_PRIMARY} DESC"
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val contactId = cursor.getLong(1)
                val entry = details.getOrPut(contactId) { ContactDetails() }
                when (cursor.getString(2)) {
                    ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE ->
                        if (entry.phoneNumber == null) entry.phoneNumber = cursor.getString(3)
                    WHATSAPP_PROFILE_MIMETYPE ->
                        if (entry.whatsAppDataId == null) entry.whatsAppDataId = cursor.getLong(0)
                }
            }
        }
        return details
    }

    fun dial(number: String) {
        startSafely(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
    }

    fun sms(number: String) {
        startSafely(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$number")))
    }

    /**
     * Opens the contact's WhatsApp chat by viewing its WhatsApp Data row —
     * the same mechanism the stock Contacts app uses for third-party actions.
     */
    fun openWhatsApp(dataId: Long) {
        startSafely(
            Intent(Intent.ACTION_VIEW).setDataAndType(
                ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, dataId),
                WHATSAPP_PROFILE_MIMETYPE
            )
        )
    }

    fun openContactCard(contact: ContactInfo) {
        startSafely(
            Intent(
                Intent.ACTION_VIEW,
                ContactsContract.Contacts.getLookupUri(contact.contactId, contact.lookupKey)
            )
        )
    }

    private fun startSafely(intent: Intent) {
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }
}
