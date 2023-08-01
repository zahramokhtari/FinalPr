package org.zahramokhtari.myaction

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import org.apache.http.HttpEntity
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.json.JSONObject
import java.awt.Dialog
import java.io.File
import java.io.IOException
import java.net.URI
import java.nio.file.Files
import java.nio.file.Paths
import javax.swing.*
import javax.swing.table.DefaultTableModel

class SuggestLibraryAction : AnAction() {

    override fun actionPerformed(event: AnActionEvent) {
        try {
            // Find the user's pom.xml file
            val userPomPath = findUserPom()

            // Read the user's pom.xml file
            val userPom = String(Files.readAllBytes(Paths.get(userPomPath)))

            // Send the user's pom.xml file to the recommendation API
            val recommendedLibraries = sendRecommendationRequest(userPom)

            // Display the recommendations to the user in a table
            val tableModel = DefaultTableModel(arrayOf("Library Name"), 0)
            recommendedLibraries.take(10).forEach { libraryName ->
                tableModel.addRow(arrayOf(libraryName))
            }
            val table = JTable(tableModel)
            table.autoResizeMode = JTable.AUTO_RESIZE_ALL_COLUMNS
            table.tableHeader.reorderingAllowed = false
            val scrollPane = JScrollPane(table)

            // Show the table in a custom dialog box
            val dialog = JDialog()
            dialog.title = "Recommended Libraries"
            dialog.modalityType = Dialog.ModalityType.APPLICATION_MODAL
            dialog.contentPane.add(scrollPane)
            dialog.pack()
            dialog.setLocationRelativeTo(null)
            dialog.isVisible = true
        } catch (ex: Exception) {
            // Show an error message to the user in a dialog box
            JOptionPane.showMessageDialog(
                null,
                "Error: ${ex.message}",
                "Error",
                JOptionPane.ERROR_MESSAGE
            )

            // Show a warning message to the user in a dialog box
            JOptionPane.showMessageDialog(
                null,
                "Warning: Some recommended libraries may not be compatible with your project.",
                "Warning",
                JOptionPane.WARNING_MESSAGE
            )
        }
    }

    @Throws(IOException::class)
    private fun findUserPom(): String {
    // Get the user's desktop directory
        val userDesktopDir = File(System.getProperty("user.home"), "Desktop")

        // Show desktop directory initially in the file chooser
        val fileChooser = JFileChooser(userDesktopDir)
        fileChooser.fileSelectionMode = JFileChooser.FILES_ONLY
        fileChooser.dialogTitle = "Select pom.xml file"

        // Display the file chooser dialog
        val result = fileChooser.showOpenDialog(null)
        if (result == JFileChooser.APPROVE_OPTION) {
            val selectedFile = fileChooser.selectedFile
            if (selectedFile.exists() && selectedFile.isFile && selectedFile.name == "pom.xml") {
                return selectedFile.absolutePath
            } else {
                throw IOException("Invalid pom.xml file selected")
            }
        } else {
            throw IOException("No pom.xml file selected")
        }
    }

    @Throws(IOException::class)
    private fun sendRecommendationRequest(userPom: String): List<String> {
        // Create an HttpClient instance
        val httpClient: CloseableHttpClient = HttpClients.createDefault()

        // Create an HttpPost request with the user's pom.xml file as the request body
        val httpPost = HttpPost(URI("http://localhost:5000/recommend").toString())
        val requestEntity = StringEntity(
            JSONObject().put("pom", userPom).toString(),
            ContentType.APPLICATION_JSON
        )
        httpPost.entity = requestEntity

        // Send the request and receive the response
        val response: CloseableHttpResponse = httpClient.execute(httpPost)
        val statusCode = response.statusLine.statusCode
        if (statusCode != 200) {
            throw IOException("HTTP error $statusCode")
        }
        val responseEntity: HttpEntity = response.entity
        val responseString = responseEntity.content.readBytes().toString(Charsets.UTF_8)
        // Close the resources
        response.close()
        httpClient.close()

        // Parse the response and extract the recommended libraries
        val responseJson = JSONObject(responseString)
        val recommendationsArray = responseJson.getJSONArray("recommendations")
        val recommendedLibraries = mutableListOf<String>()
        for (i in 0 until recommendationsArray.length()) {
            recommendedLibraries.add(recommendationsArray.getString(i))
        }

        return recommendedLibraries
    }
}