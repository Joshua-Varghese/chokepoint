package com.joshua.chokepoint.data.discovery

import android.content.Context
import android.net.wifi.WifiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException

class DiscoveryRepository(private val context: Context) {

    fun verifyDevice(targetDeviceId: String, timeoutMs: Int = 3000): Flow<Boolean> = flow {
        var socket: DatagramSocket? = null
        var isFound = false

        try {
            socket = DatagramSocket()
            socket.broadcast = true
            socket.soTimeout = 1000 

            val message = "DISCOVER:$targetDeviceId".toByteArray()
            val packet = DatagramPacket(
                message, 
                message.size, 
                InetAddress.getByName("255.255.255.255"), 
                6666
            )
            
            // Send multiple bursts
            repeat(3) {
                withContext(Dispatchers.IO) {
                    try { socket?.send(packet) } catch(e: Exception) { e.printStackTrace() }
                }
                kotlinx.coroutines.delay(100)
            }

            val startTime = System.currentTimeMillis()
            val buffer = ByteArray(1024)
            
            while (System.currentTimeMillis() - startTime < timeoutMs) {
                try {
                    val receivePacket = DatagramPacket(buffer, buffer.size)
                    withContext(Dispatchers.IO) {
                         socket?.receive(receivePacket)
                    }
                    
                    val response = String(receivePacket.data, 0, receivePacket.length).trim()
                    if (response == "HERE:$targetDeviceId") {
                        isFound = true
                        emit(true)
                        return@flow
                    }
                } catch (e: SocketTimeoutException) {
                    // Continue loop
                } catch (e: Exception) {
                   e.printStackTrace()
                }
            }
            if (!isFound) emit(false)
            
        } catch (e: Exception) {
            e.printStackTrace()
            emit(false)
        } finally {
            socket?.close()
        }
    }.flowOn(Dispatchers.IO)
}
