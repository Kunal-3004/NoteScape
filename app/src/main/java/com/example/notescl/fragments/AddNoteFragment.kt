package com.example.notescl.fragments

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import com.bumptech.glide.Glide
import com.example.notescl.MainActivity
import com.example.notescl.R
import com.example.notescl.databinding.FragmentAddNoteBinding
import com.example.notescl.model.Note
import com.example.notescl.viewModel.NoteViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.io.IOException
import java.util.Date
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


class AddNoteFragment : Fragment(R.layout.fragment_add_note),MenuProvider {

    private var addNoteBinding:FragmentAddNoteBinding?=null
    private val binding get() = addNoteBinding!!

    private lateinit var noteViewModel: NoteViewModel
    private lateinit var addNoteView: View

    private var currentPhotoPath: String? = null
    private val cameraPermissionRequestCode = 101
    private val galleryPermissionRequestCode = 102
    private val captureImageRequestCode = 103
    private val pickImageRequestCode = 104

    private val READ_EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE = 102

    private lateinit var userId: String
    private lateinit var dbFirestore: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
       addNoteBinding=FragmentAddNoteBinding.inflate(inflater,container,false)
        return binding.root



    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val menuHost: MenuHost =requireActivity()
        menuHost.addMenuProvider(this,viewLifecycleOwner, Lifecycle.State.RESUMED)

        noteViewModel=(activity as MainActivity).noteViewModel
        addNoteView=view

        userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        dbFirestore = FirebaseFirestore.getInstance()
    }
    private fun saveNote(view: View) {
        val noteTitle = binding.addNoteTitle.text.toString().trim()
        val noteContent = binding.addNoteDesc.text.toString().trim()
        val d = Date()
        val notesDate: CharSequence = android.text.format.DateFormat.format("MMMM d,yyyy", d.time)

        val user = FirebaseAuth.getInstance().currentUser
        val userId = user?.uid

        if (noteTitle.isNotEmpty()) {
            val note =
                Note("0", noteTitle, noteContent, notesDate.toString(), userId, currentPhotoPath)

            lifecycleScope.launch {
                noteViewModel.addNote(note)

                currentPhotoPath?.let { path ->

                    if (userId!=null) {
                        val imageUrl = uploadImageToStorage(path, userId, note.id)
                        if (imageUrl.isNotEmpty()) {
                            updateImageUrlInFirestore(note.id, imageUrl)
                        }
                    }
                }

                    Toast.makeText(addNoteView.context, "Note Saved", Toast.LENGTH_SHORT).show()
                    view.findNavController().popBackStack(R.id.homeFragment, false)
            }
        }
             else{
            Toast.makeText(addNoteView.context, "Please write a Note title", Toast.LENGTH_SHORT).show()
        }
    }


    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
      menu.clear()
        menuInflater.inflate(R.menu.addnote_menu,menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when(menuItem.itemId){
            R.id.Save_menu->{
                saveNote(addNoteView)
                addNoteToFirestore()
                true
            }
            R.id.addImage->{
                showImageSelectionDialog()
                true
            }
            else->false
        }
    }

    private fun showImageSelectionDialog() {
        val options = arrayOf("Take Photo", "Choose from Gallery", "Cancel")

        AlertDialog.Builder(requireContext())
            .setTitle("Select an Option")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> checkCameraPermission()
                    1 -> dispatchPickImageIntent()
                }
            }
            .show()
    }

    private fun requestStoragePermission() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            READ_EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE
        )
    }
    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            dispatchTakePictureIntent()
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.CAMERA),
                cameraPermissionRequestCode
            )
        }
    }

    private fun checkGalleryPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            dispatchPickImageIntent()
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                galleryPermissionRequestCode
            )
        }
    }
    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(requireContext().packageManager)?.also {
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    null
                }

                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        requireContext(),
                        "${requireContext().packageName}.fileprovider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, captureImageRequestCode)
                }
            }
        }
    }

    private fun createImageFile(): File {
        val storageDir: File? = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${System.currentTimeMillis()}_",
            ".jpg",
            storageDir
        ).apply {
            currentPhotoPath = absolutePath
        }
    }

    private fun dispatchPickImageIntent() {
        val pickImageIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
        }
        startActivityForResult(pickImageIntent, pickImageRequestCode)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            READ_EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showImageSelectionDialog()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Gallery permission denied",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            captureImageRequestCode -> {
                if (resultCode == Activity.RESULT_OK) {
                    loadImageIntoImageView(Uri.fromFile(File(currentPhotoPath)))
                }
            }
            pickImageRequestCode -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    val selectedImageUri: Uri? = data.data
                    loadImageIntoImageView(selectedImageUri)
                }
            }
        }
    }


    private fun loadImageIntoImageView(imageUri: Uri?) {
        imageUri?.let {
            Glide.with(requireContext())
                .load(it)
                .into(binding.imgNote)
            binding.imgNote.visibility = View.VISIBLE
        }
    }


    private fun addNoteToFirestore() {
        val noteTitle = binding.addNoteTitle.text.toString().trim()
        val noteContent = binding.addNoteDesc.text.toString().trim()
        val d = Date()
        val notesDate: CharSequence = android.text.format.DateFormat.format("MMMM d,yyyy", d.time)

        if (noteTitle.isNotEmpty()) {
            val user = FirebaseAuth.getInstance().currentUser
            val userId = user?.uid
            if (userId != null) {
                val noteMap = mapOf(
                    "title" to noteTitle,
                    "content" to noteContent,
                    "date" to notesDate.toString(),
                    "userId" to userId
                )
                val dbFireStore = FirebaseFirestore.getInstance()
                dbFireStore.collection("notes")
                    .add(noteMap)
                    .addOnSuccessListener { documentReference ->
                        Log.d(
                            "Firestore",
                            "Note added successfully. Document ID: ${documentReference.id}"
                        )
                    }
                    .addOnFailureListener { e ->
                        Log.e("Firestore", "Error adding note to Firestore: ${e.message}", e)
                    }
            }
        }
    }

    private fun uploadImageToStorage(imagePath: String, userId: String, noteId: String): String {
        val storageReference = FirebaseStorage.getInstance().reference
        val imageRef = storageReference.child("images/$userId/$noteId.jpg")

        try {
            val imageByteArray = convertImageToByteArray(imagePath)

            val uploadTask = imageRef.putBytes(imageByteArray)
            uploadTask.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    imageRef.downloadUrl.addOnCompleteListener { uriTask ->
                        if (uriTask.isSuccessful) {
                            val imageUrl = uriTask.result.toString()
                            imageUrl
                        } else {
                            Log.e("Firestore", "Error getting download URL: ${uriTask.exception?.message}", uriTask.exception)
                            ""
                        }
                    }
                } else {
                    Log.e("Firestore", "Image upload failed with error: ${task.exception?.message}", task.exception)
                    ""
                }
            }
        } catch (e: Exception) {
            Log.e("Firestore", "Error uploading image to Firebase Storage: ${e.message}", e)
            ""
        }
        return ""
    }
    private fun convertImageToByteArray(imagePath: String): ByteArray {
        val file = File(imagePath)
        return file.readBytes()
    }

    private fun updateImageUrlInFirestore(noteId: String, imageUrl: String) {
            val noteDocument = dbFirestore.collection("notes").document(noteId)
            noteDocument.update("imageUrl", imageUrl)
                .addOnSuccessListener {
                }
                .addOnFailureListener { e ->
                }
        }

    override fun onDestroy() {
        super.onDestroy()
        addNoteBinding=null
    }
}