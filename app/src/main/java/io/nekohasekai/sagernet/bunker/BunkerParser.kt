package io.nekohasekai.sagernet.bunker

import io.nekohasekai.sagernet.database.SettingsMapper
import io.nekohasekai.sagernet.outbound.Outbound
import io.nekohasekai.sagernet.outbound.`import`.ProfileImport

/**
 * Bunker Cat's preparation layer.
 *
 * The protocol/document parser remains the audited ProfileImport implementation; this layer applies
 * Bunker-specific endpoint de-duplication and stable export names without changing the VPN core.
 */
object BunkerParser {

    data class Result(
        val profiles: List<Outbound>,
        val parsedCount: Int,
        val duplicateCount: Int,
        val rejectedMessages: List<String>,
    )

    /**
     * Parses pasted text, subscription bodies or file contents supported by ProfileImport.
     *
     * Duplicate identity intentionally follows the Bunker endpoint policy: protocol + server + port.
     * Profiles with no server address are kept separately rather than accidentally collapsed.
     */
    fun parse(text: String): Result {
        val imported = ProfileImport.parse(
            text,
            SettingsMapper.xrayVlessPreference(),
            SettingsMapper.buildContext(),
        )

        val unique = LinkedHashMap<String, Outbound>()
        var anonymousIndex = 0
        imported.outbounds.forEach { profile ->
            val host = profile.server.trim().lowercase()
            val key = if (host.isNotEmpty() && profile.serverPort > 0) {
                "${profile.type.lowercase()}|$host|${profile.serverPort}"
            } else {
                "anonymous|${profile.type.lowercase()}|${profile.displayName()}|${anonymousIndex++}"
            }
            if (!unique.containsKey(key)) unique[key] = profile
        }

        val profiles = unique.values.toList()
        profiles.forEachIndexed { index, profile ->
            profile.name = "🐾 Бункер Кота №${(index + 1).toString().padStart(3, '0')}"
        }

        return Result(
            profiles = profiles,
            parsedCount = imported.outbounds.size,
            duplicateCount = imported.outbounds.size - profiles.size,
            rejectedMessages = imported.messages,
        )
    }
}
