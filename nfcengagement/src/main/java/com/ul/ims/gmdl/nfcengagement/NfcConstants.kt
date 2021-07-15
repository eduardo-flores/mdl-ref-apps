/*
 * Copyright (C) 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package com.ul.ims.gmdl.nfcengagement

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

class NfcConstants {
    companion object {
        val statusWordOK: ByteArray = byteArrayOfInts(0x90, 0x00)
        val statusWordInstructionNotSupported: ByteArray = byteArrayOfInts(0x6D, 0x00)
        val statusWordFileNotFound: ByteArray = byteArrayOfInts(0x6A, 0x82)
        val statusWordEndOfFileReached: ByteArray = byteArrayOfInts(0x62, 0x82)
        val statusWordWrongParameters: ByteArray = byteArrayOfInts(0x6B, 0x00)

        private const val hsRecordTNF: Short = 0x01
        private val hsRecordType: ByteArray = byteArrayOfInts(0x48, 0x73)
        private val hsRecordId: ByteArray? = null
        private val hsRecordPayload: ByteArray = byteArrayOfInts(
            // ac record
            0x14, // version 1.4
            0xD1, // record header, TNF = 001 (Well known type)
            0x02, // record type length = 2 bytes
            0x08, // payload length = 8 bytes
            0x61, 0x63, // record type = "ac"
            0x01, // carrier flags. CPS = 1 "active"
            0x01, // carrier data reference length = 1
            0x30, // carrier data reference = "0"
            0x01, // auxiliary data reference count: 1
            0x03, // auxiliary data reference length = 1
            // auxiliary reference = "mDL"
            0x6D, 0x44, 0x4C
        )
        private val hsRecordPayloadWifi: ByteArray = byteArrayOfInts(
            // ac record
            0x14, // version 1.4
            0xD1, // record header, TNF = 001 (Well known type)
            0x02, // record type length = 2 bytes
            0x08, // payload length = 8 bytes
            0x61, 0x63, // record type = "ac"
            0x01, // carrier flags. CPS = 1 "active"
            0x01, // carrier data reference length = 1
            0x57, // carrier data reference = "W"
            0x01, // auxiliary data reference count: 1
            0x03, // auxiliary data reference length = 1
            // auxiliary reference = "mDL"
            0x6D, 0x44, 0x4C
        )
        private val hsRecordPayloadNfc: ByteArray = byteArrayOfInts(
            // ac record
            0x14, // version 1.4
            0xD1, // NDEF Record Header: MB=1b, ME=1b, CF=0b, SR=1, IL=0b, TNF=001b
            0x02, // record type length = 2 bytes
            0x0A, // payload length = 10 bytes
            0x61, 0x63, // record type = "ac"
            0x01, // carrier flags. CPS = 1 "active"
            0x03, // carrier data reference length = 3 octets
            0x6E, 0x66, 0x63, // carrier data reference = "nfc"
            0x01, // auxiliary data reference count: 1
            0x03, // auxiliary data reference length = 1
            // auxiliary reference = "mDL"
            0x6D, 0x44, 0x4C
        )

        private const val bluetoothLERecordTNF: Short = 0x02 // type = RFC 2046 (MIME)

        // type name = "application/vnd.bluetooth.le.oob"
        private val bluetoothLERecordType: ByteArray =
            "application/vnd.bluetooth.le.oob".toByteArray()
        private val bluetoothLERecordId: ByteArray = "0".toByteArray()

        // Wifi Aware Carrier Data Record
        private const val wifiAwareRecordTNF: Short = 0x02 // type = RFC 2046 (MIME)
        private val wifiAwareRecordType = "application/vnd.wfa.nan".toByteArray()
        private val wifiAwareRecordId = "W".toByteArray()

        // Carrier Data Record, “nfc”
        private const val nfcRecordTNF: Short = 0x02 // type = RFC 2046 (MIME)
        private val nfcRecordType = "iso.org:18013".toByteArray()
        private val nfcRecordId = "nfc".toByteArray()

        private const val deviceEngagementTNF: Short = 0x04 // type = external

        // type name = "iso.org:18013:deviceengagement"
        private val deviceEngagementType: ByteArray = "iso.org:18013:deviceengagement".toByteArray()

        // id = "mDL"
        private val deviceEngagementId: ByteArray = byteArrayOfInts(0x6D, 0x44, 0x4C)

        private val hcRecord = NdefRecord(hsRecordTNF, hsRecordType, hsRecordId, hsRecordPayload)
        private val hcRecordWifi =
            NdefRecord(hsRecordTNF, hsRecordType, hsRecordId, hsRecordPayloadWifi)
        private val hcRecordNfc =
            NdefRecord(hsRecordTNF, hsRecordType, hsRecordId, hsRecordPayloadNfc)

        fun createBLEStaticHandoverRecord(
            deviceEngagementPayload: ByteArray,
            blePeripheralMode: Boolean,
            bleCentralMode: Boolean,
            bleUUID: UUID?
        ): NdefMessage {

            val bluetoothLEPayload: MutableList<Byte>

            // both modes are supported
            if (blePeripheralMode && bleCentralMode) {
                // When the mDL supports both modes, the mDL reader should act as BLE central mode.
                bluetoothLEPayload = mutableListOf(
                    0x02, // LE Role length = 2
                    0x1C, // LE Role data type
                    0x02  // Peripheral and Central Role supported, Peripheral Role preferred for connection establishment
                )
            } else {
                // only central client mode supported
                bluetoothLEPayload = if (bleCentralMode) {
                    mutableListOf(
                        0x02, // LE Role length = 2
                        0x1C, // LE Role data type
                        0x01  // Central mode only
                    )
                } else {
                    // only peripheral server mode supported
                    mutableListOf(
                        0x02, // LE Role length = 2
                        0x1C, // LE Role data type
                        0x00  // Peripheral mode only
                    )
                }
            }

            // Added Ble UUID to static handover
            bleUUID?.let { uuid ->
                val data: ByteBuffer = ByteBuffer.allocate(16)
                data.order(ByteOrder.BIG_ENDIAN)
                data.putLong(0, uuid.mostSignificantBits)
                data.putLong(8, uuid.leastSignificantBits)

                bluetoothLEPayload.addAll(data.array().toList())
            }

            val bluetoothLERecord = NdefRecord(
                bluetoothLERecordTNF,
                bluetoothLERecordType,
                bluetoothLERecordId,
                bluetoothLEPayload.toByteArray()
            )

            val deviceEngagementRecord = NdefRecord(
                deviceEngagementTNF,
                deviceEngagementType,
                deviceEngagementId,
                deviceEngagementPayload
            )

            return NdefMessage(arrayOf(hcRecord, bluetoothLERecord, deviceEngagementRecord))
        }

        fun createWiFiAwareStaticHandoverRecord(
            deviceEngagementPayload: ByteArray,
            passPhrase: String?,
            supportedBands: ByteArray?
        ): NdefMessage {

            //Cipher Suite Info
            val payloadCipherSuite = listOf<Byte>(
                0x02, // Length 2 octets
                0x01, // Data Type 0x01 - Cipher Suite Info
                0x01 // Cipher Suite ID Info (1 – NCS-SK-128 Cipher Suite)
            )

            //Password Info
            val payloadPasswordInfo = mutableListOf<Byte>()
            if (passPhrase != null) {
                payloadPasswordInfo.addAll(
                    listOf(
                        0x21, // Length 33 octets
                        0x03 // Data Type 0x03 - Password Info
                    )
                )
                payloadPasswordInfo.addAll(passPhrase.toByteArray(Charsets.UTF_8).toList())
            } else {
                payloadPasswordInfo.addAll(
                    listOf(
                        0x01, // Length 33 octets
                        0x03 // Data Type 0x03 - Password Info
                    )
                )
            }

            //Band Info
            val payloadBandinfo = if (supportedBands != null && supportedBands.isNotEmpty())
                listOf<Byte>(
                    0x02, // Length 2 octets
                    0x04, // Data Type 0x04 - Band Info
                    supportedBands[0]  // Bit 2: 2.4 GHz + Bit 4: 4.9 and 5 GHz
                )
            else
                listOf<Byte>(
                    0x02, // Length 2 octets
                    0x04, // Data Type 0x04 - Band Info
                    0x14  // Bit 2: 2.4 GHz as default
                )

            val payload = mutableListOf<Byte>()
            payload.addAll(payloadCipherSuite)
            payload.addAll(payloadPasswordInfo)
            payload.addAll(payloadBandinfo)

            val wifiAwareRecord = NdefRecord(
                wifiAwareRecordTNF,
                wifiAwareRecordType,
                wifiAwareRecordId,
                payload.toByteArray()
            )

            val deviceEngagementRecord = NdefRecord(
                deviceEngagementTNF,
                deviceEngagementType,
                deviceEngagementId,
                deviceEngagementPayload
            )

            return NdefMessage(arrayOf(hcRecordWifi, wifiAwareRecord, deviceEngagementRecord))
        }

        fun createNfcStaticHandoverRecord(
            deviceEngagementPayload: ByteArray
        ): NdefMessage {

            val payload = byteArrayOf(
                0x10, //mDL NFC Connection Handover Version. Major Version: 1, Minor Version: 0
                // Maximum length of command data field supported by mobile device, as defined in ISO/IEC 7816-4.
                // NOTE: a value over 255 bytes indicates extended length. Indicated as an unsigned integer.
                //0x00, 0xFF.toByte()
                0xFF.toByte(), 0xFF.toByte()
                //TODO(Eduardo): Extended apdu data length hard-coded, it is needed an Android API
                // to retrieve this information
            )

            val nfcRecord = NdefRecord(
                nfcRecordTNF,
                nfcRecordType,
                nfcRecordId,
                payload
            )

            val deviceEngagementRecord = NdefRecord(
                deviceEngagementTNF,
                deviceEngagementType,
                deviceEngagementId,
                deviceEngagementPayload
            )

            return NdefMessage(arrayOf(hcRecordNfc, nfcRecord, deviceEngagementRecord))
        }
    }
}