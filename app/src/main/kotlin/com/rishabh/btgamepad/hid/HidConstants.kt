package com.rishabh.btgamepad.hid

object HidConstants {
    // No Report ID in descriptor → pass 0 to sendReport / replyReport
    const val REPORT_ID: Int = 0
    const val REPORT_SIZE_BYTES = 8

    // Button bitmasks — single 16-bit field, bytes 0-1 of report
    const val BTN_A      = 1 shl 0   // 0x0001
    const val BTN_B      = 1 shl 1   // 0x0002
    const val BTN_X      = 1 shl 2   // 0x0004
    const val BTN_Y      = 1 shl 3   // 0x0008
    const val BTN_L1     = 1 shl 4   // 0x0010
    const val BTN_R1     = 1 shl 5   // 0x0020
    const val BTN_L2     = 1 shl 6   // 0x0040
    const val BTN_R2     = 1 shl 7   // 0x0080
    const val BTN_START  = 1 shl 8   // 0x0100
    const val BTN_SELECT = 1 shl 9   // 0x0200

    // Hat switch values — 0-7 valid, any value > 7 = null / centered
    const val DPAD_CENTERED:   Byte = 0xFF.toByte()   // null state
    const val DPAD_UP:         Byte = 0
    const val DPAD_UP_RIGHT:   Byte = 1
    const val DPAD_RIGHT:      Byte = 2
    const val DPAD_DOWN_RIGHT: Byte = 3
    const val DPAD_DOWN:       Byte = 4
    const val DPAD_DOWN_LEFT:  Byte = 5
    const val DPAD_LEFT:       Byte = 6
    const val DPAD_UP_LEFT:    Byte = 7

    // Signed axes: center = 0, min = -127, max = 127
    const val AXIS_CENTER: Byte = 0
}
