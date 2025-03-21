package com.xforia.vpnx;

import android.app.Service;

import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer

class MyVpnService : VpnService() {
    private var vpnInterface: ParcelFileDescriptor? = null
    // List of blocked domains
    private val blockedDomains = listOf("facebook.com", "youtube.com", "example.com")

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startVpn()
        return START_STICKY
    }

    private fun startVpn() {
        val builder = Builder()

        // Configure VPN settings
        builder.addAddress("10.0.0.2", 24) // Fake VPN IP
        builder.addDnsServer("10.0.0.2")  // Use Google's DNS

        // Allow all traffic EXCEPT blocked domains
        Thread {
            try {
                val blockedIPs = blockedDomains.mapNotNull { domain ->
                    try {
                        InetAddress.getByName(domain).hostAddress
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null // Skip failed lookups
                    }
                }

                for (ip in blockedIPs) {
                    builder.addRoute(ip, 32) // Block resolved IPs
                }

                // Establish VPN connection after resolving IPs
                vpnInterface = builder
                    .setSession("MyVpnService")
                    .setConfigureIntent(getMainActivityIntent())
                    .establish()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()


        // Start the VPN interface
        vpnInterface = builder
            .setSession("MyVpnService")
            .setConfigureIntent(getMainActivityIntent())
            .establish()

        monitorTraffic()
    }


    private fun monitorTraffic() {
        Thread {
            var inputStream: FileInputStream? = null
            var outputStream: FileOutputStream? = null

            try {
                inputStream = FileInputStream(vpnInterface!!.fileDescriptor)
                outputStream = FileOutputStream(vpnInterface!!.fileDescriptor)
                val buffer = ByteBuffer.allocate(32767)

                while (vpnInterface != null) {
                    val length = inputStream.read(buffer.array())
                    if (length > 0) {
                        val packetData = buffer.array().copyOf(length)

                        // Check if this is a DNS request
                        val domain = extractDomainFromDns(packetData)
                        if (domain != null && blockedDomains.contains(domain)) {
                            println("Blocking domain: $domain")
                            sendFakeDnsResponse(outputStream, packetData) // Return fake IP
                        } else {
                            val realResponse = forwardDnsQuery(packetData)
                            if (realResponse.isNotEmpty()) {
                                outputStream.write(realResponse)
                            }
                        }
                    }
                    buffer.clear()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                inputStream?.close()
                outputStream?.close()
            }
        }.start()
    }

    private fun forwardDnsQuery(packetData: ByteArray): ByteArray {
        return try {
            val socket = DatagramSocket()
            socket.soTimeout = 5000 // 5-second timeout

            val dnsServer = InetAddress.getByName("8.8.8.8") // Google DNS
            val requestPacket = DatagramPacket(packetData, packetData.size, dnsServer, 53)
            socket.send(requestPacket)

            val responseBuffer = ByteArray(512) // Standard DNS response size
            val responsePacket = DatagramPacket(responseBuffer, responseBuffer.size)
            socket.receive(responsePacket)

            socket.close()
            responseBuffer.copyOf(responsePacket.length) // Return actual response data
        } catch (e: Exception) {
            e.printStackTrace()
            ByteArray(0) // Return empty response on failure
        }
    }

    private fun extractDomainFromDns(packetData: ByteArray): String? {
        // Check if packet is a DNS query
        if (packetData.size < 12) return null // DNS header size

        val dnsHeaderSize = 12
        var index = dnsHeaderSize
        val domainParts = mutableListOf<String>()

        while (index < packetData.size) {
            val length = packetData[index].toInt() and 0xFF
            if (length == 0) break // End of domain

            if (index + length >= packetData.size) return null
            domainParts.add(String(packetData, index + 1, length))
            index += length + 1
        }

        return domainParts.joinToString(".")
    }

    private fun sendFakeDnsResponse(outputStream: FileOutputStream, packetData: ByteArray) {
        try {
            val fakeResponse = packetData.copyOf() // Copy original request
            fakeResponse[2] = 0x81.toByte() // Set response flag
            fakeResponse[3] = 0x80.toByte() // Set recursion available flag
            fakeResponse[7] = 0x01.toByte() // Set answer count to 1

            // Fake IP Address (127.0.0.1)
            val fakeIp = byteArrayOf(127, 0, 0, 1)

            // Append fake response
            val response = fakeResponse + byteArrayOf(
                0xC0.toByte(), 0x0C, // Pointer to query
                0x00, 0x01, // Type A
                0x00, 0x01, // Class IN
                0x00, 0x00, 0x00, 0x3C, // TTL
                0x00, 0x04 // Data length
            ) + fakeIp

            outputStream.write(response)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }





    private fun monitorTraffic2() {
        val inputStream = FileInputStream(vpnInterface!!.fileDescriptor)
        val outputStream = FileOutputStream(vpnInterface!!.fileDescriptor)
        val buffer = ByteBuffer.allocate(32767)

        while (true) {
            val length = inputStream.read(buffer.array())
            if (length > 0) {
                val packetData = buffer.array().copyOf(length)
                val blocked = blockedDomains.any { String(packetData).contains(it) }
                if (blocked) {
                    // Drop packet (do nothing)
                } else {
                    outputStream.write(packetData)
                }
            }
            buffer.clear()
        }
    }

    override fun onDestroy() {
        vpnInterface?.close()
    }

    private fun getMainActivityIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java)
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

}