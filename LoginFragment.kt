package iq.gov.smartkarbala.ui.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import iq.gov.smartkarbala.MainActivity
import iq.gov.smartkarbala.R
import iq.gov.smartkarbala.SmartKarbalaApp
import iq.gov.smartkarbala.data.model.User
import iq.gov.smartkarbala.databinding.FragmentLoginBinding
import iq.gov.smartkarbala.nfc.IraqiIdReader
import iq.gov.smartkarbala.util.PrefsManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private lateinit var prefs: PrefsManager
    private val db by lazy { SmartKarbalaApp.instance.database }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = PrefsManager(requireContext())

        setupAnimations()
        setupNfc()
        setupButtons()
        checkNfcStatus()
    }

    private fun setupAnimations() {
        // Staggered entry animations
        binding.logoContainer.alpha = 0f
        binding.logoContainer.translationY = -60f

        binding.titleText.alpha = 0f
        binding.subtitleText.alpha = 0f

        binding.nfcCard.alpha = 0f
        binding.nfcCard.translationY = 80f

        binding.altLoginContainer.alpha = 0f

        // Animate in sequence
        viewLifecycleOwner.lifecycleScope.launch {
            binding.logoContainer.animate().alpha(1f).translationY(0f).setDuration(700).start()
            delay(200)
            binding.titleText.animate().alpha(1f).setDuration(500).start()
            delay(100)
            binding.subtitleText.animate().alpha(1f).setDuration(500).start()
            delay(200)
            binding.nfcCard.animate().alpha(1f).translationY(0f).setDuration(600).start()
            delay(300)
            binding.altLoginContainer.animate().alpha(1f).setDuration(500).start()
        }

        // Start pulsing NFC animation
        startNfcPulseAnimation()
    }

    private fun startNfcPulseAnimation() {
        val pulseAnim = AnimationUtils.loadAnimation(requireContext(), R.anim.nfc_pulse)
        binding.nfcPulseRing.startAnimation(pulseAnim)
        binding.nfcPulseRing2.startAnimation(
            AnimationUtils.loadAnimation(requireContext(), R.anim.nfc_pulse_delayed)
        )
    }

    private fun setupNfc() {
        val mainActivity = activity as? MainActivity ?: return

        // Set mode to Iraqi ID reading
        mainActivity.setNfcMode(MainActivity.NfcMode.IRAQI_ID)

        mainActivity.onIraqiIdRead = { result ->
            handleNfcResult(result)
        }
    }

    private fun checkNfcStatus() {
        val mainActivity = activity as? MainActivity ?: return
        when {
            !mainActivity.isNfcAvailable() -> {
                binding.nfcStatusText.text = getString(R.string.login_nfc_not_supported)
                binding.nfcStatusText.setTextColor(requireContext().getColor(R.color.error))
                binding.nfcIcon.setImageResource(R.drawable.ic_nfc_off)
            }
            !mainActivity.isNfcEnabled() -> {
                binding.nfcStatusText.text = getString(R.string.login_enable_nfc)
                binding.nfcStatusText.setTextColor(requireContext().getColor(R.color.warning))
                binding.enableNfcBtn.visibility = View.VISIBLE
            }
            else -> {
                binding.nfcStatusText.text = getString(R.string.login_nfc_instruction)
                binding.nfcStatusText.setTextColor(requireContext().getColor(R.color.text_secondary_dark))
            }
        }
    }

    private fun setupButtons() {
        binding.enableNfcBtn.setOnClickListener {
            startActivity(android.content.Intent(android.provider.Settings.ACTION_NFC_SETTINGS))
        }

        binding.biometricBtn.setOnClickListener {
            showBiometricLogin()
        }

        binding.pinBtn.setOnClickListener {
            showPinEntry()
        }
    }

    private fun handleNfcResult(result: IraqiIdReader.ReadResult) {
        requireActivity().runOnUiThread {
            when (result) {
                is IraqiIdReader.ReadResult.Success -> {
                    showSuccess(result.data)
                }
                is IraqiIdReader.ReadResult.Error -> {
                    showError(result.message)
                }
                IraqiIdReader.ReadResult.TagLost -> {
                    showError("أُزيلت البطاقة مبكراً — حاول مرة أخرى")
                }
                IraqiIdReader.ReadResult.NfcNotSupported -> {
                    showError(getString(R.string.login_nfc_not_supported))
                }
            }
        }
    }

    private fun showScanning() {
        binding.nfcStatusText.text = getString(R.string.login_nfc_scanning)
        binding.nfcStatusText.setTextColor(requireContext().getColor(R.color.brand_gold))
        binding.nfcProgressBar.visibility = View.VISIBLE
        binding.nfcIcon.setImageResource(R.drawable.ic_nfc_scanning)
    }

    private fun showSuccess(data: IraqiIdReader.IraqiIdData) {
        viewLifecycleOwner.lifecycleScope.launch {
            // Show success state
            binding.nfcProgressBar.visibility = View.GONE
            binding.nfcStatusText.text = getString(R.string.login_nfc_success)
            binding.nfcStatusText.setTextColor(requireContext().getColor(R.color.success))
            binding.nfcIcon.setImageResource(R.drawable.ic_check_circle)

            // Success animation
            binding.nfcCard.animate()
                .scaleX(1.05f).scaleY(1.05f).setDuration(150)
                .withEndAction {
                    binding.nfcCard.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
                }.start()

            // Save user to DB
            val user = User(
                nationalId = data.nationalId,
                fullName = data.fullName,
                fullNameArabic = data.fullNameArabic,
                dateOfBirth = data.dateOfBirth,
                gender = data.gender,
                governorate = data.governorate,
                religion = data.religion,
                issueDate = data.issueDate,
                expiryDate = data.expiryDate,
                motherName = data.motherName,
                fatherName = data.fatherName,
                bloodType = data.bloodType,
                maritalStatus = data.maritalStatus
            )

            db.userDao().insertUser(user)
            prefs.isLoggedIn = true
            prefs.userId = data.nationalId
            prefs.userName = data.fullNameArabic.ifEmpty { data.fullName }

            delay(1000)

            // Navigate to home
            findNavController().navigate(R.id.action_login_to_home)
        }
    }

    private fun showError(message: String) {
        binding.nfcProgressBar.visibility = View.GONE
        binding.nfcStatusText.text = message
        binding.nfcStatusText.setTextColor(requireContext().getColor(R.color.error))
        binding.nfcIcon.setImageResource(R.drawable.ic_nfc_error)

        // Shake animation
        val shake = AnimationUtils.loadAnimation(requireContext(), R.anim.shake)
        binding.nfcCard.startAnimation(shake)

        // Reset after 3 seconds
        viewLifecycleOwner.lifecycleScope.launch {
            delay(3000)
            if (isAdded) checkNfcStatus()
        }
    }

    private fun showBiometricLogin() {
        if (!prefs.isLoggedIn) {
            showError("يرجى تسجيل الدخول بالهوية الوطنية أولاً")
            return
        }
        // Biometric prompt
        val biometricPrompt = androidx.biometric.BiometricPrompt(
            this,
            requireActivity().mainExecutor,
            object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: androidx.biometric.BiometricPrompt.AuthenticationResult) {
                    findNavController().navigate(R.id.action_login_to_home)
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    showError(errString.toString())
                }
                override fun onAuthenticationFailed() {
                    showError("فشل التحقق من البصمة")
                }
            }
        )
        val promptInfo = androidx.biometric.BiometricPrompt.PromptInfo.Builder()
            .setTitle("تسجيل الدخول ببصمة الإصبع")
            .setSubtitle("مرحباً بعودتك")
            .setNegativeButtonText("إلغاء")
            .build()
        biometricPrompt.authenticate(promptInfo)
    }

    private fun showPinEntry() {
        // Navigate to PIN entry (simplified - show dialog)
        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setTitle("إدخال رمز PIN")
            .setMessage("أدخل رمز PIN المكوّن من 6 أرقام")
            .setNegativeButton("إلغاء", null)
            .create()
        dialog.show()
    }

    override fun onResume() {
        super.onResume()
        checkNfcStatus()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
