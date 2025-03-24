package com.xforia.vpnx

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
import java.util.concurrent.ConcurrentHashMap

class MyVpnService : VpnService() {
    private var running = false
    private val blockedDomains = listOf(
        "youtube.com",
        "facebook.com",
        "test.com"
    ) // Add your domains to block here
    private val blockedIps = ConcurrentHashMap.newKeySet<String>()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        running = true
        Thread { startVpn() }.start()
        return START_STICKY
    }

    private fun startVpn() {
        blockedDomains.forEach { domain ->
            try {
                InetAddress.getAllByName(domain).forEach { addr ->
                    blockedIps.add(addr.hostAddress)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        val builder = Builder()
        builder.setSession("VPNBlocker")
            .addAddress("10.0.0.2", 32) // Virtual IP for VPN
            .addDnsServer("8.8.8.8")    // Google DNS
            .addRoute("0.0.0.0", 0)     // Route all traffic
            .setMtu(1500)
            .setBlocking(true)

        val vpnInterface = builder.establish() ?: return
        handleTraffic(vpnInterface)
    }
    private fun handleTraffic(vpnInterface: ParcelFileDescriptor) {
        val input = FileInputStream(vpnInterface.fileDescriptor)
        val output = FileOutputStream(vpnInterface.fileDescriptor)

        try {
            val buffer = ByteBuffer.allocate(32767)
            while (running) {
                buffer.clear()
                val length = input.read(buffer.array())
                if (length > 0) {
                    buffer.limit(length)

                    // Parse IP packet
                    if (isBlockedPacket(buffer)) {
                        // Drop blocked packets
                        continue
                    }

                    // Forward allowed packets
                    output.write(buffer.array(), 0, length)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            input.close()
            output.close()
        }
    }

    private fun isBlockedPacket(buffer: ByteBuffer): Boolean {
        try {
            // Check IP version (first 4 bits)
            val version = (buffer.get(0).toInt() shr 4) and 0x0F
            if (version != 4) return false // Only handle IPv4 for now

            // Extract destination IP
            val destIpBytes = ByteArray(4)
            buffer.position(16) // Destination IP offset in IPv4 header
            buffer.get(destIpBytes)
            val destIp = InetAddress.getByAddress(destIpBytes).hostAddress

            return blockedIps.contains(destIp)
        } catch (e: Exception) {
            return false // Allow packet if we can't parse it
        }
    }

    override fun onDestroy() {
        running = false
        super.onDestroy()
    }
}
