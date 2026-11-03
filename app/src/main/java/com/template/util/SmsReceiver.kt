package com.template.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (SmsRetriever.SMS_RETRIEVED_ACTION == intent.action) {
            val status = intent.extras?.getParcelable(SmsRetriever.EXTRA_STATUS, Status::class.java)
            val message = intent.extras?.getString(SmsRetriever.EXTRA_SMS_MESSAGE)
            if (status?.statusCode == CommonStatusCodes.SUCCESS && message != null) {
                val code = message.replace(Regex("[^0-9]"), "").take(6)
                onCodeReceived?.invoke(code)
            }
        }
    }

    companion object { var onCodeReceived: ((String) -> Unit)? = null }
}
