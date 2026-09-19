package com.fitlog.app.domain

import java.math.BigInteger

object Ulid {

    const val MAX_TIMESTAMP_MS = 281474976710655L

    private const val ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"
    private const val TIME_LENGTH = 10
    private const val RANDOM_LENGTH = 16
    private val RANDOM_HEX = Regex("^[0-9a-f]{20}$")
    private val ULID_PATTERN = Regex("^[0-9A-HJKMNP-TV-Z]{26}$")

    fun fromParts(timestampMs: Long, randomHex: String): String {
        require(timestampMs in 0..MAX_TIMESTAMP_MS) { "timestamp_ms fuera de rango: $timestampMs" }
        require(RANDOM_HEX.matches(randomHex)) { "random_hex invalido: $randomHex" }
        return encode(BigInteger.valueOf(timestampMs), TIME_LENGTH) +
            encode(BigInteger(randomHex, 16), RANDOM_LENGTH)
    }

    fun generate(timestampMs: Long = System.currentTimeMillis()): String {
        val random = ByteArray(10)
        java.security.SecureRandom().nextBytes(random)
        val hex = random.joinToString("") { byte -> "%02x".format(byte) }
        return fromParts(timestampMs, hex)
    }

    fun isValid(value: String): Boolean {
        if (value.length != TIME_LENGTH + RANDOM_LENGTH) return false
        if (!ULID_PATTERN.matches(value)) return false
        return value[0] <= '7'
    }

    private fun encode(value: BigInteger, length: Int): String {
        var remaining = value
        val base = BigInteger.valueOf(32)
        val out = CharArray(length)
        for (index in length - 1 downTo 0) {
            val digit = remaining.mod(base).toInt()
            out[index] = ALPHABET[digit]
            remaining = remaining.divide(base)
        }
        return String(out)
    }
}
