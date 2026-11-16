package com.example.security

/**
 * Deterministic Bank & Payment Exclusion Shield.
 * Guarantees JARVIS Bangla NEVER accesses bank apps, mobile financial services (MFS),
 * banking passwords, PINs, cards, or OTP messages.
 */
object BankGuardrail {

    const val REFUSAL_MESSAGE_BANGLA =
        "মাফ করবেন বস, আপনার নিরাপত্তার জন্য ব্যাংক, বিকাশ, নগদ, ওটিপি, পিন বা পেমেন্ট অ্যাপে আমার কোনো প্রবেশাধিকার নেই।"

    private val blockedKeywords = listOf(
        // Bangla banking & payment terms
        "ব্যাংক", "ব্যাঙ্ক", "বিকাশ", "নগদ", "রকেট", "উপায়", "উপায়", "সেলফিন",
        "নেক্সাস পে", "ওটিপি", "পিন", "পাসওয়ার্ড", "পাসওয়ার্ড", "পেমেন্ট", "টাকা পাঠা",
        "ব্যালেন্স দেখ", "এটিএম", "ক্রেডিট কার্ড", "ডেবিট কার্ড", "ইসলামী ব্যাংক",
        "ডাচ বাংলা", "ব্র্যাক ব্যাংক", "সিটি টাচ", "সোনালী ব্যাংক", "অগ্রণী ব্যাংক",
        // English / Romanized banking & payment terms
        "bank", "banking", "bkash", "b-kash", "nagad", "rocket", "upay",
        "cellfin", "nexuspay", "citytouch", "astha", "skybanking", "otp",
        "pin code", "atm pin", "bank password", "password", "payment",
        "gpay", "google pay", "google wallet", "paypal", "paytm", "tap",
        "islami bank", "dbbl", "brac bank", "ebl", "sonali", "ibbl"
    )

    private val blockedPackages = setOf(
        "com.bKash.customerapp",
        "com.konasl.nagad",
        "com.dbbl.mbs.customer",
        "com.sslwireless.upay",
        "com.ibbl.cellfin",
        "com.dbbl.nexus.pay",
        "com.citybank.citytouch",
        "com.bracbank.astha",
        "com.ebl.skybanking",
        "com.google.android.apps.walletnfcrel",
        "com.google.android.apps.nbu.paisa.user",
        "com.paypal.android.p2pmobile"
    )

    fun isBlockedRequest(input: String): Boolean {
        val normalized = input.lowercase().trim()
        if (normalized.isBlank()) return false
        return blockedKeywords.any { keyword -> normalized.contains(keyword) }
    }

    fun isBlockedPackage(packageName: String): Boolean {
        val lower = packageName.lowercase()
        if (blockedPackages.any { it.equals(packageName, ignoreCase = true) }) return true
        return lower.contains("bank") ||
            lower.contains("bkash") ||
            lower.contains("nagad") ||
            lower.contains("upay") ||
            lower.contains("wallet") ||
            lower.contains("pay")
    }
}
