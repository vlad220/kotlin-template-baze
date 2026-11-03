package com.template.util

import android.content.Context
import android.content.IntentFilter
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.auth.api.phone.SmsRetrieverClient
import com.google.android.gms.tasks.Task

class SmsAutofillHelper(private val context: Context) {
    private var smsReceiver: SmsReceiver? = null

    fun startListening(onCode: (String) -> Unit) {
        SmsReceiver.onCodeReceived = onCode
        smsReceiver = SmsReceiver()
        val intentFilter = IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION)
        context.registerReceiver(smsReceiver, intentFilter)
        val client: SmsRetrieverClient = SmsRetriever.getClient(context)
        val task: Task<Void> = client.startSmsRetriever()
        task.addOnFailureListener { stopListening() }
    }

    fun stopListening() {
        try { smsReceiver?.let { context.unregisterReceiver(it) } } catch (_: Exception) {}
        smsReceiver = null
    }
}
