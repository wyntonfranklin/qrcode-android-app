package com.wfranklin.qrcodescanner

import android.content.ClipData
import android.content.ClipboardManager
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.wfranklin.qrcodescanner.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        editor = sharedPreferences.edit()

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)
        supportActionBar?.subtitle = "Press center to start scanning"
        binding.fab.setOnClickListener { view ->
            showAbout()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_server -> {
                removeServerAddress()
                return true
            }
            R.id.action_history -> {
                showLastScanned()
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun removeServerAddress(){
        editor.remove("ip")
        editor.remove("host")
        editor.commit()
        Toast.makeText(this, "Disconnected from Server",
            Toast.LENGTH_LONG).show()
    }

    private fun showLastScanned(){
        var scannedMessage = sharedPreferences.getString("scanned", "")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Last Scanned Message")
        builder.setMessage(scannedMessage)
        builder.setPositiveButton("Close") { dialog, which ->

        }
        builder.setNeutralButton("Copy") { dialog, which ->
            (this.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).apply {
                setPrimaryClip(ClipData.newPlainText("QRCode Shared", scannedMessage))
            }
            Toast.makeText(applicationContext,
                "Text Copied to clipboard", Toast.LENGTH_SHORT).show()
        }
        builder.show()
    }

    private fun showAbout(){
        val builder = AlertDialog.Builder(this)
        builder.setTitle("About QRCode")
        builder.setMessage("A simple QRCode and BarCode reader developed by wfranklin. Enjoy!")
        builder.setNeutralButton("Ok") { dialog, which ->

        }
        builder.show()
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }
}