/*
    Copyright (C) 2019-2020 Sweetie Piggy Apps <sweetiepiggyapps@gmail.com>

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
//            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
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
//            }
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
// $ openssl s_client -showcerts -connect svc.metrotransit.org:443
//  """-----BEGIN CERTIFICATE-----
// -MIIHPTCCBiWgAwIBAgIRAPi7AL3qcfGk6K6c1YH6gUYwDQYJKoZIhvcNAQELBQAw
// -gZUxCzAJBgNVBAYTAkdCMRswGQYDVQQIExJHcmVhdGVyIE1hbmNoZXN0ZXIxEDAO
// -BgNVBAcTB1NhbGZvcmQxGDAWBgNVBAoTD1NlY3RpZ28gTGltaXRlZDE9MDsGA1UE
// -AxM0U2VjdGlnbyBSU0EgT3JnYW5pemF0aW9uIFZhbGlkYXRpb24gU2VjdXJlIFNl
// -cnZlciBDQTAeFw0yMDAxMjEwMDAwMDBaFw0yMjAxMTkyMzU5NTlaMIGxMQswCQYD
// -VQQGEwJVUzETMBEGA1UEERMKNTUxMDEtMTgwNTESMBAGA1UECBMJTWlubmVzb3Rh
// -MREwDwYDVQQHEwhTdC4gUGF1bDEdMBsGA1UECRMUMzkwIFJvYmVydCBTdC4gTm9y
// -dGgxHTAbBgNVBAoTFE1ldHJvcG9saXRhbiBDb3VuY2lsMQswCQYDVQQLEwJSQTEb
// -MBkGA1UEAwwSKi5tZXRyb3RyYW5zaXQub3JnMIIBIjANBgkqhkiG9w0BAQEFAAOC
// -AQ8AMIIBCgKCAQEA4LLKHDC9OFfph9jsUYaG2vvZEcPeSsalx2j4Y8hcszFurUr3
// -yIMsiqNrvize2beZC/wO9buVYd7LEjkJ+5V57Id8cVLw5+6fPrJ4UuC2I6tcFJRh
// -vJ5SM/Vsa1o4qjXnQ+6hlYFPtiCS+PxLiyPACPqaTWk5KsY+cggL97pn2r4mWH9j
// -4RQ7DC3ccxIdNIVyBz8wOpSw1WsInBw4Sg6BDJoJCFgzlEwm4D/yqPA26pvbSdwS
// -mqIXyRSIX+iVL99ES8X326U5M80wzbIYZtTKkncEn5qpxlKNQNIPR2AgZMrC9R0B
// -v1U+GrN238faZpL3W+SSl6CeqDh8bndzAQbXYQIDAQABo4IDaDCCA2QwHwYDVR0j
// -BBgwFoAUF9nWJSdn+THCSUPZMDZEjGypT+swHQYDVR0OBBYEFD8/SueANq4KIYiL
// -7PsiYb+XBi5UMA4GA1UdDwEB/wQEAwIFoDAMBgNVHRMBAf8EAjAAMB0GA1UdJQQW
// -MBQGCCsGAQUFBwMBBggrBgEFBQcDAjBKBgNVHSAEQzBBMDUGDCsGAQQBsjEBAgED
// -BDAlMCMGCCsGAQUFBwIBFhdodHRwczovL3NlY3RpZ28uY29tL0NQUzAIBgZngQwB
// -AgIwWgYDVR0fBFMwUTBPoE2gS4ZJaHR0cDovL2NybC5zZWN0aWdvLmNvbS9TZWN0
// -aWdvUlNBT3JnYW5pemF0aW9uVmFsaWRhdGlvblNlY3VyZVNlcnZlckNBLmNybDCB
// -igYIKwYBBQUHAQEEfjB8MFUGCCsGAQUFBzAChklodHRwOi8vY3J0LnNlY3RpZ28u
// -Y29tL1NlY3RpZ29SU0FPcmdhbml6YXRpb25WYWxpZGF0aW9uU2VjdXJlU2VydmVy
// -Q0EuY3J0MCMGCCsGAQUFBzABhhdodHRwOi8vb2NzcC5zZWN0aWdvLmNvbTAvBgNV
// -HREEKDAmghIqLm1ldHJvdHJhbnNpdC5vcmeCEG1ldHJvdHJhbnNpdC5vcmcwggF9
// -BgorBgEEAdZ5AgQCBIIBbQSCAWkBZwB3AEalVet1+pEgMLWiiWn0830RLEF0vv1J
// -uIWr8vxw/m1HAAABb8mmDcYAAAQDAEgwRgIhALzkKPunk/NZ8alC1lRQJjLEC7O4
// -AyA74lyb6DNWM4mMAiEA9zN33cnIpFPNq1nfrJm/tHco5Bub4wC3BqoxBsmmBEgA
// -dQBvU3asMfAxGdiZAKRRFf93FRwR2QLBACkGjbIImjfZEwAAAW/Jpg2uAAAEAwBG
// -MEQCID7zyQjmVhuUULj97eFWcmZHAKninJ4mKCyJpGL1fDoJAiB4hussN0wuLoS0
// -9kPLgezcQiFsRi4w2+LFjTeBbWEpYgB1ACJFRQdZVSRWlj+hL/H3bYbgIyZjrcBL
// -f13Gg1xu4g8CAAABb8mmDbIAAAQDAEYwRAIgSAVTKS3XAJkvsoM6wPvBqeF6A7Fj
// -PqYsgyqIGtbSSIECIAshf6g9EEfPJGhTfh6sMcfJtCwpAR+LiC69iB+7FDtjMA0G
// -CSqGSIb3DQEBCwUAA4IBAQA9uotfVgdleebb94V3IXSbqLKfJs4te8r5bOkcB4n2
// -qvjwkcW1UwXZ07QMdT2wOeRoizSUzWYJFEV/pDKLH9j1y2X1D08KUcdSkB5GdfXw
// -zHSt326bRcfkHza3oo6wSBK0+hffVmutq2yxeOFo+ZdDT1Pxz9AEfOeIRE0lcx7/
// -F5eMyd6VONZ+2pptVYbiCUGpohx6YeBSxAfSPTAuVKGajsvlrP0xCQbm5jQyMzF7
// -DjK+hVEqjV7h/yXyqy2Vl82NvCYOuWHtzMLqdrV2LqH5USO9iVNmuAURx09TDMh8
// -KvpFLNDTcYg9IpFY8h8pMRJfYiEPfbC/75X2Y94DUQXy
// -----END CERTIFICATE-----"""

// this is Sectigo's certificate, obtained within Firefox
"""-----BEGIN CERTIFICATE-----
MIIGGTCCBAGgAwIBAgIQE31TnKp8MamkM3AZaIR6jTANBgkqhkiG9w0BAQwFADCB
iDELMAkGA1UEBhMCVVMxEzARBgNVBAgTCk5ldyBKZXJzZXkxFDASBgNVBAcTC0pl
cnNleSBDaXR5MR4wHAYDVQQKExVUaGUgVVNFUlRSVVNUIE5ldHdvcmsxLjAsBgNV
BAMTJVVTRVJUcnVzdCBSU0EgQ2VydGlmaWNhdGlvbiBBdXRob3JpdHkwHhcNMTgx
MTAyMDAwMDAwWhcNMzAxMjMxMjM1OTU5WjCBlTELMAkGA1UEBhMCR0IxGzAZBgNV
BAgTEkdyZWF0ZXIgTWFuY2hlc3RlcjEQMA4GA1UEBxMHU2FsZm9yZDEYMBYGA1UE
ChMPU2VjdGlnbyBMaW1pdGVkMT0wOwYDVQQDEzRTZWN0aWdvIFJTQSBPcmdhbml6
YXRpb24gVmFsaWRhdGlvbiBTZWN1cmUgU2VydmVyIENBMIIBIjANBgkqhkiG9w0B
AQEFAAOCAQ8AMIIBCgKCAQEAnJMCRkVKUkiS/FeN+S3qU76zLNXYqKXsW2kDwB0Q
9lkz3v4HSKjojHpnSvH1jcM3ZtAykffEnQRgxLVK4oOLp64m1F06XvjRFnG7ir1x
on3IzqJgJLBSoDpFUd54k2xiYPHkVpy3O/c8Vdjf1XoxfDV/ElFw4Sy+BKzL+k/h
fGVqwECn2XylY4QZ4ffK76q06Fha2ZnjJt+OErK43DOyNtoUHZZYQkBuCyKFHFEi
rsTIBkVtkuZntxkj5Ng2a4XQf8dS48+wdQHgibSov4o2TqPgbOuEQc6lL0giE5dQ
YkUeCaXMn2xXcEAG2yDoG9bzk4unMp63RBUJ16/9fAEc2wIDAQABo4IBbjCCAWow
HwYDVR0jBBgwFoAUU3m/WqorSs9UgOHYm8Cd8rIDZsswHQYDVR0OBBYEFBfZ1iUn
Z/kxwklD2TA2RIxsqU/rMA4GA1UdDwEB/wQEAwIBhjASBgNVHRMBAf8ECDAGAQH/
AgEAMB0GA1UdJQQWMBQGCCsGAQUFBwMBBggrBgEFBQcDAjAbBgNVHSAEFDASMAYG
BFUdIAAwCAYGZ4EMAQICMFAGA1UdHwRJMEcwRaBDoEGGP2h0dHA6Ly9jcmwudXNl
cnRydXN0LmNvbS9VU0VSVHJ1c3RSU0FDZXJ0aWZpY2F0aW9uQXV0aG9yaXR5LmNy
bDB2BggrBgEFBQcBAQRqMGgwPwYIKwYBBQUHMAKGM2h0dHA6Ly9jcnQudXNlcnRy
dXN0LmNvbS9VU0VSVHJ1c3RSU0FBZGRUcnVzdENBLmNydDAlBggrBgEFBQcwAYYZ
aHR0cDovL29jc3AudXNlcnRydXN0LmNvbTANBgkqhkiG9w0BAQwFAAOCAgEAThNA
lsnD5m5bwOO69Bfhrgkfyb/LDCUW8nNTs3Yat6tIBtbNAHwgRUNFbBZaGxNh10m6
pAKkrOjOzi3JKnSj3N6uq9BoNviRrzwB93fVC8+Xq+uH5xWo+jBaYXEgscBDxLmP
bYox6xU2JPti1Qucj+lmveZhUZeTth2HvbC1bP6mESkGYTQxMD0gJ3NR0N6Fg9N3
OSBGltqnxloWJ4Wyz04PToxcvr44APhL+XJ71PJ616IphdAEutNCLFGIUi7RPSRn
R+xVzBv0yjTqJsHe3cQhifa6ezIejpZehEU4z4CqN2mLYBd0FUiRnG3wTqN3yhsc
SPr5z0noX0+FCuKPkBurcEya67emP7SsXaRfz+bYipaQ908mgWB2XQ8kd5GzKjGf
FlqyXYwcKapInI5v03hAcNt37N3j0VcFcC3mSZiIBYRiBXBWdoY5TtMibx3+bfEO
s2LEPMvAhblhHrrhFYBZlAyuBbuMf1a+HNJav5fyakywxnB2sJCNwQs2uRHY1ihc
6k/+JLcYCpsM0MF8XPtpvcyiTcaQvKZN8rG61ppnW5YCUtCC+cQKXA0o4D/I+pWV
idWkvklsQLI+qGu41SWyxP7x09fn1txDAXYw+zuLXfdKiXyaNb78yvBXAfCNP6CH
MntHWpdLgtJmwsQt6j8k9Kf5qLnjatkYYaA7jBU=
-----END CERTIFICATE-----"""
    }
}

