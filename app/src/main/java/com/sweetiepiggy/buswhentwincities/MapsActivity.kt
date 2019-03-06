package com.sweetiepiggy.buswhentwincities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MapsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        if (savedInstanceState == null) {
            val fragment = MyMapFragment.newInstance()
            fragment.setArguments(intent.extras)
            supportFragmentManager.beginTransaction()
            	    .add(R.id.container, fragment)
                    .commitNow()
        }
    }
}

