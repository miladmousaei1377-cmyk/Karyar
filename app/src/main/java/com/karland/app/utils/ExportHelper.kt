package com.karland.app.utils

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.karland.app.data.Task
import com.karland.app.data.TaskCategory
import com.karland.app.data.TaskPriority
import java.io.File
import java.io.FileOutputStream

object ExportHelper {

    fun buildTextBytes(tasks: List<Task>): ByteArray {
        val sb = StringBuilder()
        sb.appendLine("کارلند - گزارش کارها")
        sb.appendLine("تاریخ: ${JalaliCalendar.toShortDate(System.currentTimeMillis())}")
        sb.appendLine("=".repeat(40))
        val active = tasks.filter { !it.isCompleted }
        val done = tasks.filter { it.isCompleted }
        sb.appendLine("\nکارهای فعال (${active.size})")
        sb.appendLine("-".repeat(30))
        active.forEach { t ->
            sb.appendLine("• ${t.title}")
            if (t.description.isNotBlank()) sb.appendLine("  ${t.description}")
            sb.appendLine("  دسته: ${TaskCategory.valueOf(t.category).label}  اولویت: ${TaskPriority.valueOf(t.priority).label}")
            t.dueDate?.let { sb.appendLine("  سررسید: ${JalaliCalendar.toShortDate(it)}") }
        }
        sb.appendLine("\nانجام‌شده (${done.size})")
        sb.appendLine("-".repeat(30))
        done.forEach { t -> sb.appendLine("✓ ${t.title}") }
        return sb.toString().toByteArray(Charsets.UTF_8)
    }

    fun buildCsvBytes(tasks: List<Task>): ByteArray {
        val sb = StringBuilder()
        sb.appendLine("عنوان,توضیحات,دسته,اولویت,وضعیت,سررسید,ثبت")
        tasks.forEach { t ->
            val status = if (t.isCompleted) "انجام شده" else "فعال"
            val due = t.dueDate?.let { JalaliCalendar.toShortDate(it) } ?: ""
            val created = JalaliCalendar.toShortDate(t.createdAt)
            sb.appendLine("\"${t.title}\",\"${t.description}\",\"${TaskCategory.valueOf(t.category).label}\",\"${TaskPriority.valueOf(t.priority).label}\",\"$status\",\"$due\",\"$created\"")
        }
        return sb.toString().toByteArray(Charsets.UTF_8)
    }

    fun writePdfToStream(tasks: List<Task>, outputStream: java.io.OutputStream) {
        val doc = PdfDocument()
        val page = doc.startPage(PdfDocument.PageInfo.Builder(595,842,1).create())
        val canvas = page.canvas
        val paint = Paint().apply { textSize=13f; isAntiAlias=true; color=android.graphics.Color.BLACK }
        val title = Paint().apply { textSize=18f; isAntiAlias=true; isFakeBoldText=true; color=android.graphics.Color.rgb(46,125,50) }
        var y=50f
        canvas.drawText("کارلند - گزارش کارها", 50f, y, title); y+=28f
        canvas.drawText("تاریخ: ${JalaliCalendar.toShortDate(System.currentTimeMillis())}", 50f, y, paint); y+=22f
        canvas.drawLine(50f,y,545f,y,paint); y+=22f
        val active=tasks.filter{!it.isCompleted}; val done=tasks.filter{it.isCompleted}
        paint.isFakeBoldText=true; canvas.drawText("کارهای فعال (${active.size})",50f,y,paint); y+=20f; paint.isFakeBoldText=false
        active.forEach { t -> if(y<800f){ canvas.drawText("• ${t.title}",60f,y,paint); y+=18f } }
        y+=8f; paint.isFakeBoldText=true; canvas.drawText("انجام‌شده (${done.size})",50f,y,paint); y+=20f; paint.isFakeBoldText=false
        done.forEach { t -> if(y<800f){ canvas.drawText("✓ ${t.title}",60f,y,paint); y+=18f } }
        doc.finishPage(page)
        doc.writeTo(outputStream)
        doc.close()
    }

    fun exportText(context: Context, tasks: List<Task>): Uri? =
        write(context, "karyar_tasks.txt", buildTextBytes(tasks))

    fun exportCsv(context: Context, tasks: List<Task>): Uri? =
        write(context, "karyar_tasks.csv", buildCsvBytes(tasks))

    fun exportPdf(context: Context, tasks: List<Task>): Uri? = try {
        val file=File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),"karyar_tasks.pdf")
        writePdfToStream(tasks, FileOutputStream(file))
        FileProvider.getUriForFile(context,"${context.packageName}.fileprovider",file)
    } catch(e:Exception){ null }

    private fun write(context: Context, name: String, data: ByteArray): Uri? = try {
        val file=File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),name)
        file.writeBytes(data)
        FileProvider.getUriForFile(context,"${context.packageName}.fileprovider",file)
    } catch(e:Exception){ null }

    fun openFile(context: Context, uri: Uri, mime: String) {
        val i=Intent(Intent.ACTION_VIEW).apply { setDataAndType(uri,mime); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        context.startActivity(Intent.createChooser(i,"باز کردن با..."))
    }

    fun importCsv(context: Context, uri: Uri): List<com.karland.app.data.Task> {
        return try {
            val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: return emptyList()
            val lines = content.lines().drop(1) // skip header
            lines.mapNotNull { line ->
                if (line.isBlank()) return@mapNotNull null
                // Format: "title","desc","cat","pri","status","due","created"
                val raw = line.trim()
                val fields = raw.split("\",\"")
                    .map { it.removePrefix("\"").removeSuffix("\"") }
                if (fields.isEmpty() || fields[0].isBlank()) return@mapNotNull null
                val title = fields[0]
                val description = fields.getOrElse(1) { "" }
                val catLabel = fields.getOrElse(2) { "" }
                val priLabel = fields.getOrElse(3) { "" }
                val status = fields.getOrElse(4) { "" }
                val dueDateStr = fields.getOrElse(5) { "" }
                val category = com.karland.app.data.TaskCategory.values()
                    .firstOrNull { it.label == catLabel }?.name
                    ?: com.karland.app.data.TaskCategory.OTHER.name
                val priority = com.karland.app.data.TaskPriority.values()
                    .firstOrNull { it.label == priLabel }?.name
                    ?: com.karland.app.data.TaskPriority.MEDIUM.name
                val isCompleted = status == "انجام شده"
                val dueDate = parseJalaliDate(dueDateStr)
                com.karland.app.data.Task(
                    title = title, description = description,
                    category = category, priority = priority,
                    isCompleted = isCompleted, dueDate = dueDate
                )
            }
        } catch (e: Exception) { emptyList() }
    }

    private fun parseJalaliDate(s: String): Long? {
        val parts = s.split("/")
        if (parts.size != 3) return null
        val y = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        val d = parts[2].toIntOrNull() ?: return null
        return JalaliCalendar.toTimestamp(y, m, d)
    }
}
