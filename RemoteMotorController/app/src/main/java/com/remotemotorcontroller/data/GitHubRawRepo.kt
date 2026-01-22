package com.remotemotorcontroller.data

import com.google.gson.Gson
import okhttp3.*

// RESULT FROM THE VERSION JSON OF THE HARDWARE
data class UpdateManifest(
    val version: Int,
    val downloadUrl: String,
    val notes: String
)
object GitHubRawRepo {
    private val client = OkHttpClient()

    // URL TO THE RAW FIRMWARE VERSION FILE ON GITHUB
    private const val RAW_URL = "https://raw.githubusercontent.com/zebauman/rpm1/refs/heads/main/firmware/version.json"

    // CHECK THE VERSION FILE IF THERE'S A NEW VERSION
    fun checkForUpdate(
        currentVersion: Int,
        onUpdateFound: (UpdateManifest) -> Unit,
        onError: (String) -> Unit
    ){
        val request = Request.Builder().url(RAW_URL).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: okio.IOException) {
                onError("Network Error: ${e.message}")
                return
            }

            override fun onResponse(call: Call, response: Response) {
                if(!response.isSuccessful){
                    onError("Check Failed: HTTP ${response.code} (Check URL")
                    return
                }
                // ATTEMPT TO CONVERT THE RAW TEXT ON GITHUB TO KOTLIN OBJECT
                try{
                    val jsonStr = response.body?.string()
                    val manifest = Gson().fromJson(jsonStr, UpdateManifest::class.java)

                    if(manifest.version > currentVersion){
                        onUpdateFound(manifest)
                    }else{
                        // UP-TO-DATE SO NO ACTION
                    }
                }catch(e: Exception){
                    onError("JSON Error: ${e.message}")
                }
            }

        })
    }

    // DOWNLOAD THE ACTUAL NEW FIRMWARE BIN FILE
    fun downloadFirmware(
        url: String,
        onSuccess: (ByteArray) -> Unit,
        onFailure: (String) -> Unit
    ){
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object: Callback{
            override fun onFailure(call: Call, e: okio.IOException) {
                onFailure("Download failed: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val bytes = response.body?.bytes()
                if(bytes?.isNotEmpty() == true){
                    onSuccess(bytes)
                }else{
                    onFailure("File was empty")
                }
            }

        })
    }
}