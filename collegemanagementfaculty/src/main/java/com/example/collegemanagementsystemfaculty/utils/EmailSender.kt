package com.example.collegemanagementsystemfaculty.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

import com.example.collegemanagementsystemfaculty.BuildConfig

object EmailSender {

    private val SENDER_EMAIL    = BuildConfig.SMTP_SENDER_EMAIL
    private val SENDER_PASSWORD = BuildConfig.SMTP_SENDER_PASSWORD
    private const val SENDER_NAME = "College Management System"

    fun sendOtp(
        toEmail: String,
        toName: String,
        otp: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val props = Properties().apply {
                    put("mail.smtp.host", "smtp.gmail.com")
                    put("mail.smtp.port", "587")
                    put("mail.smtp.auth", "true")
                    put("mail.smtp.starttls.enable", "true")
                    put("mail.smtp.ssl.trust", "smtp.gmail.com")
                }

                val session = Session.getInstance(props, object : Authenticator() {
                    override fun getPasswordAuthentication() =
                        PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD)
                })

                val message = MimeMessage(session).apply {
                    setFrom(InternetAddress(SENDER_EMAIL, SENDER_NAME))
                    setRecipients(
                        Message.RecipientType.TO,
                        InternetAddress.parse(toEmail)
                    )
                    subject = "Your OTP - College Management System"
                    setContent(buildEmailBody(toName, otp), "text/html; charset=utf-8")
                }

                Transport.send(message)
                onSuccess()

            } catch (e: Exception) {
                onFailure(e.message ?: "Unknown error")
            }
        }
    }

    // ✅ Fixed Email Body — mobile friendly
    private fun buildEmailBody(name: String, otp: String): String {

        // ✅ Split OTP into individual digit boxes
        val otpBoxes = otp.map { digit ->
            """
            <td style="
                width: 36px;
                height: 44px;
                background: #E3F2FD;
                border-radius: 8px;
                text-align: center;
                vertical-align: middle;
                font-size: 24px;
                font-weight: bold;
                color: #1565C0;
                padding: 0 4px;
            ">$digit</td>
            <td style="width: 6px;"></td>
            """.trimIndent()
        }.joinToString("")

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
            </head>
            <body style="
                font-family: Arial, sans-serif;
                background-color: #f0f4f8;
                margin: 0;
                padding: 16px;">

                <!-- ✅ Outer wrapper -->
                <table width="100%" cellpadding="0" cellspacing="0">
                    <tr>
                        <td align="center">

                            <!-- ✅ Card -->
                            <table width="100%" style="
                                max-width: 480px;
                                background: #ffffff;
                                border-radius: 16px;
                                padding: 32px 24px;
                                box-shadow: 0 4px 20px rgba(0,0,0,0.08);">

                                <!-- ✅ Header -->
                                <tr>
                                    <td align="center" style="padding-bottom: 24px;">
                                        <div style="
                                            background: linear-gradient(135deg, #1565C0, #42A5F5);
                                            border-radius: 12px;
                                            padding: 12px 24px;
                                            display: inline-block;">
                                            <span style="
                                                color: white;
                                                font-size: 18px;
                                                font-weight: bold;
                                                letter-spacing: 0.5px;">
                                                🎓 College Management System
                                            </span>
                                        </div>
                                    </td>
                                </tr>

                                <!-- ✅ Greeting -->
                                <tr>
                                    <td style="padding-bottom: 8px;">
                                        <p style="
                                            color: #212121;
                                            font-size: 16px;
                                            margin: 0;">
                                            Hello, <strong>$name</strong>! 👋
                                        </p>
                                    </td>
                                </tr>

                                <!-- ✅ Description -->
                                <tr>
                                    <td style="padding-bottom: 24px;">
                                        <p style="
                                            color: #555;
                                            font-size: 14px;
                                            margin: 0;
                                            line-height: 1.6;">
                                            Your One-Time Password (OTP) for 
                                            registration is:
                                        </p>
                                    </td>
                                </tr>

                                <!-- ✅ OTP Digit Boxes — NO wrapping -->
                                <tr>
                                    <td align="center" style="padding-bottom: 24px;">
                                        <table cellpadding="0" cellspacing="0"
                                            style="display: inline-table;">
                                            <tr>$otpBoxes</tr>
                                        </table>
                                    </td>
                                </tr>

                                <!-- ✅ Validity Note -->
                                <tr>
                                    <td style="padding-bottom: 24px;">
                                        <table width="100%" style="
                                            background: #FFF8E1;
                                            border-radius: 10px;
                                            padding: 12px 16px;">
                                            <tr>
                                                <td>
                                                    <p style="
                                                        color: #F57F17;
                                                        font-size: 13px;
                                                        margin: 0;
                                                        line-height: 1.6;">
                                                        ⏰ This OTP is valid for 
                                                        <strong>10 minutes</strong>.<br>
                                                        🔒 Do not share this OTP 
                                                        with anyone.
                                                    </p>
                                                </td>
                                            </tr>
                                        </table>
                                    </td>
                                </tr>

                                <!-- ✅ Divider -->
                                <tr>
                                    <td style="
                                        border-top: 1px solid #EEEEEE;
                                        padding-top: 16px;">
                                        <p style="
                                            color: #9E9E9E;
                                            font-size: 12px;
                                            text-align: center;
                                            margin: 0;">
                                            If you didn't request this OTP,<br>
                                            please contact your admin immediately.
                                        </p>
                                    </td>
                                </tr>

                            </table>
                        </td>
                    </tr>
                </table>

            </body>
            </html>
        """.trimIndent()
    }
}
