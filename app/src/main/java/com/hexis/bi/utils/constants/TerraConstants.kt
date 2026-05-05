package com.hexis.bi.utils.constants

internal object TerraProviders {
    const val HEALTH_CONNECT = "HEALTH_CONNECT"
    const val DUMMY = "DUMMY"

    // Wearables (Terra resource codes)
    const val OURA = "OURA"
    const val WHOOP = "WHOOP"
    const val FITBIT = "FITBIT"
    const val GARMIN = "GARMIN"
    const val POLAR = "POLAR"
    const val SUUNTO = "SUUNTO"
    const val WITHINGS = "WITHINGS"
    const val ZEPP = "ZEPP"
    const val BIOSTRAP = "BIOSTRAP"
    const val HEALTHGAUGE = "HEALTHGAUGE"
    const val INBODY = "INBODY"
    const val SOMNOFY = "SOMNOFY"
    const val PUL = "PUL"
    /** EU/non-US Omron (Terra enum `OMRON`; legacy token `OMRONEU` still matched). */
    const val OMRON = "OMRON"
    const val OMRONUS = "OMRONUS"
    const val AKTIIA = "AKTIIA"
    const val HUAWEI = "HUAWEI"
    const val DEXCOM = "DEXCOM"
    /** Terra docs: DEXCOM_EU (legacy token `DEXCOMEU` still matched). */
    const val DEXCOM_EU = "DEXCOM_EU"
    const val CARDIOMOOD = "CARDIOMOOD"
    const val MOXY = "MOXY"

    // Other apps
    const val GOOGLE = "GOOGLE"
    const val HAMMERHEAD = "HAMMERHEAD"
    const val XOSS = "XOSS"
    const val LEZYNE = "LEZYNE"
    const val TECHNOGYM = "TECHNOGYM"
    const val CONCEPT2 = "CONCEPT2"
    const val DECATHLON = "DECATHLON"
    const val CATAPULTONE = "CATAPULTONE"
    const val ULTRAHUMAN = "ULTRAHUMAN"
    const val TRAININGPEAKS = "TRAININGPEAKS"
    const val ELITEHRV = "ELITEHRV"
    const val HEVY = "HEVY"
    const val TRAINXHALE = "TRAINXHALE"
    const val TRAINASONE = "TRAINASONE"
    const val RIDEWITHGPS = "RIDEWITHGPS"
    const val MAPMYTRACKS = "MAPMYTRACKS"
    const val MAPMYFITNESS = "MAPMYFITNESS"
    const val VELOHERO = "VELOHERO"
    const val XERT = "XERT"
    const val CLUE = "CLUE"
    const val WGER = "WGER"
    const val NUTRACHECK = "NUTRACHECK"
    const val MACROSFIRST = "MACROSFIRST"
    const val MYMACROSPLUS = "MYMACROSPLUS"
    const val MYFITNESSPAL = "MYFITNESSPAL"
    const val FATSECRET = "FATSECRET"
    const val EATTHISMUCH = "EATTHISMUCH"
    const val KETOMOJOEU = "KETOMOJOEU"
    const val KETOMOJOUS = "KETOMOJOUS"
    const val BODITRAX = "BODITRAX"
    const val NOLIO = "NOLIO"
    const val VIRTUAGYM = "VIRTUAGYM"
    const val STRAVA = "STRAVA"
    const val ZWIFT = "ZWIFT"
    const val CRONOMETER = "CRONOMETER"
    const val CORE = "CORE"
    const val ROUVY = "ROUVY"
    const val FINALSURGE = "FINALSURGE"
    const val BRYTONSPORT = "BRYTONSPORT"
    const val KOMOOT = "KOMOOT"
    const val TRAINERROAD = "TRAINERROAD"
    const val CYCLINGANALYTICS = "CYCLINGANALYTICS"
    const val UNDERARMOUR = "UNDERARMOUR"
    const val FLO = "FLO"
    const val TREDICT = "TREDICT"
    const val TRIDOT = "TRIDOT"

    val WEARABLE_CODES: Set<String> = setOf(
        OURA,
        WHOOP,
        FITBIT,
        GARMIN,
        POLAR,
        SUUNTO,
        WITHINGS,
        ZEPP,
        BIOSTRAP,
        HEALTHGAUGE,
        INBODY,
        SOMNOFY,
        PUL,
        OMRON,
        OMRONUS,
        AKTIIA,
        HUAWEI,
        DEXCOM,
        DEXCOM_EU,
        CARDIOMOOD,
        MOXY,
        // Legacy provider strings from older Terra callbacks / stored rows
        "OMRONEU",
        "DEXCOMEU",
    )

    val APP_CODES: Set<String> = setOf(
        GOOGLE,
        HAMMERHEAD,
        XOSS,
        LEZYNE,
        TECHNOGYM,
        CONCEPT2,
        DECATHLON,
        CATAPULTONE,
        ULTRAHUMAN,
        TRAININGPEAKS,
        ELITEHRV,
        HEVY,
        TRAINXHALE,
        TRAINASONE,
        RIDEWITHGPS,
        MAPMYTRACKS,
        MAPMYFITNESS,
        VELOHERO,
        XERT,
        CLUE,
        WGER,
        NUTRACHECK,
        MACROSFIRST,
        MYMACROSPLUS,
        MYFITNESSPAL,
        FATSECRET,
        EATTHISMUCH,
        KETOMOJOEU,
        KETOMOJOUS,
        BODITRAX,
        NOLIO,
        VIRTUAGYM,
        STRAVA,
        ZWIFT,
        CRONOMETER,
        CORE,
        ROUVY,
        FINALSURGE,
        BRYTONSPORT,
        KOMOOT,
        TRAINERROAD,
        CYCLINGANALYTICS,
        UNDERARMOUR,
        FLO,
        TREDICT,
        TRIDOT,
    )

    /** Firestore / callback may still use older Terra resource spellings. */
    fun storedMatchesUi(storedProvider: String, uiCode: String): Boolean {
        val s = storedProvider.trim()
        val u = uiCode.trim()
        if (s.equals(u, ignoreCase = true)) return true
        return when (u.uppercase()) {
            OMRON -> s.equals("OMRONEU", ignoreCase = true)
            DEXCOM_EU -> s.equals("DEXCOMEU", ignoreCase = true)
            else -> false
        }
    }
}

internal object TerraCacheConstants {
    const val RANGE_CACHE_TTL_MS = 60_000L
}

internal object HealthConnectConstants {
    const val PACKAGE_NAME = "com.google.android.apps.healthdata"
    const val MARKET_URI = "market://details?id=$PACKAGE_NAME"
    const val PLAY_STORE_URI = "https://play.google.com/store/apps/details?id=$PACKAGE_NAME"
}
