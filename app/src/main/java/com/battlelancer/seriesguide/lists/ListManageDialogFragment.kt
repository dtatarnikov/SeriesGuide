// SPDX-License-Identifier: Apache-2.0
// Copyright 2012-2024 Uwe Trottmann

package com.battlelancer.seriesguide.lists

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.DialogListManageBinding
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.util.safeShow
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

/**
 * Dialog to rename or remove a list.
 */
class ListManageDialogFragment : AppCompatDialogFragment() {

    private var binding: DialogListManageBinding? = null
    private lateinit var listId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // hide title, use custom theme
        setStyle(STYLE_NO_TITLE, 0)

        listId = requireArguments().getString(ARG_LIST_ID)!!
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogListManageBinding.inflate(layoutInflater)
        this.binding = binding

        // buttons
        binding.buttonListManageDelete.apply {
            isEnabled = false
            setText(R.string.list_remove)
            setOnClickListener {
                // ask about removing list
                DeleteListDialogFragment.create(listId)
                    .safeShow(parentFragmentManager, "confirm-delete-list")
                dismiss()
            }
        }
        binding.buttonListManageConfirm.apply {
            setText(R.string.action_save)
            setOnClickListener {
                val editText =
                    this@ListManageDialogFragment.binding?.textInputLayoutListManageListName?.editText
                        ?: return@setOnClickListener

                // update title
                val listName = editText.text.toString().trim()
                ListsTools.renameList(requireContext(), listId, listName)

                dismiss()
            }
        }

        // Delay loading data for views to after this function
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                configureViews()
            }
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.getRoot())
            .create()
    }

    private fun configureViews() {
        // Querying on main thread as the queries are very small
        val listHelper = SgRoomDatabase.getInstance(requireContext()).sgListHelper()
        // pre-populate list title
        val list = listHelper.getList(listId)
        if (list == null) {
            // list might have been removed, or query failed
            dismiss()
            return
        }
        val listName = list.name

        val binding = this@ListManageDialogFragment.binding
        if (binding == null) {
            dismiss()
            return
        }

        val textInputLayoutName = binding.textInputLayoutListManageListName
        val editTextName = textInputLayoutName.editText!!
        editTextName.setText(listName)
        editTextName.addTextChangedListener(
            AddListDialogFragment.ListNameTextWatcher(
                requireContext(), textInputLayoutName,
                binding.buttonListManageConfirm, listName
            )
        )

        // do only allow removing if this is NOT the last list
        val listsCount = listHelper.getListsCount()
        if (listsCount > 1) {
            binding.buttonListManageDelete.isEnabled = true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    companion object {

        private const val TAG = "listmanagedialog"
        private const val ARG_LIST_ID = "listId"

        private fun newInstance(listId: String): ListManageDialogFragment {
            val f = ListManageDialogFragment()
            val args = Bundle()
            args.putString(ARG_LIST_ID, listId)
            f.arguments = args
            return f
        }

        /**
         * Display a dialog which allows to edit the title of this list or remove it.
         */
        fun show(listId: String, fm: FragmentManager) {
            // replace any currently showing list dialog (do not add it to the back stack)
            val ft = fm.beginTransaction()
            val prev = fm.findFragmentByTag(TAG)
            if (prev != null) {
                ft.remove(prev)
            }
            newInstance(listId).safeShow(fm, ft, TAG)
        }
    }
}