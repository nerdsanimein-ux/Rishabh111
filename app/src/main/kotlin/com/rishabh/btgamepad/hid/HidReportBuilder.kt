package com.rishabh.btgamepad.hid

class HidReportBuilder {

    // Signed axes: center = 0, range -127..127
    var leftX:  Byte = 0
    var leftY:  Byte = 0
    var rightX: Byte = 0
    var rightY: Byte = 0

    // Hat switch: 0-7 for directions, 0xFF.toByte() = null/centered
    var dpad: Byte = HidConstants.DPAD_CENTERED

    // 10 buttons packed into a single Int (bits 0-9 used, 10-15 padding)
    private var buttons: Int = 0

    fun setButton(mask: Int, pressed: Boolean) {
        buttons = if (pressed) buttons or mask else buttons and mask.inv()
    }

    fun toReport(): ByteArray = byteArrayOf(
        (buttons and 0xFF).toByte(),          // Byte 0: buttons 0-7
        ((buttons shr 8) and 0xFF).toByte(),  // Byte 1: buttons 8-9 + 6-bit pad
        leftX,
        leftY,
        0,      // Z axis (spare / triggers combined — always 0 for now)
        rightX,
        rightY,
        dpad
    )

    fun reset() {
        leftX = 0; leftY = 0; rightX = 0; rightY = 0
        dpad = HidConstants.DPAD_CENTERED
        buttons = 0
    }
}
