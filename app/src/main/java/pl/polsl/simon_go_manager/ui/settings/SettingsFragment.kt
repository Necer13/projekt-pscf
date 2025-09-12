package pl.polsl.simon_go_manager.ui.settings

import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.navigation.fragment.findNavController
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import pl.polsl.simon_go_manager.R

class SettingsFragment : PreferenceFragmentCompat() {

    private val PREFS_NAME = "app_prefs"
    private val PASSWORD_KEY = "app_password"
    private val DEFAULT_PASSWORD = "admin123"

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        // Username
        val usernamePref = findPreference<EditTextPreference>("username")
        usernamePref?.summaryProvider = EditTextPreference.SimpleSummaryProvider.getInstance()

        // Theme
        val themePref = findPreference<ListPreference>("app_theme")
        themePref?.setOnPreferenceChangeListener { _, newValue ->
            val mode = when (newValue as String) {
                "light" -> AppCompatDelegate.MODE_NIGHT_NO
                "dark" -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            AppCompatDelegate.setDefaultNightMode(mode)
            true
        }

        // Advanced settings (require password to enter)
        val advancedPref = findPreference<Preference>("advanced_settings")
        advancedPref?.setOnPreferenceClickListener {
            showPasswordForAdvancedSettings()
            true
        }

        // Reset password
        val resetPasswordPref = findPreference<Preference>("reset_password")
        resetPasswordPref?.setOnPreferenceClickListener {
            showResetPasswordDialog()
            true
        }
    }

    private fun showPasswordForAdvancedSettings() {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentPassword = prefs.getString(PASSWORD_KEY, DEFAULT_PASSWORD) ?: DEFAULT_PASSWORD

        val input = EditText(requireContext()).apply {
            hint = "Enter password"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Password required")
            .setView(input)
            .setPositiveButton("OK") { dialog, _ ->
                if (input.text.toString() == currentPassword) {
                    findNavController().navigate(R.id.advancedSettingsFragment)
                } else {
                    Toast.makeText(requireContext(), "Wrong password", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
            .show()
    }

    private fun showResetPasswordDialog() {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentPassword = prefs.getString(PASSWORD_KEY, DEFAULT_PASSWORD) ?: DEFAULT_PASSWORD

        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
        }

        val oldPasswordInput = EditText(requireContext()).apply {
            hint = "Old password"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        val newPasswordInput = EditText(requireContext()).apply {
            hint = "New password"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        val confirmInput = EditText(requireContext()).apply {
            hint = "Confirm new password"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        layout.addView(oldPasswordInput)
        layout.addView(newPasswordInput)
        layout.addView(confirmInput)

        AlertDialog.Builder(requireContext())
            .setTitle("Change password")
            .setView(layout)
            .setPositiveButton("Save") { dialog, _ ->
                val oldPass = oldPasswordInput.text.toString()
                val newPass = newPasswordInput.text.toString()
                val confirmPass = confirmInput.text.toString()

                when {
                    oldPass != currentPassword -> {
                        Toast.makeText(requireContext(), "Old password is incorrect", Toast.LENGTH_SHORT).show()
                    }
                    newPass.isBlank() -> {
                        Toast.makeText(requireContext(), "New password cannot be empty", Toast.LENGTH_SHORT).show()
                    }
                    newPass != confirmPass -> {
                        Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        prefs.edit().putString(PASSWORD_KEY, newPass).apply()
                        Toast.makeText(requireContext(), "Password changed successfully", Toast.LENGTH_SHORT).show()
                    }
                }

                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
            .show()
    }
}
