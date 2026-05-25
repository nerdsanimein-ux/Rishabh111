package com.rishabh.btgamepad.hid

object HidConstants {
    // Report ID 1 as declared in the HID descriptor
    const val REPORT_ID: Int = 1
    const val REPORT_SIZE_BYTES = 7

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

    // Hat switch values — 4-bit field, 0-7 valid, 0xF = null / centered
    const val DPAD_CENTERED:   Byte = 0x0F.toByte()   // null state
    const val DPAD_UP:         Byte = 0
    const val DPAD_UP_RIGHT:   Byte = 1
    const val DPAD_RIGHT:      Byte = 2
    const val DPAD_DOWN_RIGHT: Byte = 3
    const val DPAD_DOWN:       Byte = 4
    const val DPAD_DOWN_LEFT:  Byte = 5
    const val DPAD_LEFT:       Byte = 6
    const val DPAD_UP_LEFT:    Byte = 7

    // Unsigned axes: center = 128, min = 0, max = 255
    const val AXIS_CENTER: Byte = 128.toByte()
}
