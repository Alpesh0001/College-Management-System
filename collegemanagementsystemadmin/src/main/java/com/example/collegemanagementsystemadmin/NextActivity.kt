package com.example.collegemanagementsystemadmin

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.ActivityOptionsCompat
import androidx.core.util.Pair
import com.example.collegemanagementsystemadmin.utils.CoreBaseActivity

class NextActivity : CoreBaseActivity() {

    private lateinit var btnGetStarted: ImageButton
    private lateinit var imgLogo: ImageView
    private lateinit var titleView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_next)

        initViews()
        setupClickListener()
    }

    private fun initViews() {
        btnGetStarted = findViewById(R.id.btnGetStarted)
        imgLogo = findViewById(R.id.imgLogo)
        titleView = findViewById(R.id.txtAppName)
    }

    private fun setupClickListener() {
        btnGetStarted.setOnClickListener { goToLogin() }
    }

    private fun goToLogin() {
        val intent = Intent(this, LoginActivity::class.java)

        val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
            this,
            Pair(imgLogo as View, "logo_shared"),
            Pair(titleView as View, "title_shared")
        )

        startActivity(intent, options.toBundle())
    }
}
