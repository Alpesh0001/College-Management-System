package com.example.collegemanagementsystemfaculty

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var navController : NavController
    private lateinit var bottomNav     : BottomNavigationView
    private lateinit var tvToolbarTitle: TextView
    private lateinit var btnNotification: ImageView
    private lateinit var btnProfile    : ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bindViews()
        setupNavigation()
        setupToolbar()
        setupBackPress()
    }

    private fun bindViews() {
        tvToolbarTitle  = findViewById(R.id.tvToolbarTitle)
        bottomNav       = findViewById(R.id.bottomNav)
        btnNotification = findViewById(R.id.btnNotification)
        btnProfile      = findViewById(R.id.btnProfile)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
    }

    private fun setupNavigation() {

        bottomNav.setOnItemSelectedListener { item ->

            val options = androidx.navigation.navOptions {

                // ⭐ Avoid multiple copies
                launchSingleTop = true

                // ⭐ Restore previous state
                restoreState = true

                // ⭐ Save state of previous fragment
                popUpTo(navController.graph.startDestinationId) {
                    saveState = true
                }
            }

            when (item.itemId) {

                R.id.homeFragment -> {
                    navController.navigate(R.id.homeFragment, null, options)
                    true
                }

                R.id.attendanceFragment -> {
                    navController.navigate(R.id.attendanceFragment, null, options)
                    true
                }

                R.id.timetableFragment -> {
                    navController.navigate(R.id.timetableFragment, null, options)
                    true
                }

                R.id.profileFragment -> {
                    navController.navigate(R.id.profileFragment, null, options)
                    true
                }

                else -> false
            }
        }

        // Toolbar title update (keep your code)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            tvToolbarTitle.text = when (destination.id) {
                R.id.homeFragment       -> "Faculty Dashboard"
                R.id.attendanceFragment -> "Attendance"
                R.id.timetableFragment  -> "My Timetable"
                R.id.profileFragment    -> "Profile"
                else                    -> "Faculty App"
            }
        }
    }

    private fun setupToolbar() {
        btnNotification.setOnClickListener {
            // Handle notifications
        }
        btnProfile.setOnClickListener {
            // Use BottomNav to keep selection in sync
            bottomNav.selectedItemId = R.id.profileFragment
        }
    }

    private fun setupBackPress() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (navController.currentDestination?.id != R.id.homeFragment) {
                        // Consistently return to Home when on any other top-level fragment
                        bottomNav.selectedItemId = R.id.homeFragment
                    } else {
                        // If already on Home, exit the app
                        finish()
                    }
                }
            }
        )
    }

    fun updateToolbarTitle(title: String) {
        tvToolbarTitle.text = title
    }
}
