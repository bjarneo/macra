package app.macra

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import app.macra.reminders.Reminders
import app.macra.ui.MacraApp

class MainActivity : ComponentActivity() {
    private val vm: MacraViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Reminders.channel(this)
        if (savedInstanceState == null) handle(intent)
        setContent { MacraApp(vm) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    override fun onResume() {
        super.onResume()
        vm.onResume()
    }

    override fun onStop() {
        super.onStop()
        vm.flush()
    }

    private fun handle(intent: Intent?) {
        val action = intent?.getStringExtra(Reminders.EXTRA_ACTION) ?: return
        intent.removeExtra(Reminders.EXTRA_ACTION)
        vm.handleAction(action)
    }
}
