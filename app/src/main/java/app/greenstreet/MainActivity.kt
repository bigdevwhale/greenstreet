package app.greenstreet

import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import app.greenstreet.ui.Root
import app.greenstreet.ui.theme.GreenTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val t = android.graphics.Color.TRANSPARENT
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(t), navigationBarStyle = SystemBarStyle.dark(t))
        setContent { GreenTheme { Root() } }
    }

    override fun onPause() {
        container.visible = false
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        container.visible = true
        container.wake()
        container.refreshWeather()
    }
}
