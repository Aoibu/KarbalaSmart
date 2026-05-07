package iq.gov.smartkarbala

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.NfcA
import android.nfc.tech.NfcB
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import iq.gov.smartkarbala.databinding.ActivityMainBinding
import iq.gov.smartkarbala.nfc.CreditCardReader
import iq.gov.smartkarbala.nfc.IraqiIdReader
import iq.gov.smartkarbala.util.PrefsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var prefs: PrefsManager

    // NFC
    private var nfcAdapter: NfcAdapter? = null
    private var pendingIntent: PendingIntent? = null
    private var nfcMode: NfcMode = NfcMode.NONE

    // Callbacks for NFC events
    var onIraqiIdRead: ((IraqiIdReader.ReadResult) -> Unit)? = null
    var onCreditCardRead: ((CreditCardReader.ReadResult) -> Unit)? = null

    enum class NfcMode { NONE, IRAQI_ID, CREDIT_CARD }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        prefs = PrefsManager(this)

        // Edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
        setupNfc()

        // Handle initial destination based on login state
        splashScreen.setKeepOnScreenCondition { false }

        if (!prefs.isLoggedIn) {
            navController.navigate(R.id.loginFragment)
        }
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        binding.bottomNavigation.setupWithNavController(navController)

        // Hide/show bottom nav based on destination
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.loginFragment -> {
                    binding.bottomNavigation.visibility = View.GONE
                    binding.navContainer.visibility = View.GONE
                }
                else -> {
                    binding.bottomNavigation.visibility = View.VISIBLE
                    binding.navContainer.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun setupNfc() {
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        if (nfcAdapter == null) {
            // Device doesn't support NFC
            return
        }

        pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE
        )
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter?.enableForegroundDispatch(
            this, pendingIntent,
            arrayOf(
                IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED),
                IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED),
                IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED)
            ),
            arrayOf(
                arrayOf(IsoDep::class.java.name),
                arrayOf(NfcA::class.java.name),
                arrayOf(NfcB::class.java.name)
            )
        )
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNfcIntent(intent)
    }

    private fun handleNfcIntent(intent: Intent) {
        val action = intent.action
        if (action == NfcAdapter.ACTION_TECH_DISCOVERED ||
            action == NfcAdapter.ACTION_TAG_DISCOVERED ||
            action == NfcAdapter.ACTION_NDEF_DISCOVERED) {

            val tag = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            }

            tag?.let { processNfcTag(it) }
        }
    }

    private fun processNfcTag(tag: Tag) {
        lifecycleScope.launch {
            when (nfcMode) {
                NfcMode.IRAQI_ID -> {
                    val result = withContext(Dispatchers.IO) {
                        IraqiIdReader.readCard(tag)
                    }
                    vibrateNfcFeedback(result is IraqiIdReader.ReadResult.Success)
                    onIraqiIdRead?.invoke(result)
                }
                NfcMode.CREDIT_CARD -> {
                    val result = withContext(Dispatchers.IO) {
                        CreditCardReader.readCard(tag)
                    }
                    vibrateNfcFeedback(result is CreditCardReader.ReadResult.Success)
                    onCreditCardRead?.invoke(result)
                }
                NfcMode.NONE -> {
                    // Auto-detect: try Iraqi ID first, then credit card
                    val idResult = withContext(Dispatchers.IO) {
                        IraqiIdReader.readCard(tag)
                    }
                    if (idResult is IraqiIdReader.ReadResult.Success) {
                        vibrateNfcFeedback(true)
                        onIraqiIdRead?.invoke(idResult)
                    } else {
                        val cardResult = withContext(Dispatchers.IO) {
                            CreditCardReader.readCard(tag)
                        }
                        vibrateNfcFeedback(cardResult is CreditCardReader.ReadResult.Success)
                        onCreditCardRead?.invoke(cardResult)
                    }
                }
            }
        }
    }

    private fun vibrateNfcFeedback(success: Boolean) {
        val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val vm = getSystemService(android.os.VibratorManager::class.java)
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(android.os.Vibrator::class.java)
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            if (success) {
                vibrator.vibrate(android.os.VibrationEffect.createWaveform(
                    longArrayOf(0, 80, 50, 80), -1
                ))
            } else {
                vibrator.vibrate(android.os.VibrationEffect.createOneShot(300,
                    android.os.VibrationEffect.DEFAULT_AMPLITUDE))
            }
        }
    }

    fun setNfcMode(mode: NfcMode) {
        nfcMode = mode
    }

    fun isNfcAvailable(): Boolean = nfcAdapter != null
    fun isNfcEnabled(): Boolean = nfcAdapter?.isEnabled == true
}
