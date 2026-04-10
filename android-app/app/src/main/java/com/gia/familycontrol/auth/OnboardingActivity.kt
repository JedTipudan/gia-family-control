package com.gia.familycontrol.auth

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.gia.familycontrol.R
import com.google.android.material.button.MaterialButton

data class OnboardingSlide(val emoji: String, val title: String, val desc: String)

class OnboardingActivity : AppCompatActivity() {

    private val slides = listOf(
        OnboardingSlide("🛡️", "Welcome to Gia Family Control",
            "This app helps your parent keep you safe online.\n\nIt monitors your device with your knowledge and consent."),
        OnboardingSlide("📍", "Location Tracking",
            "Your parent can see your real-time location on a map.\n\nThis helps them know you are safe at all times."),
        OnboardingSlide("📱", "App Monitoring",
            "Your parent can block or hide apps on this device.\n\nThis is to help you focus and stay safe online."),
        OnboardingSlide("🔒", "Remote Lock",
            "Your parent can lock this device remotely.\n\nThis is used to manage screen time and keep you safe."),
        OnboardingSlide("🔕", "Notification Control",
            "Your parent may disable the notification panel.\n\nThis prevents changes to device settings without permission."),
        OnboardingSlide("🆘", "Emergency SOS",
            "You can send an SOS alert to your parent anytime.\n\nTap the SOS button in the app if you need help."),
        OnboardingSlide("✅", "You're All Set!",
            "This app runs with your full knowledge.\n\nIt is NOT spyware — your parent is here to keep you safe.\n\nTap Get Started to continue."),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        val viewPager  = findViewById<ViewPager2>(R.id.viewPager)
        val dotsLayout = findViewById<ViewGroup>(R.id.dotsLayout)
        val btnNext    = findViewById<MaterialButton>(R.id.btnNext)
        val btnSkip    = findViewById<TextView>(R.id.btnSkip)

        viewPager.adapter = SlideAdapter(slides)

        // Build dots
        val dots = Array(slides.size) { ImageView(this).apply {
            setImageResource(android.R.drawable.presence_invisible)
            setPadding(8, 0, 8, 0)
        }}
        dots.forEach { dotsLayout.addView(it) }
        dots[0].setImageResource(android.R.drawable.presence_online)

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                dots.forEachIndexed { i, dot ->
                    dot.setImageResource(
                        if (i == position) android.R.drawable.presence_online
                        else android.R.drawable.presence_invisible
                    )
                }
                btnNext.text = if (position == slides.lastIndex) "Get Started ✓" else "Next →"
            }
        })

        btnNext.setOnClickListener {
            val current = viewPager.currentItem
            if (current < slides.lastIndex) {
                viewPager.currentItem = current + 1
            } else {
                finishOnboarding()
            }
        }

        btnSkip.setOnClickListener { finishOnboarding() }
    }

    private fun finishOnboarding() {
        getSharedPreferences("gia_prefs", MODE_PRIVATE)
            .edit().putBoolean("onboarding_done", true).apply()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}

class SlideAdapter(private val slides: List<OnboardingSlide>) :
    RecyclerView.Adapter<SlideAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val emoji: TextView = view.findViewById(R.id.tvEmoji)
        val title: TextView = view.findViewById(R.id.tvTitle)
        val desc:  TextView = view.findViewById(R.id.tvDesc)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context)
            .inflate(R.layout.item_onboarding_slide, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.emoji.text = slides[position].emoji
        holder.title.text = slides[position].title
        holder.desc.text  = slides[position].desc
    }

    override fun getItemCount() = slides.size
}
