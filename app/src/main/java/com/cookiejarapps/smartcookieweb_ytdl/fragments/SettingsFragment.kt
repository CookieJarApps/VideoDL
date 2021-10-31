package com.cookiejarapps.smartcookieweb_ytdl.fragments

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.Menu
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.cookiejarapps.smartcookieweb_ytdl.NavActivity
import com.cookiejarapps.smartcookieweb_ytdl.R


class SettingsFragment : PreferenceFragmentCompat() {

    var downloadLocationPref: Preference? = null
    var downloadSplitPref: Preference? = null
    var downloadManagerPref: Preference? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preference_headers, rootKey)
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)

        downloadLocationPref =
            findPreference(DOWNLOAD_LOCATION)
        downloadLocationPref?.let {
            val location = sharedPrefs.getString(DOWNLOAD_LOCATION, null)
            location?.apply { updatePathInSummary(it, this) } ?: it.setSummary(R.string.placeholder)
            it.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                openDirectoryChooser()
                true
            }
        }

        downloadSplitPref =
            findPreference(DOWNLOAD_SPLIT)
        downloadSplitPref?.let {
            updateDownloadSplit(it)
            it.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                setDownloadSplit(it)
                true
            }
        }

        val downloader = PreferenceManager.getDefaultSharedPreferences(context).getString(
            DOWNLOAD_MANAGER, resources.getString(
                R.string.internal
            )
        )

        downloadManagerPref =
            findPreference(DOWNLOAD_MANAGER)
        downloadManagerPref?.let {
            updateDownloadManager(it)
            it.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                setDownloadManager(it)
                true
            }
        }

        downloadSplitPref?.isEnabled = downloader == resources.getString(R.string.internal)
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

    private fun setDownloadSplit(preference: Preference){
        val list: List<Int> = 1.rangeTo(16).toList()
        val items: Array<CharSequence> = list.map { it.toString() }.toTypedArray()

        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(requireContext().resources.getString(R.string.download_split))

        builder.setItems(items) { dialog, which ->
            val editor = PreferenceManager.getDefaultSharedPreferences(context).edit()
            editor.putInt(DOWNLOAD_SPLIT, which + 1).apply()
            updateDownloadSplit(preference)
        }

        val dialog = builder.create()
        dialog.show()
    }

    private fun updateDownloadSplit(preference: Preference) {
        val split = PreferenceManager.getDefaultSharedPreferences(context).getInt(
            DOWNLOAD_SPLIT, 1
        )

        val part = if(split == 1) resources.getString(R.string.part) else resources.getString(R.string.parts)

        preference.summary = "$split $part"
    }

    private fun setDownloadManager(preference: Preference){
        val packageManager = requireActivity().getPackageManager()
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setData(Uri.parse("https://cookiejarapps.com/blank.mp4"))
        val list = packageManager.queryIntentActivities(intent,
        PackageManager.MATCH_ALL)

        val nameList: MutableList<CharSequence> = emptyList<CharSequence>().toMutableList()
        for(i in list){
            if(i.activityInfo.name != null){ nameList.add(i.activityInfo.loadLabel(packageManager)) }
        }
        nameList.add(0, resources.getString(R.string.internal))

        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(requireContext().resources.getString(R.string.downloader))

        builder.setItems(nameList.toTypedArray()) { dialog, which ->
            val editor = PreferenceManager.getDefaultSharedPreferences(context).edit()
            if(which == 0){
                editor.putString(DOWNLOAD_MANAGER, resources.getString(R.string.internal)).apply()
            }
            else{
                editor.putString(DOWNLOAD_MANAGER, list[which - 1].activityInfo.packageName).apply()
                editor.putString(DOWNLOAD_MANAGER_ACTIVITY, list[which - 1].activityInfo.name).apply()
            }
            updateDownloadManager(preference)
        }

        val dialog = builder.create()
        dialog.show()
    }

    private fun updateDownloadManager(preference: Preference) {
        val downloader = PreferenceManager.getDefaultSharedPreferences(context).getString(
            DOWNLOAD_MANAGER, resources.getString(
                R.string.internal
            )
        )

        val name: String = if(downloader != resources.getString(R.string.internal)){
            val packageManager = requireActivity().getPackageManager()
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setData(Uri.parse("https://cookiejarapps.com/blank.mp4"))
            val list = packageManager.queryIntentActivities(intent,
                PackageManager.MATCH_ALL)

            var final = resources.getString(R.string.internal)
            for(i in list){
                if(i.activityInfo.packageName == downloader){
                    final = i.activityInfo.applicationInfo.loadLabel(packageManager).toString()
                }
            }
            final
        }
        else{
            resources.getString(R.string.internal)
        }

        downloadSplitPref?.isEnabled = downloader == resources.getString(R.string.internal)

        preference.summary = name
    }

    companion object {
        private const val OPEN_DIRECTORY_REQUEST_CODE = 42070
        private const val DOWNLOAD_LOCATION = "download"
        private const val DOWNLOAD_MANAGER = "download_manager"
        private const val DOWNLOAD_SPLIT = "download_split"
        private const val DOWNLOAD_MANAGER_ACTIVITY = "download_manager_activity"
    }
}
