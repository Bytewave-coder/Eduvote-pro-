package com.example.data

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat

class TelegramJobService : JobService() {

    override fun onStartJob(params: JobParameters?): Boolean {
        try {
            val serviceIntent = Intent(applicationContext, TelegramForegroundService::class.java)
            ContextCompat.startForegroundService(applicationContext, serviceIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // Reschedule next check
        scheduleJob(applicationContext)
        jobFinished(params, false)
        return false
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        return true // Reschedule if stopped unexpectedly
    }

    companion object {
        private const val JOB_ID = 8888

        fun scheduleJob(context: Context) {
            try {
                val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler ?: return
                val componentName = ComponentName(context, TelegramJobService::class.java)
                
                val builder = JobInfo.Builder(JOB_ID, componentName)
                    .setPersisted(true) // Keeps scheduled job alive across device reboot & process kills
                    .setMinimumLatency(15 * 1000L) // Check every 15 seconds
                    .setOverrideDeadline(30 * 1000L) // Force execute within 30 seconds max

                jobScheduler.schedule(builder.build())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
