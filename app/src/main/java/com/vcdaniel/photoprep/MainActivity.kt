package com.vcdaniel.photoprep

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationView
import com.vcdaniel.photoprep.databinding.ActivityMainBinding

/** The primary activity of the app. This holds the fragments that are used as the primary means
 * of navigation for the app. The fragments are swapped using the Navigation library. */
class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_photo_shoot_locations, R.id.nav_common_prep_library, R.id.nav_preferences
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        // Swap which fragment is shown using the navigation library
        // https://developer.android.com/guide/navigation/navigation-ui
        navController.addOnDestinationChangedListener { _, _, arguments ->

            // In some fragments such as the edit photo shoot location fragment show a close icon
            // instead of the back icon to indicate the relationship between fragments
            if (arguments?.getBoolean(getString(R.string.show_close_button_key), false) == true) {
                binding.appBarMain.toolbar.navigationIcon =
                    getDrawable(R.drawable.ic_baseline_close_24)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}