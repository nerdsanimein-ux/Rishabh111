package com.rishabh.btgamepad.hid

import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings

object GamepadHidDescriptor {

    /**
     * HID Report Descriptor — standard gamepad, NO Report ID.
     *
     * Matches the proven-compatible "Direct" format:
     *   Bytes 0-1 : 10 buttons (A,B,X,Y,L1,R1,L2,R2,Start,Select) + 6-bit padding
     *   Bytes 2-6 : 5 signed axes  −127…127  (LX, LY, Z=spare, RX, RY)
     *   Byte  7   : Hat switch 0-7 / 0xFF=null (8-bit with null state)
     *
     * No Report ID → sendReport uses id=0.
     */
    val DESCRIPTOR: ByteArray = listOf(
        0x05, 0x01,   // Usage Page (Generic Desktop)
        0x09, 0x05,   // Usage (Game Pad)
        0xa1, 0x01,   // Collection (Application)
        0xa1, 0x00,   //   Collection (Physical)

        // Buttons 1-10
        0x05, 0x09,   //   Usage Page (Button)
        0x19, 0x01,   //   Usage Minimum (Button 1)
        0x29, 0x0a,   //   Usage Maximum (Button 10)
        0x15, 0x00,   //   Logical Minimum (0)
        0x25, 0x01,   //   Logical Maximum (1)
        0x95, 0x0a,   //   Report Count (10)
        0x75, 0x01,   //   Report Size (1)
        0x81, 0x02,   //   Input (Data, Var, Abs)

        // 6-bit padding (completes the 2-byte button field)
        0x95, 0x01,   //   Report Count (1)
        0x75, 0x06,   //   Report Size (6)
        0x81, 0x03,   //   Input (Const, Var, Abs)

        // 5 analog axes, signed -127 to 127
        0x05, 0x01,   //   Usage Page (Generic Desktop)
        0x09, 0x30,   //   Usage (X)  ← Left Stick X
        0x09, 0x31,   //   Usage (Y)  ← Left Stick Y
        0x09, 0x32,   //   Usage (Z)  ← spare / combined triggers
        0x09, 0x33,   //   Usage (Rx) ← Right Stick X
        0x09, 0x34,   //   Usage (Ry) ← Right Stick Y
        0x15, 0x81,   //   Logical Minimum (-127)
        0x25, 0x7f,   //   Logical Maximum (127)
        0x75, 0x08,   //   Report Size (8)
        0x95, 0x05,   //   Report Count (5)
        0x81, 0x02,   //   Input (Data, Var, Abs)

        // Hat switch — 8-bit, null state outside 0-7
        0x05, 0x01,   //   Usage Page (Generic Desktop)
        0x09, 0x39,   //   Usage (Hat Switch)
        0x15, 0x00,   //   Logical Minimum (0)
        0x25, 0x07,   //   Logical Maximum (7)
        0x75, 0x08,   //   Report Size (8)
        0x95, 0x01,   //   Report Count (1)
        0x81, 0x42,   //   Input (Data, Var, Abs, Null State)

        0xc0,         //   End Collection
        0xc0          // End Collection
    ).map { it.toByte() }.toByteArray()

    fun buildSdpSettings(name: String = "Android Controller"): BluetoothHidDeviceAppSdpSettings =
        BluetoothHidDeviceAppSdpSettings(
            name,
            "Bluetooth HID Gamepad",
            "Android",
            BluetoothHidDevice.SUBCLASS2_GAMEPAD,
            DESCRIPTOR
        )
}
