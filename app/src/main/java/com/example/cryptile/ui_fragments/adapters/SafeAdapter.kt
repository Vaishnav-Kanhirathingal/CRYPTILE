package com.example.cryptile.ui_fragments.adapters

import android.app.Dialog
import android.content.Context
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.asLiveData
import androidx.navigation.NavController
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.cryptile.R
import com.example.cryptile.app_data.room_files.SafeData
import com.example.cryptile.databinding.ListItemSafeBinding
import com.example.cryptile.databinding.PromptOpenSafeBinding
import com.example.cryptile.firebase.UserDataConstants
import com.example.cryptile.ui_fragments.MainFragmentDirections
import com.example.cryptile.ui_fragments.prompt.AdditionalPrompts
import com.example.cryptile.view_models.AppViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat

private const val TAG = "SafeAdapter"

class SafeAdapter(
    private val viewModel: AppViewModel,
    private val lifeCycle: LifecycleOwner,
    private val inflater: LayoutInflater,
    private val navController: NavController,
) :
    ListAdapter<Int, SafeAdapter.SafeAdapterViewHolder>(diffCallBack) {

    class SafeAdapterViewHolder(
        private val binding: ListItemSafeBinding,
        private val context: Context
    ) : RecyclerView.ViewHolder(binding.root) {

        private var locationCorrect: Boolean = true

        /**
         * applies bindings that affect the visuals of the card. no listeners
         */
        fun bind(safeData: SafeData) {
            binding.apply {
                fileNameTextView.text = safeData.safeName
                locationCorrect = File(
                    Environment.getExternalStorageDirectory(), safeData.safeAbsoluteLocation
                ).exists()
                safeCondition.setImageResource(
                    if (locationCorrect) {
                        R.drawable.offline_24
                    } else {
                        R.drawable.error_24
                    }
                )
                safeCreatedTextView.text = context.getString(
                    R.string.created_on,
                    SimpleDateFormat("dd/MM/yyyy").format(safeData.safeCreated)
                )
                safeOwnerTextView.text = safeData.safeOwner

                if (safeData.hideSafePath) {
                    safePathImageView.setImageResource(R.drawable.location_off_24)
                    safeAbsolutePathTextView.text = context.getString(R.string.location_hidden)
                } else {
                    safePathImageView.setImageResource(R.drawable.location_24)
                    safeAbsolutePathTextView.text = safeData.safeAbsoluteLocation
                }
                if (safeData.personalAccessOnly) {
                    safeAccessImageView.setImageResource(R.drawable.personal_safe_24)
                    safeAccessTextView.text = context.getString(R.string.owner_access_only)
                } else {
                    safeAccessImageView.setImageResource(R.drawable.lock_24)
                    safeAccessTextView.text = context.getString(R.string.public_access)
                }
            }
        }

        /**
         * listener for the card. also takes care of the password prompts
         */
        fun promptListener(
            inflater: LayoutInflater,
            safeData: SafeData,
            viewModel: AppViewModel,
            navController: NavController
        ) {
            if (locationCorrect) {
                val binding: PromptOpenSafeBinding = PromptOpenSafeBinding.inflate(inflater)
                val dialogBox = Dialog(context)
                binding.apply {
                    safeNameTextView.text = safeData.safeName
                    passwordTwoTextLayout.isEnabled = safeData.safeUsesMultiplePassword
                    resetImageButton.setOnClickListener {
                        passwordOneTextLayout.apply {
                            isEnabled = true
                            isErrorEnabled = false
                            editText!!.setText("")
                        }
                        passwordTwoTextLayout.apply {
                            isEnabled = safeData.safeUsesMultiplePassword
                            isErrorEnabled = false
                            editText!!.setText("")
                        }
                    }
                    removeImageButton.setOnClickListener { viewModel.delete(safeData);dialogBox.dismiss() }
                    passwordOneTextLayout.apply {
                        setEndIconOnClickListener {
                            this.isEnabled = false
                        }
                    }
                    passwordTwoTextLayout.apply {
                        setEndIconOnClickListener {
                            this.isEnabled = false
                        }
                    }
                    cancelButton.setOnClickListener { dialogBox.dismiss() }
                    openButton.setOnClickListener {
                        openButton.isEnabled = false
                        Firebase.firestore
                            .collection(UserDataConstants.tableName)
                            .document(Firebase.auth.currentUser!!.uid)
                            .get()
                            .addOnSuccessListener {
                                CoroutineScope(Dispatchers.IO).launch {
                                    val keyList = if (safeData.safeUsesMultiplePassword) {
                                        safeData.getKey(
                                            passwordOne = passwordOneTextLayout.editText!!.text.toString(),
                                            passwordTwo = passwordTwoTextLayout.editText!!.text.toString()
                                        )
                                    } else {
                                        safeData.getKey(
                                            passwordOne = passwordOneTextLayout.editText!!.text.toString()
                                        )
                                    }
                                    if (safeData.personalAccessOnly) {
                                        keyList.add(
                                            SafeData.stringToKey(
                                                it.get(UserDataConstants.userKey).toString()
                                            )
                                        )
                                    }
                                    val keyIsCorrect: Boolean = try {
                                        safeData.checkKeyGenerated(keyList)
                                    } catch (e: Exception) {
                                        e.printStackTrace();false
                                    }
                                    val stringList = mutableListOf<String>()
                                    for (i in keyList) stringList.add(SafeData.keyToString(i))


                                    val gson = Gson().toJson(stringList)
                                    CoroutineScope(Dispatchers.Main).launch {
                                        if (keyIsCorrect) {
                                            dialogBox.dismiss()
                                            navController.navigate(
                                                MainFragmentDirections.actionMainFragmentToSafeViewerFragment(
                                                    safeData.id,
                                                    gson
                                                )
                                            )
                                        } else {
                                            passwordOneTextLayout.apply {
                                                error =
                                                    "Password might be incorrect"; isErrorEnabled =
                                                true
                                            }
                                            passwordTwoTextLayout.apply {
                                                if (safeData.safeUsesMultiplePassword) error =
                                                    "Password might be incorrect"; isErrorEnabled =
                                                true
                                            }
                                        }
                                        openButton.isEnabled = true
                                    }
                                }
                            }.addOnFailureListener {
                                it.printStackTrace()
                                Toast.makeText(
                                    context,
                                    "an error has occurred: ${it.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                }
                dialogBox.apply {
                    setContentView(binding.root)
                    window!!.setLayout(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    setCancelable(true)
                    show()
                }
            } else {
                AdditionalPrompts.confirmationPrompt(
                    context = context,
                    title = "Warning!!!",
                    message = "The safe you were looking for no longer exists at it's specified " +
                            "location. You may want to import it again. Remove safe data from " +
                            "the app?",
                    onSuccess = { viewModel.delete(safeData) }
                )
            }
        }
    }

    companion object {
        val diffCallBack = object : DiffUtil.ItemCallback<Int>() {
            override fun areItemsTheSame(oldItem: Int, newItem: Int) = oldItem == newItem
            override fun areContentsTheSame(oldItem: Int, newItem: Int) = oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SafeAdapterViewHolder =
        SafeAdapterViewHolder(
            ListItemSafeBinding.inflate(LayoutInflater.from(parent.context)),
            parent.context
        )

    override fun onBindViewHolder(holder: SafeAdapterViewHolder, position: Int) {
        viewModel.getById(getItem(position)).asLiveData().observe(lifeCycle) {
            try {
                holder.bind(it)
                val data = it
                holder.itemView.setOnClickListener {
                    Log.d(TAG, "item selected = $data")
                    holder.promptListener(
                        inflater = inflater,
                        safeData = data,
                        viewModel = viewModel,
                        navController = navController
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}