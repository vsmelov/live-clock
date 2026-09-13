package com.vsmelov.liveclock.data

import com.vsmelov.liveclock.domain.LifeState

/**
 * Export and restore of the whole log.
 *
 * This exists because sync is off by default and the DataStore is the only copy
 * of anything: a lost phone is a lost history. The format is the same JSON the
 * app stores, pretty-printed, so the file can be read and edited by hand.
 */
object Backup {

    /** Suggested file name. The date makes several backups sort sensibly. */
    fun fileName(today: String): String = "live-clock-$today.json"

    fun encode(state: LifeState): String =
        LifeClockJson.encodeToString(state.toDto())

    /**
     * Reads a backup. Returns null on anything unreadable rather than throwing:
     * the caller is a button press, and a corrupt file is a message to the user,
     * not a crash.
     */
    fun decode(raw: String): LifeState? = try {
        LifeClockJson.decodeFromString<LifeStateDto>(raw).toDomain()
    } catch (error: IllegalArgumentException) {
        null
    }
}
