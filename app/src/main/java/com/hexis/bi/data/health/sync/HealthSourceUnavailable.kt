package com.hexis.bi.data.health.sync

import java.io.IOException

class HealthSourceUnavailable(cause: Throwable) : IOException("health source unreachable", cause)
