package org.sroot.app

import android.content.Context
import org.json.JSONObject

data class SupportProfile(
    val profileId: String,
    val displayName: String,
    val models: Set<String>,
    val fingerprintPrefixes: List<String>,
    val androidReleases: Set<String>,
    val abis: Set<String>,
    val mode: String,
) {
    fun matches(snapshot: DeviceSnapshot): Boolean {
        val fingerprintMatches = fingerprintPrefixes.any(snapshot.fingerprint::startsWith)
        return snapshot.model in models &&
            fingerprintMatches &&
            snapshot.androidRelease in androidReleases &&
            snapshot.abi in abis
    }
}

class SupportManifest(
    val schemaVersion: Int,
    val profiles: List<SupportProfile>,
) {
    fun match(snapshot: DeviceSnapshot): SupportProfile? =
        profiles.firstOrNull { it.matches(snapshot) }

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
