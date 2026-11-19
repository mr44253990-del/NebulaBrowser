package com.nebula.browser.feature.extensions

import android.content.Context
import android.net.Uri
import com.nebula.browser.core.ExtensionReport
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.ZipInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** Inserts no extension code into WebView. It only performs static compatibility inspection. */
class ExtensionCompatibilityAnalyzer(private val context: Context) {
    suspend fun analyze(uri: Uri, displayName: String): Result<ExtensionReport> = withContext(Dispatchers.IO) {
        runCatching {
            val input = context.contentResolver.openInputStream(uri)
                ?: error("Cannot open this file")
            input.use { stream ->
                val zipStream = prepareZip(stream)
                val manifestBytes = findManifest(zipStream)
                    ?: error("manifest.json was not found in the extension package")
                val manifest = JSONObject(manifestBytes.toString(Charsets.UTF_8))
                report(displayName, manifestBytes.toString(Charsets.UTF_8), manifest)
            }
        }
    }

    private fun prepareZip(input: InputStream): ZipInputStream {
        val header = input.readExact(4)
        if (header.size >= 2 && header[0] == 'P'.code.toByte() && header[1] == 'K'.code.toByte()) {
            return ZipInputStream(PrefixInputStream(header, input))
        }
        require(header.toString(Charsets.US_ASCII) == "Cr24") { "Use a .zip, .xpi, or .crx extension package" }
        val version = input.readLittleEndianInt()
        val skipBytes = when (version) {
            2L -> input.readLittleEndianInt() + input.readLittleEndianInt()
            3L -> input.readLittleEndianInt()
            else -> error("Unsupported CRX version $version")
        }
        require(skipBytes in 0..16 * 1024 * 1024) { "Invalid CRX header" }
        input.skipFully(skipBytes.toInt())
        return ZipInputStream(input)
    }

    private fun findManifest(zip: ZipInputStream): ByteArray? {
        var entries = 0
        while (entries++ < 200) {
            val entry = zip.nextEntry ?: break
            if (!entry.isDirectory && entry.name.removePrefix("./") == "manifest.json") {
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var total = 0
                while (true) {
                    val count = zip.read(buffer)
                    if (count < 0) break
                    total += count
                    require(total <= 2 * 1024 * 1024) { "manifest.json is too large" }
                    output.write(buffer, 0, count)
                }
                return output.toByteArray()
            }
        }
        return null
    }

    private fun report(fileName: String, rawManifest: String, manifest: JSONObject): ExtensionReport {
        val permissions = manifest.optJSONArray("permissions").toStringList()
        val hosts = manifest.optJSONArray("host_permissions").toStringList()
        val manifestVersion = manifest.optInt("manifest_version", 1)
        val chromeApis = Regex("(?:chrome|browser)\\.([A-Za-z0-9_]+)").findAll(rawManifest)
            .map { it.value }.toSet().toList().sorted()
        val warnings = mutableListOf<String>(
            "Static analysis cannot prove that an extension is safe or fully functional.",
            "Nebula's WebView engine does not execute Chrome or Firefox extensions.",
        )
        if (manifestVersion == 2) warnings += "Manifest V2 requires a compatibility runtime and is not treated as Chromium MV3 compatible."
        if (manifest.optJSONObject("background")?.has("service_worker") == true) {
            warnings += "A Manifest V3 background service worker requires a native extension lifecycle."
        }
        if ("nativeMessaging" in permissions) warnings += "Native messaging needs a trusted native host and is unsupported."
        if ("<all_urls>" in permissions || "<all_urls>" in hosts) warnings += "This extension requests access to every website."
        val chromeOnly = setOf("chrome://", "chrome.i18n.getUILanguage", "chrome.sessions")
        val incompatible = chromeApis.any { it in chromeOnly }
        return ExtensionReport(
            fileName = fileName,
            name = manifest.optString("name").ifBlank { "Unnamed extension" },
            version = manifest.optString("version").ifBlank { "Unknown" },
            manifestVersion = manifestVersion,
            permissions = permissions,
            hostPermissions = hosts,
            detectedApis = chromeApis,
            warnings = warnings,
            chromiumCompatible = manifestVersion == 3 && !incompatible,
            geckoCompatible = manifestVersion in 2..3 && !incompatible,
            runtimeAvailable = false,
        )
    }

    private fun InputStream.readExact(count: Int): ByteArray {
        val result = ByteArray(count)
        var offset = 0
        while (offset < count) {
            val read = read(result, offset, count - offset)
            require(read >= 0) { "Unexpected end of package" }
            offset += read
        }
        return result
    }

    private fun InputStream.readLittleEndianInt(): Long {
        val bytes = readExact(4)
        require(bytes.size == 4) { "Unexpected end of package" }
        return bytes.foldIndexed(0L) { index, result, byte -> result or ((byte.toLong() and 0xff) shl (index * 8)) }
    }

    private fun InputStream.skipFully(count: Int) {
        var remaining = count
        while (remaining > 0) {
            val skipped = skip(remaining.toLong())
            if (skipped <= 0) require(read() >= 0) { "Unexpected end of package" } else remaining -= skipped.toInt()
            if (skipped <= 0) remaining--
        }
    }

    private fun org.json.JSONArray?.toStringList(): List<String> = this?.let { array ->
        List(array.length()) { index -> array.optString(index) }.filter { it.isNotBlank() }
    }.orEmpty()
}

private class PrefixInputStream(private val prefix: ByteArray, private val delegate: InputStream) : InputStream() {
    private var position = 0
    override fun read(): Int = if (position < prefix.size) prefix[position++].toInt() and 0xff else delegate.read()
    override fun read(b: ByteArray, off: Int, len: Int): Int {
        if (position >= prefix.size) return delegate.read(b, off, len)
        val count = minOf(len, prefix.size - position)
        System.arraycopy(prefix, position, b, off, count)
        position += count
        return count
    }
}
