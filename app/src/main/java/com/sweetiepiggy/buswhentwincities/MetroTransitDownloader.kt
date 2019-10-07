/*
    Copyright (C) 2019 Sweetie Piggy Apps <sweetiepiggyapps@gmail.com>

    This file is part of Bus When? (Twin Cities).

    Bus When? (Twin Cities) is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 3 of the License, or
    (at your option) any later version.

    Bus When? (Twin Cities) is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Bus When? (Twin Cities); if not, see <http://www.gnu.org/licenses/>.
*/

package com.sweetiepiggy.buswhentwincities

import android.os.Build
import android.util.JsonReader
import java.io.*
import java.net.MalformedURLException
import java.net.SocketException
import java.net.URL
import java.net.UnknownHostException
import java.security.KeyStore
import java.security.cert.CertificateFactory
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory

class MetroTransitDownloader() {

    @Throws(MalformedURLException::class, UnsupportedEncodingException::class, IOException::class,
			IllegalStateException::class)
    fun openJsonReader(operation: NexTripOperation): JsonReader {
        val urlConnection = (getMetroTransitUrl(operation).openConnection() as HttpsURLConnection).apply {
            // trust svc.metrotransit.org server certificate on Android 4.1 - 4.4
            // https://developer.android.com/training/articles/security-ssl
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
                val cf: CertificateFactory = CertificateFactory.getInstance("X.509")
                val caInput: InputStream = ByteArrayInputStream(SERVER_CERTIFICATE.toByteArray())
                val ca = caInput.use {
                    cf.generateCertificate(it)
                }
                val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
                    load(null, null)
                    setCertificateEntry("ca", ca)
                }
                val tmf =
                	TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply {
                        init(keyStore)
                    }
                val context = SSLContext.getInstance("TLS").apply {
                    init(null, tmf.trustManagers, null)
                }
                sslSocketFactory = context.socketFactory
            }
        }

        return JsonReader(InputStreamReader(urlConnection.inputStream, "utf-8"))
    }

    sealed class DownloadError {
        object UnknownHost: DownloadError()
        data class FileNotFound(val message: String?): DownloadError()
        data class TimedOut(val message: String?): DownloadError()
        data class OtherDownloadError(val message: String?): DownloadError()
    }

    sealed class NexTripOperation {
        object GetProviders: NexTripOperation()
        object GetRoutes: NexTripOperation()
        data class GetDirections(val routeId: String): NexTripOperation()
        data class GetStops(val routeId: String, val direction: NexTrip.Direction): NexTripOperation()
        data class GetDepartures(val stopId: Int): NexTripOperation()
        data class GetTimepointDepartures(val routeId: String, val direction: NexTrip.Direction,
        		val timestopId: String): NexTripOperation()
        data class GetVehicleLocations(val route: Int): NexTripOperation()
    }

    private fun getMetroTransitUrl(operation: NexTripOperation): URL =
	    URL(NEXTRIP_URL +
        	when(operation) {
                is NexTripOperation.GetProviders -> "Providers"
                is NexTripOperation.GetRoutes -> "Routes"
                is NexTripOperation.GetDirections -> "Directions/${operation.routeId}"
                is NexTripOperation.GetStops -> "Stops/${operation.routeId}/${NexTrip.getDirectionId(operation.direction)}"
                is NexTripOperation.GetDepartures -> "${operation.stopId}"
                is NexTripOperation.GetTimepointDepartures -> "${operation.routeId}/${NexTrip.getDirectionId(operation.direction)}/${operation.timestopId}"
                is NexTripOperation.GetVehicleLocations -> "VehicleLocations/${operation.route}"
            } + "?format=json"
		)

    companion object {
        private val NEXTRIP_URL = "https://svc.metrotransit.org/NexTrip/"
        private val SERVER_CERTIFICATE =
"""-----BEGIN CERTIFICATE-----
MIIFvDCCBKSgAwIBAgIQEHEGWQaQjqn4GvYBlPTznTANBgkqhkiG9w0BAQsFADCB
ljELMAkGA1UEBhMCR0IxGzAZBgNVBAgTEkdyZWF0ZXIgTWFuY2hlc3RlcjEQMA4G
A1UEBxMHU2FsZm9yZDEaMBgGA1UEChMRQ09NT0RPIENBIExpbWl0ZWQxPDA6BgNV
BAMTM0NPTU9ETyBSU0EgT3JnYW5pemF0aW9uIFZhbGlkYXRpb24gU2VjdXJlIFNl
cnZlciBDQTAeFw0xODAzMDgwMDAwMDBaFw0yMDAyMTkyMzU5NTlaMIGqMQswCQYD
VQQGEwJVUzETMBEGA1UEERMKNTUxMDEtMTgwNTELMAkGA1UECBMCTU4xETAPBgNV
BAcTCFN0LiBQYXVsMR0wGwYDVQQJExQzOTAgUm9iZXJ0IFN0LiBOb3J0aDEdMBsG
A1UEChMUTWV0cm9wb2xpdGFuIENvdW5jaWwxCzAJBgNVBAsTAlJBMRswGQYDVQQD
DBIqLm1ldHJvdHJhbnNpdC5vcmcwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEK
AoIBAQDD3olzlnHMedflum3NgbrYyhpOau04tTPzwKg14cphWGpfnkdLa2vJXte+
1dZ4VN8kievHqTGcfjDKYEoXEqrTx16G6+5f7WvFq3RH+auTTZ5KHluWGA/wVCjq
byS5uy1Xg7GE3AF8OF8FpilET1C3OGCBndhgiRs/d5fCJE9a9WXIfqNEXDW4f/2m
+GHgbJjKCAkm08F7CTPLNQEYdN4K3ECg+a3/NjWraQUCThDE/B2HfVZdDF6ZNSLH
PxSpjbB+l15MLKd57sV6fhAdt3WqKksYtEwgy0qrHLX5EaqQsp+eimKvOGju8BZ2
u32gdNQepIvl52GYPt94uPpmv87VAgMBAAGjggHuMIIB6jAfBgNVHSMEGDAWgBSa
8yvaz61Pti+7KkhIKhK3G0LBJDAdBgNVHQ4EFgQU6P0mHQenUc2NC1aZ1/8aro2y
e3gwDgYDVR0PAQH/BAQDAgWgMAwGA1UdEwEB/wQCMAAwHQYDVR0lBBYwFAYIKwYB
BQUHAwEGCCsGAQUFBwMCMFAGA1UdIARJMEcwOwYMKwYBBAGyMQECAQMEMCswKQYI
KwYBBQUHAgEWHWh0dHBzOi8vc2VjdXJlLmNvbW9kby5jb20vQ1BTMAgGBmeBDAEC
AjBaBgNVHR8EUzBRME+gTaBLhklodHRwOi8vY3JsLmNvbW9kb2NhLmNvbS9DT01P
RE9SU0FPcmdhbml6YXRpb25WYWxpZGF0aW9uU2VjdXJlU2VydmVyQ0EuY3JsMIGL
BggrBgEFBQcBAQR/MH0wVQYIKwYBBQUHMAKGSWh0dHA6Ly9jcnQuY29tb2RvY2Eu
Y29tL0NPTU9ET1JTQU9yZ2FuaXphdGlvblZhbGlkYXRpb25TZWN1cmVTZXJ2ZXJD
QS5jcnQwJAYIKwYBBQUHMAGGGGh0dHA6Ly9vY3NwLmNvbW9kb2NhLmNvbTAvBgNV
HREEKDAmghIqLm1ldHJvdHJhbnNpdC5vcmeCEG1ldHJvdHJhbnNpdC5vcmcwDQYJ
KoZIhvcNAQELBQADggEBABCGppVgD6PHNAzdoy1gHtvoJEwYmoTclPZTqFb80sX9
aKiwogXGZhuBprfv6IevW897lKd0cm7/Vze4O8raU0eVA/S6t6a+KLTQ8x8oi4yr
uqeaphBKwBsiM3k6QxPNsEcPfH3DMAGXnOvYtoPP1bW6GUG4MGbhJJq8/4ZQft20
RA19x6ALEbk3NZf359c3MUcm6hM0mL3rhimo3bnlI7aoWbmnweLqGbXaxkmB1+0w
Re5CC3678BMa+CX45pXNxAuL2UbJzjEyS3KQVtaqUTSbDCVLsNcZfEsdiwN5JL4o
AbmzNlB7SmQ4YwT66SxKHunjBEI/ZK8lCJtjGOPYsrs=
-----END CERTIFICATE-----"""
    }
}

