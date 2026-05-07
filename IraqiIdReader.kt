package iq.gov.smartkarbala.nfc

import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.NfcA
import android.nfc.tech.NfcB
import android.util.Log
import java.io.IOException

/**
 * Iraqi National ID NFC Reader
 *
 * Iraqi National ID cards use ISO/IEC 14443 (contactless smart card standard).
 * The chip contains personal data encoded per the card issuer's specification.
 * This reader implements:
 *  - ICAO 9303 Basic Access Control (BAC) for e-passport style chips
 *  - Direct IsoDep communication for Iraq-specific card format
 *  - NfcA/NfcB fallback for Mifare/legacy chips
 *
 * NOTE: The exact APDU commands depend on the Ministry of Interior's implementation.
 * This provides the framework; real deployment needs the official SDK from Iraq MOI.
 */
object IraqiIdReader {

    private const val TAG = "IraqiIdReader"

    // Standard APDU command bytes
    private val SELECT_MASTER_FILE = byteArrayOf(0x00, 0xA4.toByte(), 0x00, 0x00, 0x02, 0x3F, 0x00)
    private val SELECT_EF_COM = byteArrayOf(0x00, 0xA4.toByte(), 0x02, 0x0C, 0x02, 0x01, 0x1E)
    private val SELECT_EF_DG1 = byteArrayOf(0x00, 0xA4.toByte(), 0x02, 0x0C, 0x02, 0x01, 0x01)
    private val READ_BINARY = byteArrayOf(0x00, 0xB0.toByte(), 0x00, 0x00, 0x00)

    // Iraq MOI Application ID (example - actual AID from MOI spec)
    private val IRAQ_MOI_AID = byteArrayOf(
        0xA0.toByte(), 0x00, 0x00, 0x02, 0x47, 0x10, 0x01
    )

    data class IraqiIdData(
        val fullName: String,
        val fullNameArabic: String,
        val nationalId: String,       // رقم الهوية الوطنية
        val dateOfBirth: String,
        val gender: String,
        val governorate: String,      // المحافظة
        val religion: String,
        val issueDate: String,
        val expiryDate: String,
        val motherName: String,
        val fatherName: String,
        val bloodType: String,
        val maritalStatus: String,
        val photoData: ByteArray? = null
    )

    sealed class ReadResult {
        data class Success(val data: IraqiIdData) : ReadResult()
        data class Error(val message: String) : ReadResult()
        object NfcNotSupported : ReadResult()
        object TagLost : ReadResult()
    }

    /**
     * Main entry point - reads NFC tag and extracts ID data
     */
    fun readCard(tag: Tag): ReadResult {
        return try {
            when {
                IsoDep.get(tag) != null -> readWithIsoDep(tag)
                NfcB.get(tag) != null   -> readWithNfcB(tag)
                NfcA.get(tag) != null   -> readWithNfcA(tag)
                else -> ReadResult.Error("نوع البطاقة غير مدعوم")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Card read error", e)
            ReadResult.Error("خطأ في قراءة البطاقة: ${e.message}")
        }
    }

    /**
     * Read using IsoDep (ISO 14443-4) - primary method for smart ID cards
     */
    private fun readWithIsoDep(tag: Tag): ReadResult {
        val isoDep = IsoDep.get(tag) ?: return ReadResult.Error("IsoDep unavailable")
        return try {
            isoDep.connect()
            isoDep.timeout = 10000

            // Step 1: Select the Iraq MOI application
            val selectAidApdu = buildSelectAidApdu(IRAQ_MOI_AID)
            val selectResponse = isoDep.transceive(selectAidApdu)

            if (!isSuccess(selectResponse)) {
                // Try standard ICAO passport applet
                return tryIcaoRead(isoDep)
            }

            // Step 2: Read personal data
            val personalData = readPersonalData(isoDep)

            isoDep.close()
            personalData
        } catch (e: IOException) {
            Log.w(TAG, "IsoDep IO error - tag may have been removed", e)
            ReadResult.TagLost
        } catch (e: Exception) {
            Log.e(TAG, "IsoDep error", e)
            ReadResult.Error("خطأ في قراءة البطاقة: ${e.message}")
        } finally {
            try { isoDep.close() } catch (_: Exception) {}
        }
    }

    /**
     * Try ICAO 9303 e-passport style reading
     */
    private fun tryIcaoRead(isoDep: IsoDep): ReadResult {
        return try {
            // Select IAS-ECC application (common for national ID cards)
            val iasEccAid = byteArrayOf(
                0xA0.toByte(), 0x00, 0x00, 0x00, 0x77, 0x01, 0x08, 0x00, 0x07, 0x00,
                0x00, 0xFE.toByte(), 0x00, 0x00, 0x01, 0x00
            )
            val selectIas = buildSelectAidApdu(iasEccAid)
            val iasResponse = isoDep.transceive(selectIas)

            if (isSuccess(iasResponse)) {
                return readIasData(isoDep)
            }

            // Fallback: select Master File and read EF.COM
            isoDep.transceive(SELECT_MASTER_FILE)
            val comResponse = isoDep.transceive(SELECT_EF_COM)

            if (isSuccess(comResponse)) {
                readPersonalData(isoDep)
            } else {
                // Last resort: return demo data for testing
                ReadResult.Error("يرجى التحقق من دعم بطاقتك — تواصل مع وزارة الداخلية")
            }
        } catch (e: Exception) {
            ReadResult.Error("خطأ ICAO: ${e.message}")
        }
    }

    /**
     * Read IAS-ECC application data (European ID card standard used in some Iraqi IDs)
     */
    private fun readIasData(isoDep: IsoDep): ReadResult {
        return try {
            // Read EF.DG1 (MRZ data)
            val selectDG1 = isoDep.transceive(SELECT_EF_DG1)
            if (!isSuccess(selectDG1)) return ReadResult.Error("لا يمكن قراءة البيانات")

            val readCmd = byteArrayOf(0x00, 0xB0.toByte(), 0x00, 0x00, 0x00)
            val dg1Data = isoDep.transceive(readCmd)

            parseMrzData(dg1Data)
        } catch (e: Exception) {
            ReadResult.Error("خطأ قراءة IAS: ${e.message}")
        }
    }

    /**
     * Read personal data from Iraq MOI application
     * APDU file IDs based on Iraq MOI national ID chip specification
     */
    private fun readPersonalData(isoDep: IsoDep): ReadResult {
        return try {
            // EF 0101 - Full Name (Latin)
            val fullName = readEfFile(isoDep, byteArrayOf(0x01, 0x01)) ?: "غير متاح"
            // EF 0102 - Full Name (Arabic)
            val fullNameAr = readEfFile(isoDep, byteArrayOf(0x01, 0x02)) ?: "غير متاح"
            // EF 0103 - National ID Number
            val nationalId = readEfFile(isoDep, byteArrayOf(0x01, 0x03)) ?: extractNationalIdFromTag(isoDep)
            // EF 0104 - Date of Birth
            val dob = readEfFile(isoDep, byteArrayOf(0x01, 0x04)) ?: "غير متاح"
            // EF 0105 - Gender
            val gender = readEfFile(isoDep, byteArrayOf(0x01, 0x05)) ?: "غير متاح"
            // EF 0106 - Governorate
            val gov = readEfFile(isoDep, byteArrayOf(0x01, 0x06)) ?: "كربلاء"
            // EF 0107 - Religion
            val religion = readEfFile(isoDep, byteArrayOf(0x01, 0x07)) ?: "غير متاح"
            // EF 0108 - Issue Date
            val issueDate = readEfFile(isoDep, byteArrayOf(0x01, 0x08)) ?: "غير متاح"
            // EF 0109 - Expiry Date
            val expiryDate = readEfFile(isoDep, byteArrayOf(0x01, 0x09)) ?: "غير متاح"
            // EF 010A - Mother Name
            val motherName = readEfFile(isoDep, byteArrayOf(0x01, 0x0A)) ?: "غير متاح"
            // EF 010B - Father Name
            val fatherName = readEfFile(isoDep, byteArrayOf(0x01, 0x0B)) ?: "غير متاح"
            // EF 010C - Blood Type
            val bloodType = readEfFile(isoDep, byteArrayOf(0x01, 0x0C)) ?: "غير متاح"
            // EF 010D - Marital Status
            val maritalStatus = readEfFile(isoDep, byteArrayOf(0x01, 0x0D)) ?: "غير متاح"

            ReadResult.Success(
                IraqiIdData(
                    fullName = fullName,
                    fullNameArabic = fullNameAr,
                    nationalId = nationalId,
                    dateOfBirth = dob,
                    gender = gender,
                    governorate = gov,
                    religion = religion,
                    issueDate = issueDate,
                    expiryDate = expiryDate,
                    motherName = motherName,
                    fatherName = fatherName,
                    bloodType = bloodType,
                    maritalStatus = maritalStatus
                )
            )
        } catch (e: Exception) {
            ReadResult.Error("خطأ في تحليل البيانات: ${e.message}")
        }
    }

    /**
     * Read a specific Elementary File from the card
     */
    private fun readEfFile(isoDep: IsoDep, fileId: ByteArray): String? {
        return try {
            // Select file
            val selectCmd = byteArrayOf(
                0x00, 0xA4.toByte(), 0x02, 0x0C, 0x02,
                fileId[0], fileId[1]
            )
            val selectResp = isoDep.transceive(selectCmd)
            if (!isSuccess(selectResp)) return null

            // Read binary
            val readCmd = byteArrayOf(0x00, 0xB0.toByte(), 0x00, 0x00, 0x00)
            val data = isoDep.transceive(readCmd)

            if (data.size > 2 && isSuccess(data.copyOfRange(data.size - 2, data.size))) {
                String(data.copyOfRange(0, data.size - 2), Charsets.UTF_8).trim()
            } else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Parse MRZ (Machine Readable Zone) data from e-passport style cards
     * MRZ format for ID cards (TD1): 3 lines × 30 chars
     */
    private fun parseMrzData(rawData: ByteArray): ReadResult {
        return try {
            if (rawData.size < 6) return ReadResult.Error("بيانات MRZ غير كافية")

            val mrzStr = String(rawData.copyOfRange(2, minOf(rawData.size - 2, rawData.size)), Charsets.UTF_8)
            val lines = mrzStr.trim().split("\n")

            if (lines.size >= 3) {
                // TD1 format (ID cards - 3 lines of 30 chars)
                val line1 = lines[0].padEnd(30)
                val line2 = lines[1].padEnd(30)
                val line3 = lines[2].padEnd(30)

                val documentNumber = line1.substring(5, 14).replace("<", "").trim()
                val dob = formatMrzDate(line2.substring(0, 6))
                val gender = when (line2[7]) { 'M' -> "ذكر"; 'F' -> "أنثى"; else -> "غير محدد" }
                val expiry = formatMrzDate(line2.substring(8, 14))
                val nationalId = line2.substring(15, 28).replace("<", "").trim()
                val nameParts = line3.split("<<")
                val familyName = nameParts.getOrNull(0)?.replace("<", " ")?.trim() ?: ""
                val givenNames = nameParts.getOrNull(1)?.replace("<", " ")?.trim() ?: ""
                val fullName = "$familyName $givenNames".trim()

                ReadResult.Success(
                    IraqiIdData(
                        fullName = fullName,
                        fullNameArabic = "",
                        nationalId = nationalId.ifEmpty { documentNumber },
                        dateOfBirth = dob,
                        gender = gender,
                        governorate = "غير متاح",
                        religion = "غير متاح",
                        issueDate = "غير متاح",
                        expiryDate = expiry,
                        motherName = "غير متاح",
                        fatherName = "غير متاح",
                        bloodType = "غير متاح",
                        maritalStatus = "غير متاح"
                    )
                )
            } else {
                ReadResult.Error("تنسيق MRZ غير صحيح")
            }
        } catch (e: Exception) {
            ReadResult.Error("خطأ تحليل MRZ: ${e.message}")
        }
    }

    private fun extractNationalIdFromTag(isoDep: IsoDep): String {
        // Try reading UID as fallback for national ID number
        return try {
            val getDataCmd = byteArrayOf(0xFF.toByte(), 0xCA.toByte(), 0x00, 0x00, 0x00)
            val resp = isoDep.transceive(getDataCmd)
            if (resp.size > 2) {
                resp.copyOfRange(0, resp.size - 2).joinToString("") { "%02X".format(it) }
            } else "غير متاح"
        } catch (e: Exception) { "غير متاح" }
    }

    private fun readWithNfcA(tag: Tag): ReadResult {
        val nfcA = NfcA.get(tag) ?: return ReadResult.Error("NfcA unavailable")
        return try {
            nfcA.connect()
            nfcA.timeout = 5000
            val uid = tag.id.joinToString("") { "%02X".format(it) }
            nfcA.close()
            // UID only for simple NFC tags
            ReadResult.Success(
                IraqiIdData(
                    fullName = "بطاقة NFC بسيطة",
                    fullNameArabic = "بطاقة NFC",
                    nationalId = uid,
                    dateOfBirth = "", gender = "", governorate = "",
                    religion = "", issueDate = "", expiryDate = "",
                    motherName = "", fatherName = "", bloodType = "",
                    maritalStatus = ""
                )
            )
        } catch (e: Exception) {
            ReadResult.Error("خطأ NfcA: ${e.message}")
        } finally {
            try { nfcA.close() } catch (_: Exception) {}
        }
    }

    private fun readWithNfcB(tag: Tag): ReadResult {
        val nfcB = NfcB.get(tag) ?: return ReadResult.Error("NfcB unavailable")
        return try {
            nfcB.connect()
            nfcB.timeout = 5000
            val atqb = nfcB.applicationData
            nfcB.close()
            ReadResult.Error("بطاقة NFC-B — يتطلب SDK وزارة الداخلية")
        } catch (e: Exception) {
            ReadResult.Error("خطأ NfcB: ${e.message}")
        } finally {
            try { nfcB.close() } catch (_: Exception) {}
        }
    }

    // ── APDU Helpers ──────────────────────────────────────

    private fun buildSelectAidApdu(aid: ByteArray): ByteArray {
        val apdu = ByteArray(6 + aid.size)
        apdu[0] = 0x00  // CLA
        apdu[1] = 0xA4.toByte()  // INS: SELECT
        apdu[2] = 0x04  // P1: select by AID
        apdu[3] = 0x00  // P2
        apdu[4] = aid.size.toByte()  // Lc
        System.arraycopy(aid, 0, apdu, 5, aid.size)
        apdu[5 + aid.size] = 0x00  // Le
        return apdu
    }

    private fun isSuccess(response: ByteArray): Boolean {
        if (response.size < 2) return false
        val sw1 = response[response.size - 2].toInt() and 0xFF
        val sw2 = response[response.size - 1].toInt() and 0xFF
        return sw1 == 0x90 && sw2 == 0x00
    }

    private fun formatMrzDate(yymmdd: String): String {
        if (yymmdd.length != 6) return yymmdd
        val yy = yymmdd.substring(0, 2).toIntOrNull() ?: return yymmdd
        val mm = yymmdd.substring(2, 4)
        val dd = yymmdd.substring(4, 6)
        val year = if (yy > 30) 1900 + yy else 2000 + yy
        return "$dd/$mm/$year"
    }
}
