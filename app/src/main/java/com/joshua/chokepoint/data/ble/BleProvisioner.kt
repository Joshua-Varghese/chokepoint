package com.joshua.chokepoint.data.ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

@SuppressLint("MissingPermission") // Checked in UI
class BleProvisioner(private val context: Context) {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val adapter = bluetoothManager.adapter
    private val scanner = adapter.bluetoothLeScanner

    // UUIDs matching Firmware
    private val SERVICE_UUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")
    private val CHAR_SSID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8")
    private val CHAR_PASS = UUID.fromString("828917c1-002f-4388-9923-a5c8874668b5")
    private val CHAR_STATUS = UUID.fromString("e3223119-9250-4086-9085-3c54678f137d")

    private val _status = MutableStateFlow("Idle")
    val status: StateFlow<String> = _status

    private var gatt: BluetoothGatt? = null
    private var ssidToWrite: String = ""
    private var passToWrite: String = ""
    
    // Callback for Scan
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.device?.let { device ->
                // Debug: Log every device found
                // Log.d("BLE", "Scanned: ${device.name} - ${device.address}")
                
                if (device.name != null && device.name.contains("Chokepoint")) {
                    Log.d("BLE", "Found Target: ${device.name} (${device.address})")
                    scanner.stopScan(this)
                    _status.value = "Found ${device.name}. Connecting..."
                    connectToDevice(device)
                }
            }
        }
        
        override fun onScanFailed(errorCode: Int) {
             _status.value = "Scan Failed: $errorCode"
        }
    }

    fun startProvisioning(ssid: String, pass: String) {
        ssidToWrite = ssid
        passToWrite = pass
        _status.value = "Scanning for Device..."
        scanner.startScan(scanCallback)
        
        // Timeout
        Handler(Looper.getMainLooper()).postDelayed({
            if (_status.value == "Scanning for Device...") {
                scanner.stopScan(scanCallback)
                _status.value = "Device Not Found. Is it in setup mode?"
            }
        }, 10000)
    }

    private fun connectToDevice(device: BluetoothDevice) {
        gatt = device.connectGatt(context, false, gattCallback)
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                _status.value = "Connected. Discovering Services..."
                gatt?.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                _status.value = "Disconnected"
                gatt?.close()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt?.getService(SERVICE_UUID)
                if (service != null) {
                    _status.value = "Service Found. Writing Credentials..."
                    writeCredential(gatt, service, CHAR_SSID, ssidToWrite)
                    // We engage a chain: Write SSID -> Success -> Write Pass -> Enable Notify
                } else {
                    _status.value = "Invalid Device (Service Missing)"
                }
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (characteristic?.uuid == CHAR_SSID) {
                    val service = gatt?.getService(SERVICE_UUID)
                    writeCredential(gatt!!, service!!, CHAR_PASS, passToWrite)
                } else if (characteristic?.uuid == CHAR_PASS) {
                    _status.value = "Credentials Sent. Waiting for Confirmation..."
                    enableNotifications(gatt!!)
                }
            } else {
                _status.value = "Write Failed: $status"
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
             // We use changed (Notify) to get status updates from ESP32
             // Or we could have used Read. BleManager.py uses Notify.
             // Wait, BleManager.py calls gatts_notify. 
             // Note: onCharacteristicChanged signature differs in recent Android versions, check imports.
             // Assuming older callback for compat or matching signature manually.
             // Actually, usually it's (gatt, char).
             
             val value = characteristic.getStringValue(0)
             Log.d("BLE", "Notification: $value")
             
             if (value.startsWith("SUCCESS:")) {
                 val deviceId = value.substringAfter("SUCCESS:")
                 _status.value = "SUCCESS:$deviceId"
                 // Clean up happens in UI
             } else {
                 _status.value = "Status: $value"
             }
        }
    }

    private fun writeCredential(gatt: BluetoothGatt, service: BluetoothGattService, uuid: UUID, value: String) {
        val char = service.getCharacteristic(uuid)
        char.setValue(value)
        char.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        gatt.writeCharacteristic(char)
    }
    
    private fun enableNotifications(gatt: BluetoothGatt) {
        val service = gatt.getService(SERVICE_UUID)
        val char = service.getCharacteristic(CHAR_STATUS)
        
        gatt.setCharacteristicNotification(char, true)
        
        // Don't forget the descriptor!
        val CLIENT_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        val desc = char.getDescriptor(CLIENT_CONFIG_UUID)
        if (desc != null) {
            desc.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gatt.writeDescriptor(desc)
        }
    }
    
    fun close() {
        gatt?.disconnect()
        gatt?.close()
    }
}
