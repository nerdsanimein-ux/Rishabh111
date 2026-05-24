package com.rishabh.btgamepad.hid

object HidConstants {
    const val REPORT_ID: Byte = 1
    const val REPORT_SIZE_BYTES = 8

    const val AXIS_LX_IDX = 0
    const val AXIS_LY_IDX = 1
    const val AXIS_RX_IDX = 2
    const val AXIS_RY_IDX = 3

    // D-pad hat switch values (4 bits: 0x0–0x7, 0xF = centered)
    const val DPAD_CENTERED: Byte = 0x0F
    const val DPAD_UP: Byte = 0x00
    const val DPAD_UP_RIGHT: Byte = 0x01
    const val DPAD_RIGHT: Byte = 0x02
    const val DPAD_DOWN_RIGHT: Byte = 0x03
    const val DPAD_DOWN: Byte = 0x04
    const val DPAD_DOWN_LEFT: Byte = 0x05
    const val DPAD_LEFT: Byte = 0x06
    const val DPAD_UP_LEFT: Byte = 0x07

    // Byte 5: face buttons (bits 0-3)
    const val BTN_A: Int = 0x01
    const val BTN_B: Int = 0x02
    const val BTN_X: Int = 0x04
    const val BTN_Y: Int = 0x08

    // Byte 6: shoulder + system buttons (bits 0-7)
    const val BTN_L1: Int = 0x01
    const val BTN_R1: Int = 0x02
    const val BTN_L2: Int = 0x04
    const val BTN_R2: Int = 0x08
    const val BTN_START: Int = 0x10
    const val BTN_SELECT: Int = 0x20
    const val BTN_L3: Int = 0x40
    const val BTN_R3: Int = 0x80

    const val AXIS_CENTER: Byte = 128.toByte()
}
