package com.cookiejarapps.smartcookieweb_ytdl

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.URLUtil
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.iterator
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavOptions
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.cookiejarapps.smartcookieweb_ytdl.models.VideoInfoViewModel
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), NavActivity {

    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        val navController = findNavController(R.id.nav_host_fragment)

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.home_fragment,
                R.id.downloads_fragment
            )
        )
        toolbar.setupWithNavController(navController, appBarConfiguration)
        supportActionBar?.title = navController.currentDestination?.label
        bottom_view?.setupWithNavController(navController)

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent!!)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_topbar, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        val navController = Navigation.findNavController(
            this,
            R.id.nav_host_fragment
        )
        val navigated = NavigationUI.onNavDestinationSelected(item!!, navController)
        return navigated || super.onOptionsItemSelected(item)
    }

    private fun handleIntent(intent: Intent) {
        if (Intent.ACTION_SEND == intent.action) {
            val navController = Navigation.findNavController(
                this,
                R.id.nav_host_fragment
            )
            val navOptions = NavOptions.Builder().setLaunchSingleTop(true).build()
            navController.navigate(R.id.home_fragment, null, navOptions)

            intent.getStringExtra(Intent.EXTRA_TEXT)?.let {
                if (URLUtil.isValidUrl(it)) {
                    val videoModel =
                        ViewModelProvider(this).get(VideoInfoViewModel::class.java)
                    videoModel.fetchInfo(it)
                }
                else{
                    Toast.makeText(applicationContext, R.string.invalid_url, Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    override fun hideNav() {
        bottom_view?.visibility = View.GONE
    }

    override fun showNav() {
        bottom_view?.visibility = View.VISIBLE
    }

    override fun showOptions() {
        toolbar.menu.iterator().forEach { it.isVisible = true }
    }

    override fun hideOptions() {
        toolbar.menu.iterator().forEach { it.isVisible = false }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}

interface NavActivity {
    fun hideNav()
    fun showNav()
    fun showOptions()
    fun hideOptions()
}
