package com.gymbro.mad

import android.app.Application

/**
 * Application class.
 *
 * Empty for now, and that is deliberate: the repositories live in the `Repos`
 * object and build themselves lazily, so there is nothing to initialise at
 * startup. It is declared in the manifest from the first commit because the
 * things that will need it - the Room database in A-5, a notification channel
 * for the rest timer in P-4 - are both easier to add to a class that already
 * exists than to introduce along with a manifest change.
 */
class GymBroApplication : Application()
