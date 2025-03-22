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

class MyVpnService : VpnService() {
    private var running = false
    private val blockedDomains = listOf(
        "youtube.com",
        "facebook.com",
        "test.com"
    ) // Add your domains to block here

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        running = true
        Thread { startVpn() }.start()
        return START_STICKY
    }

    private fun startVpn() {
        val builder = Builder()
        builder.setSession("VPNBlocker")
            .addAddress("10.0.0.2", 32)
            .addRoute("0.0.0.0", 0)
            .setMtu(1500)

        blockedDomains.forEach { domain ->
            try {
                val addresses = InetAddress.getAllByName(domain)
                for (address in addresses) {
                    val ip = address.hostAddress ?: continue
                    builder.addDisallowedApplication(ip)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val vpnInterface = builder.establish() ?: return
        protectVpn(vpnInterface)
    }

    private fun protectVpn(vpnInterface: ParcelFileDescriptor) {
        val input = FileInputStream(vpnInterface.fileDescriptor)
        val output = FileOutputStream(vpnInterface.fileDescriptor)
        val socket = DatagramSocket()

        try {
            protect(socket)
            while (running) {
                // Basic packet forwarding implementation
                val buffer = ByteArray(32767)
                val packet = DatagramPacket(buffer, buffer.size)
                socket.receive(packet)
                output.write(packet.data, 0, packet.length)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            input.close()
            output.close()
            socket.close()
        }
    }

    override fun onDestroy() {
        running = false
        super.onDestroy()
    }
}
