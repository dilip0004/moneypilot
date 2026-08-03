package com.yourname.moneypilot.util

import java.io.InputStream
import java.io.BufferedReader
import java.io.InputStreamReader

object CsvParser {
    fun parse(inputStream: InputStream): List<List<String>> {
        return try {
            val reader = BufferedReader(InputStreamReader(inputStream))
            val result = mutableListOf<List<String>>()
            
            reader.useLines { lines ->
                lines.forEach { line ->
                    if (line.isNotBlank()) {
                        result.add(parseLine(line))
                    }
                }
            }
            result
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false
        
        for (char in line) {
            when (char) {
                '\"' -> inQuotes = !inQuotes
                ',' -> {
                    if (inQuotes) {
                        current.append(char)
                    } else {
                        result.add(current.toString().trim())
                        current = StringBuilder()
                    }
                }
                else -> current.append(char)
            }
        }
        result.add(current.toString().trim())
        return result
    }
}
