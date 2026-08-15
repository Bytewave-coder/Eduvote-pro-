package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.example.data.CandidateWithStudent
import java.io.File
import java.io.FileOutputStream

import android.graphics.BitmapFactory
import android.net.Uri

object ExportChartHelper {
    fun generateWinnerImage(context: Context, electionTitle: String, winner: CandidateWithStudent): File? {
        val hasPhoto = !winner.student.photoUri.isNullOrBlank()
        val hasLogo = !winner.candidate.partySymbolUri.isNullOrBlank()
        if (!hasPhoto && !hasLogo) return null

        val width = 800
        val height = 600
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background
        val bgPaint = Paint().apply { color = Color.parseColor("#121212") }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Title
        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 32f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("Winner: $electionTitle", width / 2f, 60f, titlePaint)

        // Name and Party
        val namePaint = Paint().apply {
            color = Color.parseColor("#BB86FC")
            textSize = 40f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("${winner.student.name} (${winner.candidate.partyName})", width / 2f, height - 60f, namePaint)
        
        val valPaint = Paint().apply {
            color = Color.LTGRAY
            textSize = 30f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("${winner.candidate.voteCount} votes", width / 2f, height - 20f, valPaint)

        // Draw Images
        try {
            val contentResolver = context.contentResolver
            if (hasPhoto) {
                val uri = Uri.parse(winner.student.photoUri)
                val photoBitmap = android.provider.MediaStore.Images.Media.getBitmap(contentResolver, uri)
                if (photoBitmap != null) {
                    val scaled = Bitmap.createScaledBitmap(photoBitmap, 250, 250, true)
                    canvas.drawBitmap(scaled, width / 2f - 280f, 150f, null)
                }
            }
            if (hasLogo) {
                val uri = Uri.parse(winner.candidate.partySymbolUri)
                val logoBitmap = android.provider.MediaStore.Images.Media.getBitmap(contentResolver, uri)
                if (logoBitmap != null) {
                    val scaled = Bitmap.createScaledBitmap(logoBitmap, 250, 250, true)
                    canvas.drawBitmap(scaled, width / 2f + 30f, 150f, null)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val file = File(context.cacheDir, "winner_export.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return file
    }

    fun generateChart(context: Context, electionTitle: String, candidates: List<CandidateWithStudent>): File {
        val width = 800
        val height = 600
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background
        val bgPaint = Paint().apply { color = Color.parseColor("#121212") }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Title
        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 40f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("Live Voting: $electionTitle", width / 2f, 60f, titlePaint)

        if (candidates.isEmpty()) {
            val emptyPaint = Paint().apply {
                color = Color.GRAY
                textSize = 30f
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText("No candidates available", width / 2f, height / 2f, emptyPaint)
        } else {
            val maxVotes = candidates.maxOfOrNull { it.candidate.voteCount }?.coerceAtLeast(1) ?: 1
            val startY = 120f
            val endY = height - 40f
            val availableHeight = endY - startY
            val barHeight = availableHeight / candidates.size - 20f
            
            val maxBarWidth = width - 300f // space for name and count

            val namePaint = Paint().apply {
                color = Color.LTGRAY
                textSize = 24f
                isAntiAlias = true
            }
            val valPaint = Paint().apply {
                color = Color.WHITE
                textSize = 24f
                isAntiAlias = true
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val barPaint = Paint().apply {
                color = Color.parseColor("#BB86FC") // Primary Color
                isAntiAlias = true
            }

            var currentY = startY
            for (c in candidates.sortedByDescending { it.candidate.voteCount }) {
                val voteCount = c.candidate.voteCount
                val barWidth = (voteCount.toFloat() / maxVotes) * maxBarWidth

                // Name
                canvas.drawText(c.student.name.take(15), 20f, currentY + barHeight / 2 + 8f, namePaint)

                // Bar
                val rect = RectF(200f, currentY, 200f + barWidth, currentY + barHeight)
                canvas.drawRoundRect(rect, 8f, 8f, barPaint)

                // Value
                canvas.drawText("$voteCount votes", 200f + barWidth + 10f, currentY + barHeight / 2 + 8f, valPaint)

                currentY += barHeight + 20f
            }
        }

        val file = File(context.cacheDir, "chart_export.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return file
    }
}
