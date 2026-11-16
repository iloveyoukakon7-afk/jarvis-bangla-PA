package com.example.security

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

sealed interface BiometricAvailability {
    data object Ready : BiometricAvailability
    data object NotEnrolled : BiometricAvailability
    data class Unavailable(val reasonBangla: String) : BiometricAvailability
}

object VoiceBiometricManager {

    private const val ALLOWED_AUTHENTICATORS =
        BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL

    fun checkAvailability(context: Context): BiometricAvailability {
        val manager = BiometricManager.from(context)
        return when (manager.canAuthenticate(ALLOWED_AUTHENTICATORS)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.Ready
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NotEnrolled
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
                BiometricAvailability.Unavailable("এই ডিভাইসে বায়োমেট্রিক হার্ডওয়্যার নেই।")
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
                BiometricAvailability.Unavailable("বায়োমেট্রিক সেন্সর এখন ব্যস্ত বা অনুপলব্ধ।")
            else ->
                BiometricAvailability.Unavailable("ডিভাইস সিকিউরিটি সেটআপ করা নেই।")
        }
    }

    /**
     * Checks whether the spoken Bangla/Romanized phrase matches the Boss's registered unlock phrase.
     */
    fun matchesUnlockPhrase(spoken: String, configuredPhrase: String): Boolean {
        val normSpoken = normalizePhrase(spoken)
        val normConfigured = normalizePhrase(configuredPhrase)
        if (normSpoken.isBlank() || normConfigured.isBlank()) return false
        if (normSpoken.contains(normConfigured) || normConfigured == normSpoken) return true

        val configuredTokens = normConfigured.split(" ").filter { it.length >= 2 }
        if (configuredTokens.isEmpty()) return false
        val matchedTokens = configuredTokens.count { token -> normSpoken.contains(token) }
        return matchedTokens >= ((configuredTokens.size + 1) / 2).coerceAtLeast(1) &&
            (normSpoken.contains("খোল") || normSpoken.contains("আনলক") ||
                normSpoken.contains("khol") || normSpoken.contains("unlock") ||
                matchedTokens == configuredTokens.size)
    }

    /**
     * Launches Android's official BiometricPrompt (Voice Match + Official OS Biometric/Credential)
     * without ever bypassing the OS lock security.
     */
    fun authenticateWithOfficialPrompt(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val availability = checkAvailability(activity)
        if (availability !is BiometricAvailability.Ready) {
            val msg = when (availability) {
                is BiometricAvailability.NotEnrolled ->
                    "বস, অনুগ্রহ করে ফোনের সেটিংস থেকে ফিঙ্গারপ্রিন্ট, ফেস বা পিন সেট করুন।"
                is BiometricAvailability.Unavailable -> availability.reasonBangla
                BiometricAvailability.Ready -> ""
            }
            onError(msg)
            return
        }

        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    dismissKeyguardIfLocked(activity)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError("বস, বায়োমেট্রিক যাচাই বাতিল হয়েছে: $errString")
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onError("বস, বায়োমেট্রিক মেলেনি। আবার চেষ্টা করুন।")
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("বস ভয়েস ও বায়োমেট্রিক যাচাই")
            .setSubtitle("কণ্ঠস্বর মিলেছে — অফিশিয়াল বায়োমেট্রিক দিয়ে নিশ্চিত করুন")
            .setAllowedAuthenticators(ALLOWED_AUTHENTICATORS)
            .build()

        prompt.authenticate(promptInfo)
    }

    private fun dismissKeyguardIfLocked(activity: FragmentActivity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val keyguardManager =
                activity.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            if (keyguardManager?.isKeyguardLocked == true) {
                keyguardManager.requestDismissKeyguard(activity, null)
            }
        }
    }

    private fun normalizePhrase(raw: String): String {
        return raw.lowercase()
            .replace(",", " ")
            .replace("।", " ")
            .replace(".", " ")
            .replace("!", " ")
            .replace("?", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
