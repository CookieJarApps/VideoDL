package com.cookiejarapps.videodl.fragments

import android.app.Dialog
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.preference.PreferenceManager
import com.cookiejarapps.videodl.R
import kotlinx.android.synthetic.main.dialog_download_path.view.*

class SAFDialogFragment : DialogFragment() {

    private lateinit var listener: DialogListener

    interface DialogListener {
        fun onAccept(dialog: SAFDialogFragment)
        fun onPickFile(dialog: SAFDialogFragment)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            val inflater = requireActivity().layoutInflater

            val view = inflater.inflate(R.layout.dialog_download_path, null)
            val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
            val location = sharedPrefs.getString("download", null)
            if (location != null) {
                val docId = DocumentsContract.getTreeDocumentId(Uri.parse(location))
                docId?.apply { view.download_path.text = docId }
                    ?: run { view.download_path.text = location }
            } else {
                view.download_path.setText(R.string.placeholder)
            }
            builder.setView(view)
                .setIcon(R.drawable.ic_folder)
                .setTitle(R.string.download_location_title)
                .setNegativeButton(R.string.action_choose_folder)
                { _, _ ->
                    listener.onPickFile(this)
                }
                .setPositiveButton(android.R.string.ok)
                { _, _ ->
                    listener.onAccept(this)
                }

            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            listener = parentFragment as DialogListener
        } catch (e: ClassCastException) {
            Log.d("SAFDialogFragment", e.toString())
        }
    }

}

