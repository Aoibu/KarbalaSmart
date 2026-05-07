package iq.gov.smartkarbala.nfc

import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.util.Log

/**
 * EMV Credit/Debit Card NFC Reader
 *
 * Reads contactless payment cards using EMV standard APDU commands.
 * Extracts: card number (PAN), expiry date, cardholder name, card type.
 *
 * Supported: Visa, Mastercard, AMEX, Mada, Iraqi bank cards
 */
object CreditCardReader {

    private const val TAG = "CreditCardReader"

    // EMV Application IDs
    private val VISA_AID        = byteArrayOf(0xA0.toByte(), 0x00, 0x00, 0x00, 0x03, 0x10, 0x10)
    private val MASTERCARD_AID  = byteArrayOf(0xA0.toByte(), 0x00, 0x00, 0x00, 0x04, 0x10, 0x10)
    private val AMEX_AID        = byteArrayOf(0xA0.toByte(), 0x00, 0x00, 0x00, 0x25, 0x01, 0x01)
    private val MADA_AID        = byteArrayOf(0xA0.toByte(), 0x00, 0x00, 0x00, 0x04, 0x30, 0x60)
    private val PAYPASS_AID     = byteArrayOf(0xA0.toByte(), 0x00, 0x00, 0x00, 0x04, 0x10, 0x10)

    // PSE (Payment System Environment)
    private val PPSE_AID = "2PAY.SYS.DDF01".toByteArray(Charsets.US_ASCII)

    data class CardData(
        val pan: String,           // Card number (masked: **** **** **** 1234)
        val panFull: String,       // Full PAN (stored encrypted)
        val expiryDate: String,    // MM/YY
        val cardholderName: String,
        val cardType: CardType,
        val bankName: String,
        val currency: String = "IQD"
    )

    enum class CardType {
        VISA, MASTERCARD, AMEX, MADA, IRAQI_BANK, UNKNOWN
    }

    sealed class ReadResult {
        data class Success(val card: CardData) : ReadResult()
        data class Error(val message: String) : ReadResult()
        object TagLost : ReadResult()
    }

    fun readCard(tag: Tag): ReadResult {
        val isoDep = IsoDep.get(tag) ?: return ReadResult.Error("بطاقة غير مدعومة")
        return try {
            isoDep.connect()
            isoDep.timeout = 10000

            // Try each card type
            val result = tryReadCard(isoDep)
            isoDep.close()
            result
        } catch (e: Exception) {
            Log.e(TAG, "Card read error", e)
            ReadResult.Error("تعذّر قراءة البطاقة")
        } finally {
            try { isoDep.close() } catch (_: Exception) {}
        }
    }

    private fun tryReadCard(isoDep: IsoDep): ReadResult {
        // Try PPSE first (selects the appropriate payment application)
        val ppseResult = tryPpse(isoDep)
        if (ppseResult is ReadResult.Success) return ppseResult

        // Try each AID directly
        val aids = listOf(
            VISA_AID to CardType.VISA,
            MASTERCARD_AID to CardType.MASTERCARD,
            AMEX_AID to CardType.AMEX,
            MADA_AID to CardType.MADA
        )

        for ((aid, type) in aids) {
            val result = tryAid(isoDep, aid, type)
            if (result is ReadResult.Success) return result
        }

        return ReadResult.Error("لم يتم التعرف على نوع البطاقة")
    }

    private fun tryPpse(isoDep: IsoDep): ReadResult {
        return try {
            val selectPpse = byteArrayOf(
                0x00, 0xA4.toByte(), 0x04, 0x00,
                PPSE_AID.size.toByte()
            ) + PPSE_AID + byteArrayOf(0x00)

            val response = isoDep.transceive(selectPpse)
            if (!isSuccess(response)) return ReadResult.Error("PPSE not supported")

            // Parse PPSE response to find the right AID
            val aidFromPpse = parsePpseResponse(response)
            if (aidFromPpse != null) {
                val cardType = detectCardType(aidFromPpse)
                tryAid(isoDep, aidFromPpse, cardType)
            } else {
                ReadResult.Error("No AID in PPSE")
            }
        } catch (e: Exception) {
            ReadResult.Error("PPSE error: ${e.message}")
        }
    }

    private fun tryAid(isoDep: IsoDep, aid: ByteArray, expectedType: CardType): ReadResult {
        return try {
            // SELECT APPLICATION
            val selectCmd = byteArrayOf(
                0x00, 0xA4.toByte(), 0x04, 0x00,
                aid.size.toByte()
            ) + aid + byteArrayOf(0x00)

            val selectResp = isoDep.transceive(selectCmd)
            if (!isSuccess(selectResp)) return ReadResult.Error("AID not found")

            // GET PROCESSING OPTIONS (GPO)
            val gpoCmd = byteArrayOf(
                0x80.toByte(), 0xA8.toByte(), 0x00, 0x00, 0x02,
                0x83.toByte(), 0x00, 0x00
            )
            val gpoResp = isoDep.transceive(gpoCmd)

            // READ RECORDS - SFI 1, Record 1
            val readRecord = byteArrayOf(
                0x00, 0xB2.toByte(), 0x01, 0x0C, 0x00
            )
            val recordResp = isoDep.transceive(readRecord)

            // Parse EMV record data
            val tlvData = parseTlvData(recordResp)

            val pan = tlvData["5A"] ?: tlvData["57"]?.let { extractPanFromTrack2(it) }
                ?: return ReadResult.Error("رقم البطاقة غير متاح")

            val expiry = tlvData["5F24"]?.let { formatExpiry(it) }
                ?: extractExpiryFromTrack2(tlvData["57"] ?: "")

            val name = tlvData["5F20"] ?: ""
            val cardType = if (expectedType != CardType.UNKNOWN) expectedType
                           else detectCardTypeFromPan(pan)

            ReadResult.Success(
                CardData(
                    pan = maskPan(pan),
                    panFull = pan,
                    expiryDate = expiry ?: "غير متاح",
                    cardholderName = formatCardholderName(name),
                    cardType = cardType,
                    bankName = detectBankName(pan)
                )
            )
        } catch (e: Exception) {
            ReadResult.Error("خطأ AID: ${e.message}")
        }
    }

    // ── TLV Parser ────────────────────────────────────────

    private fun parseTlvData(data: ByteArray): Map<String, String> {
        val result = mutableMapOf<String, String>()
        var i = 0
        while (i < data.size - 2) { // -2 for SW
            val tag: String
            val tagByte = data[i].toInt() and 0xFF
            if ((tagByte and 0x1F) == 0x1F) {
                // Two-byte tag
                if (i + 1 >= data.size - 2) break
                tag = "%02X%02X".format(tagByte, data[i + 1].toInt() and 0xFF)
                i += 2
            } else {
                tag = "%02X".format(tagByte)
                i++
            }
            if (i >= data.size - 2) break
            val length = data[i].toInt() and 0xFF
            i++
            if (i + length > data.size - 2) break
            val value = data.copyOfRange(i, i + length)
            result[tag] = value.joinToString("") { "%02X".format(it) }
            i += length
        }
        return result
    }

    private fun parsePpseResponse(data: ByteArray): ByteArray? {
        // Find 4F (AID) tag in PPSE response
        var i = 0
        while (i < data.size - 2) {
            if (data[i].toInt() and 0xFF == 0x4F) {
                i++
                if (i >= data.size) break
                val len = data[i].toInt() and 0xFF
                i++
                if (i + len <= data.size - 2) {
                    return data.copyOfRange(i, i + len)
                }
            }
            i++
        }
        return null
    }

    private fun extractPanFromTrack2(track2Hex: String): String? {
        val track2 = track2Hex.replace(" ", "")
        val dPos = track2.indexOf('D').takeIf { it > 0 }
            ?: track2.indexOf('=').takeIf { it > 0 }
        return if (dPos != null) track2.substring(0, dPos) else null
    }

    private fun extractExpiryFromTrack2(track2Hex: String): String? {
        val sepPos = track2Hex.indexOfFirst { it == 'D' || it == '=' }
        return if (sepPos >= 0 && sepPos + 4 < track2Hex.length) {
            val yymm = track2Hex.substring(sepPos + 1, sepPos + 5)
            "${yymm.substring(2, 4)}/${yymm.substring(0, 2)}"
        } else null
    }

    private fun formatExpiry(expiryHex: String): String? {
        return if (expiryHex.length >= 4) {
            val yy = expiryHex.substring(0, 2)
            val mm = expiryHex.substring(2, 4)
            "$mm/$yy"
        } else null
    }

    private fun maskPan(pan: String): String {
        if (pan.length < 8) return pan
        val first4 = pan.substring(0, 4)
        val last4 = pan.substring(pan.length - 4)
        val middle = "*".repeat(pan.length - 8)
        return "$first4 $middle $last4".replace(Regex("(\\S{4})")) { "${it.value} " }.trim()
    }

    private fun formatCardholderName(raw: String): String {
        if (raw.isEmpty()) return "صاحب البطاقة"
        val parts = raw.trim().split("/", "<")
        return parts.reversed().joinToString(" ").trim()
            .lowercase().split(" ")
            .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
    }

    private fun detectCardType(aid: ByteArray): CardType {
        val aidHex = aid.joinToString("") { "%02X".format(it) }
        return when {
            aidHex.startsWith("A0000000031010") -> CardType.VISA
            aidHex.startsWith("A0000000041010") -> CardType.MASTERCARD
            aidHex.startsWith("A0000000250101") -> CardType.AMEX
            aidHex.startsWith("A0000000043060") -> CardType.MADA
            else -> CardType.UNKNOWN
        }
    }

    private fun detectCardTypeFromPan(pan: String): CardType {
        return when {
            pan.startsWith("4") -> CardType.VISA
            pan.startsWith("5") || pan.startsWith("2") -> CardType.MASTERCARD
            pan.startsWith("3") -> CardType.AMEX
            else -> CardType.IRAQI_BANK
        }
    }

    private fun detectBankName(pan: String): String {
        // Iraqi bank BINs (first 6 digits)
        if (pan.length < 6) return "بنك"
        return when (pan.substring(0, 6)) {
            "418114" -> "بنك الرافدين"
            "449266" -> "بنك الرشيد"
            "512345" -> "المصرف التجاري العراقي"
            "465784" -> "بنك بغداد"
            "421234" -> "بنك التعاون"
            else -> when {
                pan.startsWith("4") -> "Visa"
                pan.startsWith("5") -> "Mastercard"
                else -> "بنك"
            }
        }
    }

    private fun isSuccess(response: ByteArray): Boolean {
        if (response.size < 2) return false
        val sw1 = response[response.size - 2].toInt() and 0xFF
        val sw2 = response[response.size - 1].toInt() and 0xFF
        return (sw1 == 0x90 && sw2 == 0x00) || sw1 == 0x61
    }
}
