package com.xforia.vpnx

import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.InetAddress
import java.nio.ByteBuffer

class MyVpnService : VpnService() {
    private var vpnInterface: ParcelFileDescriptor? = null

    // Blocked domains list
    private val blockedDomains = listOf("facebook.com", "youtube.com", "example.com")

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startVpn()
        return START_STICKY
    }

    private fun startVpn() {
        val builder = Builder()
        builder.addAddress("10.0.0.2", 24) // Fake VPN IP
        builder.addDnsServer("8.8.8.8")    // Use Google's DNS

        Thread {
            try {
                for (domain in blockedDomains) {
                    val addresses = InetAddress.getAllByName(domain) // Get all possible IPs
                    for (address in addresses) {
                        val ip = address.hostAddress?: continue
                        if (ip.isNotEmpty() && ip.matches(Regex("^\\d+\\.\\d+\\.\\d+\\.\\d+$"))) {
                            // Log the IP before adding it
                            android.util.Log.d("MyVpnService", "Blocking IP: $ip for domain: $domain")
                            builder.addRoute(ip, 32) // Block the specific IP
                        } else {
                            android.util.Log.w("MyVpnService", "Skipping invalid IP for $domain: $ip")
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            vpnInterface = builder
                .setSession("MyVpnService")
                .setConfigureIntent(getMainActivityIntent())
                .establish()

            monitorTraffic() // Start monitoring traffic after VPN is established
        }.start()
    }

    private fun monitorTraffic() {
        Thread {
            val inputStream = FileInputStream(vpnInterface!!.fileDescriptor)
            val outputStream = FileOutputStream(vpnInterface!!.fileDescriptor)
            val buffer = ByteBuffer.allocate(32767)

            android.util.Log.d("MyVpnService", "Started monitoring traffic...")

            try {
                while (vpnInterface != null) {
                    val length = inputStream.read(buffer.array())

                    if (length > 0) {
                        val packetData = buffer.array().copyOf(length)

                        // Log the raw packet data
                        android.util.Log.d("MyVpnService", "Received packet: ${packetData.joinToString(", ")}")

                        val blocked = isBlockedDomain(packetData)

                        if (blocked) {
                            android.util.Log.w("MyVpnService", "Blocked a packet containing a restricted domain.")
                        } else {
                            android.util.Log.d("MyVpnService", "Forwarding packet...")
                            outputStream.write(packetData) // Forward allowed packets
                        }
                    }
                    buffer.clear()
                }
            } catch (e: Exception) {
                android.util.Log.e("MyVpnService", "Error while monitoring traffic", e)
            } finally {
                try {
                    inputStream.close()
                    outputStream.close()
                    android.util.Log.d("MyVpnService", "Stopped monitoring traffic.")
                } catch (e: IOException) {
                    android.util.Log.e("MyVpnService", "Error closing streams", e)
                }
            }
        }.start()
    }


    // Function to check if packet contains a blocked domain
    private fun isBlockedDomain(packetData: ByteArray): Boolean {
        val packetStr = packetData.joinToString(" ") { it.toInt().toString() } // Convert raw bytes
        return blockedDomains.any { domain -> packetStr.contains(domain, ignoreCase = true) }
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
