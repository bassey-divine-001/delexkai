package com.delexai.controller.databinding

import androidx.viewbinding.ViewBinding

/**
 * Extension functions for ViewBinding utilities.
 */

/**
 * Extension to safely access the binding root view.
 */
inline fun <T : ViewBinding> T.root() = this.root
