package org.sroot.app

import android.content.Context
import org.json.JSONObject

data class SupportProfile(
    val profileId: String,
    val displayName: String,
    val priority: Int,
    val models: Set<String>,
    val fingerprintPrefixes: List<String>,
    val androidReleases: Set<String>,
    val abis: Set<String>,
    val mode: String,
) {
    fun matchScore(snapshot: DeviceSnapshot): Int? {
        if (snapshot.model !in models) {
            return null
        }
        if (androidReleases.isNotEmpty() &&
            snapshot.androidRelease !in androidReleases
        ) {
            return null
        }
        if (abis.isNotEmpty() && snapshot.abi !in abis) {
            return null
        }

        val fingerprintLength = fingerprintPrefixes
            .filter(snapshot.fingerprint::startsWith)
            .maxOfOrNull(String::length)
            ?: return null

        val androidScore = if (androidReleases.isNotEmpty()) 10 else 0
        val abiScore = if (abis.isNotEmpty()) 1 else 0
        return priority * 1_000_000 +
            100_000 +
            fingerprintLength * 100 +
            androidScore +
            abiScore
    }
}

class SupportManifest(
    val schemaVersion: Int,
    val profiles: List<SupportProfile>,
) {
    fun match(snapshot: DeviceSnapshot): SupportProfile? =
        profiles
            .mapNotNull { profile ->
                profile.matchScore(snapshot)?.let { score -> score to profile }
            }
            .maxByOrNull { it.first }
            ?.second

    companion object {
        fun load(context: Context): SupportManifest {
            val text = context.assets
                .open("support_manifest.json")
                .bufferedReader()
                .use { it.readText() }
            val root = JSONObject(text)
            val profilesJson = root.getJSONArray("profiles")
            val profiles = buildList {
                for (index in 0 until profilesJson.length()) {
                    val item = profilesJson.getJSONObject(index)
                    add(
                        SupportProfile(
                            profileId = item.getString("profileId"),
                            displayName = item.getString("displayName"),
                            priority = item.optInt("priority", 0),
                            models = item.stringSet("models"),
                            fingerprintPrefixes = item.stringList("fingerprintPrefixes"),
                            androidReleases = item.stringSet("androidReleases"),
                            abis = item.stringSet("abis"),
                            mode = item.getString("mode"),
                        ),
                    )
                }
            }
            return SupportManifest(root.getInt("schemaVersion"), profiles)
        }
    }
}

private fun JSONObject.stringList(name: String): List<String> {
    val array = getJSONArray(name)
    return buildList {
        for (index in 0 until array.length()) {
            add(array.getString(index))
        }
    }
}

private fun JSONObject.stringSet(name: String): Set<String> =
    stringList(name).toSet()
