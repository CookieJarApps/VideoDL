package com.cookiejarapps.smartcookieweb_ytdl.fragments

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.Menu
import android.view.View
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreference
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.cookiejarapps.smartcookieweb_ytdl.NavActivity
import com.cookiejarapps.smartcookieweb_ytdl.R
import com.yausername.youtubedl_android.YoutubeDL


class SettingsFragment : PreferenceFragmentCompat() {


    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preference_headers, rootKey)
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)

        val downloadLocationPref: Preference? =
            findPreference(DOWNLOAD_LOCATION)
        downloadLocationPref?.let {
            val location = sharedPrefs.getString(DOWNLOAD_LOCATION, null)
            location?.apply { updatePathInSummary(it, this) } ?: it.setSummary(R.string.placeholder)
            it.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                openDirectoryChooser()
                true
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? NavActivity)?.hideNav()
        setHasOptionsMenu(true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (activity as? NavActivity)?.showNav()
        (activity as? NavActivity)?.showOptions()
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        (activity as? NavActivity)?.hideOptions()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            OPEN_DIRECTORY_REQUEST_CODE -> {
                if (resultCode == Activity.RESULT_OK) {
                    data?.data?.let {
                        activity?.contentResolver?.takePersistableUriPermission(
                            it,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
                        updateDefaultDownloadLocation(it.toString())
                    }
                }
            }
        }
    }

    private fun openDirectoryChooser() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        }
        startActivityForResult(intent, OPEN_DIRECTORY_REQUEST_CODE)
    }

    private fun updateDefaultDownloadLocation(path: String) {
        val editor = PreferenceManager.getDefaultSharedPreferences(context).edit()
        editor.putString(DOWNLOAD_LOCATION, path).apply()
        findPreference<Preference>(DOWNLOAD_LOCATION)?.let { preference ->
            updatePathInSummary(preference, path)
        }
    }

    private fun updatePathInSummary(preference: Preference, path: String) {
        val docId = DocumentsContract.getTreeDocumentId(Uri.parse(path))
        docId?.apply { preference.summary = docId }
            ?: run { preference.summary = path }
    }

    companion object {
        private const val OPEN_DIRECTORY_REQUEST_CODE = 42070
        private const val DOWNLOAD_LOCATION = "download"
    }
}
