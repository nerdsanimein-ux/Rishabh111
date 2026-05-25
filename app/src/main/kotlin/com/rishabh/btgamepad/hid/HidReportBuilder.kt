package com.rishabh.btgamepad.hid

class HidReportBuilder {

    // Unsigned axes: center = 128, range 0..255
    var leftX:  Byte = HidConstants.AXIS_CENTER
    var leftY:  Byte = HidConstants.AXIS_CENTER
    var rightX: Byte = HidConstants.AXIS_CENTER
    var rightY: Byte = HidConstants.AXIS_CENTER

    // Hat switch: 0-7 for directions, 0x0F = null/centered (4-bit field)
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
        rightX,
        rightY,
        (dpad.toInt() and 0x0F).toByte()      // Byte 6: hat nibble (low 4 bits) + 0 padding
    )

    fun reset() {
        leftX  = HidConstants.AXIS_CENTER
        leftY  = HidConstants.AXIS_CENTER
        rightX = HidConstants.AXIS_CENTER
        rightY = HidConstants.AXIS_CENTER
        dpad = HidConstants.DPAD_CENTERED
        buttons = 0
    }
}
