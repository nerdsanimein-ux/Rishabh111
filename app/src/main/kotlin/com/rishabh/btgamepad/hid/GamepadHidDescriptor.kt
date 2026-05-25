package com.rishabh.btgamepad.hid

import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings

object GamepadHidDescriptor {

    /**
     * HID Report Descriptor — standard Windows-compatible gamepad.
     *
     * Uses Report ID 1 and UNSIGNED axes (0–255, center 128) which is what
     * Windows' built-in HID gamepad driver expects. Signed axes (-127–127)
     * cause Windows to disconnect after descriptor exchange on many systems.
     *
     * Report layout (7 bytes, Report ID handled separately by sendReport):
     *   Bytes 0-1 : 10 buttons + 6-bit padding
     *   Bytes 2-5 : 4 unsigned axes (LX, LY, RX, RY)  0=min, 128=center, 255=max
     *   Byte  6   : hat switch low nibble (0-7, 0xF=null) + 4-bit padding
     */
    val DESCRIPTOR: ByteArray = listOf(
        0x05, 0x01,        // Usage Page (Generic Desktop)
        0x09, 0x05,        // Usage (Game Pad)
        0xa1, 0x01,        // Collection (Application)
        0x85, 0x01,        //   Report ID (1)

        // Buttons 1-10
        0x05, 0x09,        //   Usage Page (Button)
        0x19, 0x01,        //   Usage Minimum (1)
        0x29, 0x0a,        //   Usage Maximum (10)
        0x15, 0x00,        //   Logical Minimum (0)
        0x25, 0x01,        //   Logical Maximum (1)
        0x75, 0x01,        //   Report Size (1)
        0x95, 0x0a,        //   Report Count (10)
        0x81, 0x02,        //   Input (Data, Variable, Absolute)

        // 6-bit padding to complete the 2-byte button field
        0x95, 0x01,        //   Report Count (1)
        0x75, 0x06,        //   Report Size (6)
        0x81, 0x03,        //   Input (Constant)

        // 4 analog axes, UNSIGNED 0-255 (center=128)
        0x05, 0x01,        //   Usage Page (Generic Desktop)
        0x09, 0x30,        //   Usage (X)   ← Left Stick X
        0x09, 0x31,        //   Usage (Y)   ← Left Stick Y
        0x09, 0x33,        //   Usage (Rx)  ← Right Stick X
        0x09, 0x34,        //   Usage (Ry)  ← Right Stick Y
        0x15, 0x00,        //   Logical Minimum (0)
        0x26, 0xff, 0x00,  //   Logical Maximum (255)
        0x75, 0x08,        //   Report Size (8)
        0x95, 0x04,        //   Report Count (4)
        0x81, 0x02,        //   Input (Data, Variable, Absolute)

        // Hat switch — 4-bit, 0-7 valid, 0xF = null/centered
        0x09, 0x39,        //   Usage (Hat Switch)
        0x15, 0x00,        //   Logical Minimum (0)
        0x25, 0x07,        //   Logical Maximum (7)
        0x35, 0x00,        //   Physical Minimum (0)
        0x46, 0x3b, 0x01,  //   Physical Maximum (315)
        0x65, 0x14,        //   Unit (Eng Rotation)
        0x75, 0x04,        //   Report Size (4)
        0x95, 0x01,        //   Report Count (1)
        0x81, 0x42,        //   Input (Data, Variable, Absolute, Null State)

        // 4-bit padding to complete hat byte
        0x75, 0x04,        //   Report Size (4)
        0x95, 0x01,        //   Report Count (1)
        0x81, 0x03,        //   Input (Constant)

        0xc0               // End Collection
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
