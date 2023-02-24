package org.bidon.sdk.databinders.session

internal interface SessionTracker {
    val sessionId: String
    val launchTs: Long
    val launchMonotonicTs: Long
    val startTs: Long
    val startMonotonicTs: Long
    val ts: Long
    val monotonicTs: Long

    val memoryWarningsTs: List<Long>
    val memoryWarningsMonotonicTs: List<Long>
}
