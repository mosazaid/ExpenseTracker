package com.mousa.mizani.core.security

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject

/**
 * Whether this phone can unlock the app lock (fingerprint or a screen lock). Turning the lock on
 * without it would keep the user out of their own data.
 */
fun interface AppLockAvailability {
    fun isAvailable(): Boolean
}

class BiometricAppLockAvailability @Inject constructor(
    private val biometricAuthManager: BiometricAuthManager
) : AppLockAvailability {
    override fun isAvailable(): Boolean =
        biometricAuthManager.checkAvailability() == BiometricAvailability.Available
}

@Module
@InstallIn(SingletonComponent::class)
abstract class AppLockAvailabilityModule {

    @Binds
    abstract fun bindAppLockAvailability(impl: BiometricAppLockAvailability): AppLockAvailability
}
