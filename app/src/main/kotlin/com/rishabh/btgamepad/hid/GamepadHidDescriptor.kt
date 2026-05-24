package com.rishabh.btgamepad.hid

import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings

object GamepadHidDescriptor {

    /**
     * HID Report Descriptor — standard gamepad layout.
     *
     * Report ID 1, 8-byte payload:
     *   [0] Left Stick X    0x00–0xFF (center 0x80)
     *   [1] Left Stick Y    0x00–0xFF (center 0x80)
     *   [2] Right Stick X   0x00–0xFF (center 0x80)
     *   [3] Right Stick Y   0x00–0xFF (center 0x80)
     *   [4] D-pad hat (low 4 bits) | padding (high 4 bits)
     *         0x00=Up 0x01=UpRight 0x02=Right 0x03=DownRight
     *         0x04=Down 0x05=DownLeft 0x06=Left 0x07=UpLeft 0x0F=Center
     *   [5] A(b0) B(b1) X(b2) Y(b3) | 0000 padding
     *   [6] L1(b0) R1(b1) L2(b2) R2(b3) Start(b4) Select(b5) L3(b6) R3(b7)
     *   [7] padding
     */
    val DESCRIPTOR: ByteArray = byteArrayOf(
        0x05.toByte(), 0x01.toByte(),  // Usage Page (Generic Desktop)
        0x09.toByte(), 0x05.toByte(),  // Usage (Gamepad)
        0xA1.toByte(), 0x01.toByte(),  // Collection (Application)
        0x85.toByte(), 0x01.toByte(),  //   Report ID (1)

        // Analog axes: Left X, Left Y, Right X, Right Y
        0x05.toByte(), 0x01.toByte(),  //   Usage Page (Generic Desktop)
        0x09.toByte(), 0x30.toByte(),  //   Usage (X)
        0x09.toByte(), 0x31.toByte(),  //   Usage (Y)
        0x09.toByte(), 0x32.toByte(),  //   Usage (Z)
        0x09.toByte(), 0x35.toByte(),  //   Usage (Rz)
        0x15.toByte(), 0x00.toByte(),  //   Logical Minimum (0)
        0x26.toByte(), 0xFF.toByte(), 0x00.toByte(), // Logical Maximum (255)
        0x75.toByte(), 0x08.toByte(),  //   Report Size (8)
        0x95.toByte(), 0x04.toByte(),  //   Report Count (4)
        0x81.toByte(), 0x02.toByte(),  //   Input (Data, Var, Abs)

        // D-pad hat switch (4 bits, null state)
        0x09.toByte(), 0x39.toByte(),  //   Usage (Hat Switch)
        0x15.toByte(), 0x00.toByte(),  //   Logical Minimum (0)
        0x25.toByte(), 0x07.toByte(),  //   Logical Maximum (7)
        0x35.toByte(), 0x00.toByte(),  //   Physical Minimum (0)
        0x46.toByte(), 0x3B.toByte(), 0x01.toByte(), // Physical Maximum (315)
        0x65.toByte(), 0x14.toByte(),  //   Unit (Degrees)
        0x75.toByte(), 0x04.toByte(),  //   Report Size (4)
        0x95.toByte(), 0x01.toByte(),  //   Report Count (1)
        0x81.toByte(), 0x42.toByte(),  //   Input (Data, Var, Abs, Null State)
        0x65.toByte(), 0x00.toByte(),  //   Unit (None)

        // 4-bit padding to complete the byte
        0x75.toByte(), 0x04.toByte(),
        0x95.toByte(), 0x01.toByte(),
        0x81.toByte(), 0x03.toByte(),  //   Input (Const)

        // Face buttons: A, B, X, Y (4 bits)
        0x05.toByte(), 0x09.toByte(),  //   Usage Page (Button)
        0x19.toByte(), 0x01.toByte(),  //   Usage Minimum (1)
        0x29.toByte(), 0x04.toByte(),  //   Usage Maximum (4)
        0x15.toByte(), 0x00.toByte(),  //   Logical Minimum (0)
        0x25.toByte(), 0x01.toByte(),  //   Logical Maximum (1)
        0x75.toByte(), 0x01.toByte(),  //   Report Size (1)
        0x95.toByte(), 0x04.toByte(),  //   Report Count (4)
        0x81.toByte(), 0x02.toByte(),  //   Input (Data, Var, Abs)

        // 4-bit padding
        0x75.toByte(), 0x04.toByte(),
        0x95.toByte(), 0x01.toByte(),
        0x81.toByte(), 0x03.toByte(),  //   Input (Const)

        // Shoulder + system buttons: L1,R1,L2,R2,Start,Select,L3,R3 (8 bits)
        0x05.toByte(), 0x09.toByte(),  //   Usage Page (Button)
        0x19.toByte(), 0x05.toByte(),  //   Usage Minimum (5)
        0x29.toByte(), 0x0C.toByte(),  //   Usage Maximum (12)
        0x15.toByte(), 0x00.toByte(),  //   Logical Minimum (0)
        0x25.toByte(), 0x01.toByte(),  //   Logical Maximum (1)
        0x75.toByte(), 0x01.toByte(),  //   Report Size (1)
        0x95.toByte(), 0x08.toByte(),  //   Report Count (8)
        0x81.toByte(), 0x02.toByte(),  //   Input (Data, Var, Abs)

        // 1 padding byte
        0x75.toByte(), 0x08.toByte(),
        0x95.toByte(), 0x01.toByte(),
        0x81.toByte(), 0x03.toByte(),  //   Input (Const)

        0xC0.toByte()                  // End Collection
    )

    fun buildSdpSettings(): BluetoothHidDeviceAppSdpSettings =
        BluetoothHidDeviceAppSdpSettings(
            "Android Gamepad",
            "Virtual Gamepad via BluetoothHidDevice",
            "Rishabh",
            BluetoothHidDevice.SUBCLASS2_GAMEPAD,
            DESCRIPTOR
        )
}
