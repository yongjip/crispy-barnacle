package com.example.warehousewrangler

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Skeleton Activity for AR functionality using NRSDK.
 *
 * Note: To fully implement this, you need to import the NRSDK library (aar)
 * and configure Gradle dependencies.
 *
 * Instructions:
 * 1. Download NRSDK from XREAL Developer site.
 * 2. Add implementation files('libs/nrsdk.aar') to build.gradle.
 * 3. Implement NRSDK session management here.
 */
class ArActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ar)

        // Placeholder for NRSDK initialization
        // val session = NRSession(this)
        // session.start()

        val tvStatus = findViewById<TextView>(R.id.tv_ar_status)
        tvStatus.text = "AR Mode Initialized (NRSDK Required)"

        // In a real implementation, you would:
        // 1. Setup NRPlaneDetector to find the floor/racks.
        // 2. Use NRAnchor to place 3D arrows pointing to the location.
        // 3. Render a 3D overlay using OpenGL or Unity (if using Unity SDK).
    }

    override fun onDestroy() {
        super.onDestroy()
        // session.stop()
    }
}
