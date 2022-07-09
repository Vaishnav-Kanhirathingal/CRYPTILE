package com.example.cryptile.ui_fragments.prompt

import android.app.AlertDialog
import android.app.Dialog
import android.app.ProgressDialog
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import com.example.cryptile.databinding.PromptLoadingBinding
import com.example.cryptile.databinding.PromptMessageBinding
import com.example.cryptile.databinding.PromptVerifyAccountBinding
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "AdditionalPrompts"

object AdditionalPrompts {
    private lateinit var loadingBinding: PromptLoadingBinding
    private lateinit var loadingDialog: Dialog

    fun confirmationPrompt(
        context: Context,
        title: String,
        message: String,
        onSuccess: () -> Unit
    ) {
        Log.d(TAG, "title : $title, message: $message")
        val dialogBox = AlertDialog.Builder(context)
        dialogBox.apply {
            setTitle(title)
            setMessage(message)
            setCancelable(true)
            setNegativeButton("No") { _, _ -> }
            setPositiveButton("Yes") { _, _ -> onSuccess() }
            show()
        }
    }

    fun initializeLoading(
        layoutInflater: LayoutInflater,
        context: Context,
        title: String
    ) {
        CoroutineScope(Dispatchers.Main).launch{
            loadingBinding = PromptLoadingBinding.inflate(layoutInflater)
            loadingDialog = Dialog(context)
            loadingDialog.apply {
                setContentView(loadingBinding.root)
                window!!.setLayout(MATCH_PARENT, WRAP_CONTENT)
                setCancelable(false)
                show()
            }
            loadingBinding.progressTitleTextView.text = "$title..."
        }
    }

    fun addProgress(
        progress: Int,
        dismiss: Boolean
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            if (dismiss) {
                loadingDialog.dismiss()
            } else {
                loadingBinding.progressBar.progress = progress
            }
        }
    }

    fun verifyUser(
        layoutInflater: LayoutInflater,
        context: Context,
        notice: String,
        onSuccess: () -> Unit
    ) {
        val auth = FirebaseAuth.getInstance()
        val binding = PromptVerifyAccountBinding.inflate(layoutInflater)
        val dialogBox = Dialog(context)
        dialogBox.apply {
            setContentView(binding.root)
            window!!.setLayout(MATCH_PARENT, WRAP_CONTENT)
            setCancelable(true)
            show()
        }
        Log.d(TAG, "provider - ${auth.currentUser!!.providerId}")// TODO: check why firebase
        binding.apply {
            noticeTextView.text = "*** $notice ***"
            confirmButton.setOnClickListener {
                val cred = EmailAuthProvider.getCredential(
                    userEmailTextLayout.editText!!.text.toString(),
                    userPasswordTextLayout.editText!!.text.toString()
                )
                auth.currentUser!!.reauthenticate(cred).addOnSuccessListener {
                    Log.d(TAG, "user authenticated")
                    onSuccess()
                    dialogBox.dismiss()
                }.addOnFailureListener {
                    userEmailTextLayout.apply {
                        error = "Email might be incorrect"
                        isErrorEnabled = true
                    }
                    userPasswordTextLayout.apply {
                        error = "Password might be incorrect"
                        isErrorEnabled = true
                    }
                }
            }
            cancelButton.setOnClickListener { dialogBox.dismiss() }
        }
    }


    fun showMessagePrompt(
        context: Context,
        layoutInflater: LayoutInflater,
        message: String,
        onDismiss: () -> Unit
    ) {
        val alertDialog = Dialog(context)
        val binding = PromptMessageBinding.inflate(layoutInflater)
        alertDialog.apply {
            setContentView(binding.root)
            window!!.setLayout(MATCH_PARENT, WRAP_CONTENT)
            setCancelable(false)
            show()
        }
        binding.apply {
            messageTextView.text = message
            dismissButton.setOnClickListener {
                alertDialog.dismiss()
                onDismiss()
            }
        }
    }
}