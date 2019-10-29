package mrblrrd.fcmsend

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import com.google.auth.oauth2.AccessToken
import com.google.auth.oauth2.GoogleCredentials
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

fun main(args: Array<String>) = FcmSend().main(args)

private class FcmSend : CliktCommand(
    help = "Send push notifications through Firebase.",
    name = "fcm-send",
    printHelpOnEmptyArgs = true
) {

    private val appCredentialsFile: File
            by option(
                "-c",
                "--app-credentials",
                help = "App credentials file path. GOOGLE_APPLICATION_CREDENTIALS environment variable can be used instead. See https://firebase.google.com/docs/cloud-messaging/auth-server for more details.",
                envvar = "GOOGLE_APPLICATION_CREDENTIALS"
            ).file(
                exists = true,
                fileOkay = true,
                folderOkay = false,
                writable = false,
                readable = true
            ).required()

    private val messageFileList: List<File>
            by option(
                "-m",
                "--message-file",
                help = "Path to the file containing message JSON. File must be UTF-8 encoded. Message format specification: https://firebase.google.com/docs/reference/fcm/rest/v1/projects.messages. Multiple occurrences can be used."
            ).file(
                exists = true,
                fileOkay = true,
                folderOkay = false,
                writable = false,
                readable = true
            ).multiple()

    private val projectId: String
        get() = JSONObject(appCredentialsFile.readText(Charsets.UTF_8)).getString("project_id")

    override fun run() {
        messageFileList.forEach { messageFile ->
            sendMessage(messageFile.readText(Charsets.UTF_8))
        }
    }

    private fun sendMessage(message: String) {

        val connection: HttpURLConnection =
            getConnection(
                apiUrl = "https://fcm.googleapis.com/v1/projects/$projectId/messages:send",
                token = getAccessToken(appCredentialsFile).tokenValue
            ).apply { doOutput = true }

        DataOutputStream(connection.outputStream).use { outputStream ->
            outputStream.writeBytes(message)
            outputStream.flush()
        }

        when (connection.responseCode) {
            200 -> {
                println("Message sent to Firebase for delivery. Response:")
                println(connection.inputStream.bufferedReader().use(BufferedReader::readText))
            }
            else -> {
                println("Unable to send message to Firebase:")
                println(connection.errorStream.bufferedReader().use(BufferedReader::readText))
            }
        }
    }

    private fun getConnection(apiUrl: String, token: String): HttpURLConnection =
        (URL(apiUrl).openConnection() as HttpURLConnection)
            .apply {
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "application/json; UTF-8")
            }

    private fun getAccessToken(appCredentialsFile: File): AccessToken =
        GoogleCredentials
            .fromStream(appCredentialsFile.inputStream())
            .createScoped("https://www.googleapis.com/auth/firebase.messaging")
            .apply { refreshIfExpired() }
            .accessToken
}