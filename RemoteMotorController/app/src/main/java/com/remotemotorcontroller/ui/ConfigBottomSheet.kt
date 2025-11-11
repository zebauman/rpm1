package com.remotemotorcontroller.ui

import android.os.Bundle
import android.view.*
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.core.os.bundleOf
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.remotemotorcontroller.R

class ConfigBottomSheet : BottomSheetDialogFragment() {

    companion object {
        fun new(title: String, @LayoutRes contentLayout: Int) =
            ConfigBottomSheet().apply {
                arguments = bundleOf("t" to title, "l" to contentLayout)
            }
    }

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, b: Bundle?): View {
        val root = i.inflate(R.layout.bs_config_host, c, false)
        root.findViewById<TextView>(R.id.bsTitle).text = requireArguments().getString("t")
        i.inflate(requireArguments().getInt("l"), root.findViewById<FrameLayout>(R.id.bsContent), true)
        return root
    }
}
