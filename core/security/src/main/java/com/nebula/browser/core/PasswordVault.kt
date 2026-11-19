package com.nebula.browser.core

import java.security.SecureRandom
import org.json.JSONArray
import org.json.JSONObject

class PasswordVault(private val secrets: SecretStore) {
    @Synchronized
    fun all(): List<PasswordCredential> = runCatching {
        val raw = secrets.get(VAULT_KEY) ?: return emptyList()
        val array = JSONArray(raw)
        buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(PasswordCredential(
                    id = item.getString("id"),
                    title = item.getString("title"),
                    username = item.getString("username"),
                    password = item.getString("password"),
                    url = item.optString("url"),
                    notes = item.optString("notes"),
                    updatedAt = item.optLong("updatedAt"),
                ))
            }
        }
    }.getOrDefault(emptyList())

    @Synchronized
    fun save(credential: PasswordCredential) {
        val updated = all().filterNot { it.id == credential.id } + credential
        persist(updated)
    }

    @Synchronized
    fun delete(id: String) = persist(all().filterNot { it.id == id })

    fun generatePassword(length: Int = 20): String {
        require(length >= 12) { "Use at least 12 characters" }
        val groups = listOf(UPPER, LOWER, DIGITS, SYMBOLS)
        val alphabet = groups.joinToString("")
        val chars = (groups.map { it.random() } + List(length - groups.size) { alphabet.random() }).toMutableList()
        java.util.Collections.shuffle(chars, random)
        return chars.joinToString("")
    }

    private fun persist(credentials: List<PasswordCredential>) {
        val array = JSONArray()
        credentials.forEach { credential ->
            array.put(JSONObject().apply {
                put("id", credential.id)
                put("title", credential.title)
                put("username", credential.username)
                put("password", credential.password)
                put("url", credential.url)
                put("notes", credential.notes)
                put("updatedAt", credential.updatedAt)
            })
        }
        secrets.put(VAULT_KEY, array.toString())
    }

    private fun String.random(): Char = this[random.nextInt(length)]

    private companion object {
        const val VAULT_KEY = "password_vault"
        const val UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ"
        const val LOWER = "abcdefghijkmnopqrstuvwxyz"
        const val DIGITS = "23456789"
        const val SYMBOLS = "!@#$%^&*()-_=+"
        val random = SecureRandom()
    }
}
