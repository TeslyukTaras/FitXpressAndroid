package com.hexis.bi.data.terra

/**
 * One identity in a multi-source pull: [terraUserId] is the query key for Terra REST v2;
 * [provider] is a display or Firestore label (e.g. OURA, HEALTH_CONNECT).
 */
data class TerraRestIdentity(
    val terraUserId: String,
    val provider: String,
)
