package io.pleo.antaeus.core.external

import io.pleo.antaeus.models.Notification

interface NotificationProvider {

    /*
        Send email notification.
     */

    fun send(notification: Notification)
}