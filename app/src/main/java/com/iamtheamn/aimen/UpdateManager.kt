package com.iamtheamn.aimen

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import java.util.Locale

data class GithubRelease(
    val tag_name: String,
    val body: String?,
    val html_url: String
)

@Composable
fun UpdateCheckerDialog(
    currentAppVersion: String = "1.0",
    context: Context,
    containerColor: Color,
    textColor: Color,
    accentColor: Color,
    appLanguage: String
) {
    var updateInfo by remember { mutableStateOf<GithubRelease?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    val githubApiUrl = "https://api.github.com/repos/iamtheamn/FolliA/releases/latest"

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val jsonResponse = URL(githubApiUrl).readText()
                val fetchedRelease = Gson().fromJson(jsonResponse, GithubRelease::class.java)
                val cleanLatest = fetchedRelease.tag_name.lowercase().replace("v", "").trim()
                val cleanCurrent = currentAppVersion.lowercase().replace("v", "").trim()

                if (cleanLatest != cleanCurrent) {
                    updateInfo = fetchedRelease
                    showDialog = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    if (showDialog && updateInfo != null) {

        val isFrench = if (appLanguage == "system") Locale.getDefault().language == "fr" else appLanguage == "fr"
        val releaseNotes = updateInfo!!.body ?: ""

        AlertDialog(
            onDismissRequest = { showDialog = false },
            containerColor = containerColor,
            title = {
                Text(
                    text = stringResource(R.string.update_available_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = textColor
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 250.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = stringResource(R.string.update_version_available, updateInfo!!.tag_name),
                        fontWeight = FontWeight.SemiBold,
                        color = accentColor,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Text(
                        text = stringResource(R.string.update_whats_new),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = textColor,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = releaseNotes,
                        fontSize = 14.sp,
                        color = textColor.copy(alpha = 0.8f)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateInfo!!.html_url))
                        context.startActivity(intent)
                        showDialog = false
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = Color.White
                    )
                ) {
                    Text(stringResource(R.string.update_download))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.update_later), color = textColor.copy(alpha = 0.6f))
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}