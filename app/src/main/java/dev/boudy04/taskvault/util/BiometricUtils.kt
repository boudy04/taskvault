package dev.boudy04.taskvault.util

import android.os.Build
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL

/**
 * Allowed authenticators for the app-lock prompts.
 *
 * ponytail: on API <= 28 androidx.biometric throws IllegalArgumentException for the
 * BIOMETRIC_WEAK | DEVICE_CREDENTIAL combination, so fall back to credential-only there;
 * keep both on API 29+.
 */
fun allowedAuthenticatorsCompat(): Int =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) BIOMETRIC_WEAK or DEVICE_CREDENTIAL
    else DEVICE_CREDENTIAL
