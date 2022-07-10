package com.wfranklin.qrcodescanner

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.wfranklin.qrcodescanner.databinding.FragmentFirstBinding
import android.content.Context.WIFI_SERVICE
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.text.format.Formatter

import android.net.wifi.WifiManager
import android.preference.PreferenceManager
import android.util.Log
import android.util.Patterns
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.budiyev.android.codescanner.AutoFocusMode
import com.budiyev.android.codescanner.CodeScanner
import com.budiyev.android.codescanner.CodeScannerView
import com.budiyev.android.codescanner.DecodeCallback
import com.budiyev.android.codescanner.ErrorCallback
import com.budiyev.android.codescanner.ScanMode
import java.util.*
import androidx.core.app.ActivityCompat
import java.net.InetAddress
import java.net.Socket


enum class Notification {
    SUCCESS, ERROR, INFO
}

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null
    private lateinit var codeScanner: CodeScanner
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private var myDisplayTextView: TextView? = null
    private val MY_CAMERA_REQUEST_CODE = 100
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private var notificationView: TextView? = null
    private var isCameraLive : Boolean = false


    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        editor = sharedPreferences.edit()
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        val scannerView = binding.root.findViewById<CodeScannerView>(R.id.scanner_view)
        notificationView = binding.root.findViewById(R.id.notification_view) as TextView
        notificationView!!.setText("Welcome to QR Code Scanner")
        codeScanner = CodeScanner(requireContext().applicationContext, scannerView)

        // Parameters (default values)
        codeScanner.camera = CodeScanner.CAMERA_BACK // or CAMERA_FRONT or specific camera id
        codeScanner.formats = CodeScanner.ALL_FORMATS // list of type BarcodeFormat,
        // ex. listOf(BarcodeFormat.QR_CODE)
        codeScanner.autoFocusMode = AutoFocusMode.SAFE // or CONTINUOUS
        codeScanner.scanMode = ScanMode.SINGLE // or CONTINUOUS or PREVIEW
        codeScanner.isAutoFocusEnabled = true // Whether to enable auto focus or not
        codeScanner.isFlashEnabled = false // Whether to enable flash or not

        // Callbacks
        codeScanner.decodeCallback = DecodeCallback {
            activity?.runOnUiThread {
                scanCallback(it.text)
            }
        }

        codeScanner.errorCallback = ErrorCallback { // or ErrorCallback.SUPPRESS
            activity?.runOnUiThread {
                Toast.makeText(requireContext().applicationContext, "Camera initialization error: ${it.message}",
                    Toast.LENGTH_LONG).show()
            }
        }


        scannerView.setOnClickListener {
            if(isCameraLive){
                codeScanner.stopPreview()
                isCameraLive = false
            }else{
                checkForPermissions()
            }

        }

        return binding.root

    }

    private fun setNotificationMessage(message: String, style: Notification){
        activity?.runOnUiThread {
            notificationView!!.setText(message)
            if(style == Notification.SUCCESS){
                notificationView!!.setBackgroundColor(Color.parseColor("#34bf49"))
            }else if(style == Notification.ERROR){
                notificationView!!.setBackgroundColor(Color.parseColor("#ff4c4c"))
            }else if(style == Notification.INFO){
                notificationView!!.setBackgroundColor(Color.parseColor("#00a4e4"))
            }
            notificationView!!.setTextColor(Color.parseColor("#ffffff"))
            notificationView!!.postDelayed({
                notificationView!!.text = "Welcome to QR Code"
                notificationView!!.setTextColor(Color.parseColor("#000000"))
                notificationView!!.setBackgroundColor(Color.parseColor("#ffffff")) }, 3000)
        }
    }

    private fun scanCallback(message : String){
        isCameraLive = false
        saveScannedMessage(message)
        // set to clipboard
        (requireContext().getSystemService(AppCompatActivity.CLIPBOARD_SERVICE) as ClipboardManager).apply {
            setPrimaryClip(ClipData.newPlainText("QRCode Shared", message))
        }
        if(message.contains("server-url")){
            val parts = message.split(":")
            val ipaddress = parts[1]
            val port = parts[2]
            updateServerIpAddress(ipaddress, port)
        }else{
            Toast.makeText(requireContext().applicationContext, message, Toast.LENGTH_LONG).show()
            sendToServer(message + "\r\n")
        }
    }

    private fun updateServerIpAddress(ip: String, port: String){
        if(Patterns.IP_ADDRESS.matcher(ip).matches()){
            saveServerAddress(ip, port)
            setNotificationMessage("Connected to server at $ip", Notification.SUCCESS)
            Toast.makeText(requireContext().applicationContext, "Connected to Server",
                Toast.LENGTH_LONG).show()
        }
    }

    private fun saveServerAddress(ip:String, port: String){
        editor.putString("ip", ip)
        editor.putString("port", port)
        editor.commit()
    }

    private fun saveScannedMessage(msg: String){
        editor.putString("scanned", msg)
        editor.commit()
    }

    private fun sendToServer(message: String) {
        var ipAddress = sharedPreferences.getString("ip", "0")
        val port = sharedPreferences.getString("port", "0")
        if(ipAddress!!.equals("0") || port!!.equals("0")){
            /*
            Toast.makeText(requireContext().applicationContext, "Ip Address not set",
                Toast.LENGTH_LONG).show()*/
        }else{
            Thread {
                try{
                    val client = Socket(InetAddress.getByName(ipAddress), port.toInt())
                    client.outputStream.write(message.toByteArray())
                    setNotificationMessage("Message Successfully Sent", Notification.SUCCESS)
                    client.close()
                }catch(e: Exception){
                    //code that handles exception
                    activity?.runOnUiThread {
                        setNotificationMessage("Message not sent", Notification.ERROR)
                        Toast.makeText(requireContext().applicationContext, e.message,
                            Toast.LENGTH_LONG).show()
                    }
                }
            }.start()
        }
    }

    private fun startCamera(){
        isCameraLive = true
        codeScanner.startPreview()
    }

    private fun checkForPermissions(){
        when {
            ContextCompat.checkSelfPermission(
                requireContext().applicationContext,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                // You can use the API that requires the permission.
                startCamera()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
            // In an educational UI, explain to the user why your app requires this
            // permission for a specific feature to behave as expected. In this UI,
            // include a "cancel" or "no thanks" button that allows the user to
            // continue using your app without granting the permission.
                ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.CAMERA), MY_CAMERA_REQUEST_CODE)

        }else -> {
                // You can directly ask for the permission.
                // The registered ActivityResultCallback gets the result of this request.
              //  requestPermissionLauncher.launch(
              //      Manifest.permission.CAMERA)
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.CAMERA), MY_CAMERA_REQUEST_CODE)

        }
        }
    }

    override fun onResume() {
        super.onResume()
        codeScanner.startPreview()
    }

    override fun onPause() {
        codeScanner.releaseResources()
        super.onPause()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == MY_CAMERA_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                codeScanner.startPreview()
            } else {

            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val wifiManager: WifiManager = requireContext().applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        val ip: String = Formatter.formatIpAddress(wifiManager.connectionInfo.ipAddress)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}