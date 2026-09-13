package com.vsmelov.liveclock.sync

import com.vsmelov.liveclock.domain.LifeEvent

/**
 * The default implementation: goes nowhere and does nothing.
 *
 * Enabled out of the box, because the app has to be useful without a single line
 * of configuration and without a network.
 */
object NoopSyncClient : EventSyncClient {

    override suspend fun push(events: List<LifeEvent>) = Unit
}
