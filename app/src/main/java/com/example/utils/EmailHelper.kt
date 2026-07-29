package com.example.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object EmailHelper {

    fun sendToGmail(
        context: Context,
        recipientEmail: String,
        subject: String,
        body: String
    ) {
        val targetEmail = recipientEmail.ifBlank { "" }
        val mailUri = Uri.parse("mailto:" + Uri.encode(targetEmail))

        // First attempt specifically targeting Gmail app package if installed
        val gmailIntent = Intent(Intent.ACTION_SENDTO, mailUri).apply {
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            setPackage("com.google.android.gm")
        }

        val chooserIntent = Intent.createChooser(
            Intent(Intent.ACTION_SENDTO, mailUri).apply {
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
            },
            "ارسال یادداشت به جیمیل"
        )

        try {
            if (gmailIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(gmailIntent)
            } else {
                context.startActivity(chooserIntent)
            }
        } catch (e: Exception) {
            try {
                // Fallback to general ACTION_SEND intent
                val generalIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "message/rfc822"
                    if (targetEmail.isNotBlank()) {
                        putExtra(Intent.EXTRA_EMAIL, arrayOf(targetEmail))
                    }
                    putExtra(Intent.EXTRA_SUBJECT, subject)
                    putExtra(Intent.EXTRA_TEXT, body)
                }
                context.startActivity(Intent.createChooser(generalIntent, "ارسال ایمیل"))
            } catch (ex: Exception) {
                Toast.makeText(context, "برنامه‌ای برای ارسال ایمیل پیدا نشد", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
