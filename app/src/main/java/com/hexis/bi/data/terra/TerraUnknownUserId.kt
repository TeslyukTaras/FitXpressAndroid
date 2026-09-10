package com.hexis.bi.data.terra

import com.google.firebase.functions.FirebaseFunctionsException
import com.hexis.bi.utils.constants.TerraSyncConstants

internal fun Throwable.isTerraUnknownUserId(): Boolean {
    val functionsError = generateSequence(this, Throwable::cause)
        .filterIsInstance<FirebaseFunctionsException>()
        .firstOrNull()
        ?: return false
    return when (functionsError.code) {
        FirebaseFunctionsException.Code.NOT_FOUND -> true
        FirebaseFunctionsException.Code.INTERNAL ->
            functionsError.message?.contains(TerraSyncConstants.UNKNOWN_USER_ID_MARKER) == true

        else -> false
    }
}
