package com.rishabh.btgamepad.hid

import com.rishabh.btgamepad.hid.HidConstants.AXIS_CENTER
import com.rishabh.btgamepad.hid.HidConstants.DPAD_CENTERED

/**
 * Holds the current gamepad state and serializes it to an 8-byte HID report.
 * All mutations must happen on a single thread (UI thread).
 */
class HidReportBuilder {

    var leftX: Byte = AXIS_CENTER
    var leftY: Byte = AXIS_CENTER
    var rightX: Byte = AXIS_CENTER
    var rightY: Byte = AXIS_CENTER

    /** Hat value: 0x00=Up … 0x07=UpLeft, 0x0F=Center */
    var dpad: Byte = DPAD_CENTERED

    /** Bits 0-3: A, B, X, Y */
    var buttons1: Byte = 0

    /** Bits 0-7: L1, R1, L2, R2, Start, Select, L3, R3 */
    var buttons2: Byte = 0

    fun toReport(): ByteArray = byteArrayOf(
        leftX,
        leftY,
        rightX,
        rightY,
        (dpad.toInt() and 0x0F).toByte(),
        buttons1,
        buttons2,
        0x00.toByte()
    )

    /**
     * @param byteGroup 1 = face buttons byte, 2 = shoulder/system byte
     */
    fun setButton(mask: Int, byteGroup: Int, pressed: Boolean) {
        when (byteGroup) {
            1 -> buttons1 = if (pressed) (buttons1.toInt() or mask).toByte()
                            else (buttons1.toInt() and mask.inv()).toByte()
            2 -> buttons2 = if (pressed) (buttons2.toInt() or mask).toByte()
                            else (buttons2.toInt() and mask.inv()).toByte()
        }
    }

    fun reset() {
        leftX = AXIS_CENTER; leftY = AXIS_CENTER
        rightX = AXIS_CENTER; rightY = AXIS_CENTER
        dpad = DPAD_CENTERED
        buttons1 = 0; buttons2 = 0
    }
}
