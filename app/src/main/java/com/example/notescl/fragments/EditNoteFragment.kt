package com.example.notescl.fragments

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.notescl.MainActivity
import com.example.notescl.R
import com.example.notescl.adapter.NoteAdapter
import com.example.notescl.databinding.FragmentEditNoteBinding
import com.example.notescl.model.Note
import com.example.notescl.repository.NoteRepository
import com.example.notescl.viewModel.NoteViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.Date


class EditNoteFragment : Fragment(R.layout.fragment_edit_note),MenuProvider {

    private var editNoteBinding: FragmentEditNoteBinding? = null
    private val binding get() = editNoteBinding!!

    private lateinit var noteViewModel: NoteViewModel
    private lateinit var currentNote: Note
    private lateinit var noteAdapter: NoteAdapter

    private var currentPhotoPath: String? = null
    private val cameraPermissionRequestCode = 101
    private val captureImageRequestCode = 103
    private val pickImageRequestCode = 104
    private val READ_EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE = 102

    private lateinit var userId: String
    private lateinit var dbFirestore: FirebaseFirestore
    private val args: EditNoteFragmentArgs by navArgs()

    private var imageUrl: String = ""
    private var imageUploadDeferred = CompletableDeferred<String?>()




    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        editNoteBinding = FragmentEditNoteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        noteAdapter = NoteAdapter()
        noteViewModel = (activity as MainActivity).noteViewModel
        currentNote = args.note!!
        imageUploadDeferred = CompletableDeferred()
        dbFirestore = FirebaseFirestore.getInstance()


        binding.editNoteTitle.setText(currentNote.title)
        binding.editNoteDesc.setText(currentNote.content)
        binding.updateNoteDate.setText(currentNote.date)

        if (currentNote.imageUrl?.isNotEmpty() == true) {
            loadImageIntoImageView(Uri.parse(currentNote.imageUrl))
        }

        binding.editNoteFab.setOnClickListener {
            val noteTitle = binding.editNoteTitle.text.toString().trim()
            val noteContent = binding.editNoteDesc.text.toString().trim()
            val d = Date()
            val notesDate: CharSequence =
                android.text.format.DateFormat.format("MMMM d,yyyy", d.time)

            val user = FirebaseAuth.getInstance().currentUser
            val userId = user?.uid

            if (noteTitle.isNotEmpty()) {
                currentNote.title = noteTitle
                currentNote.content = noteContent
                currentNote.date = notesDate.toString()

                currentPhotoPath?.let { path ->
                    if (currentNote.id.isNotEmpty()) {
                        lifecycleScope.launchWhenResumed{
                            try {

                                val updatedImageUrl = imageUploadDeferred.await()

                                if (!updatedImageUrl.isNullOrEmpty()) {
                                    currentNote.imageUrl = updatedImageUrl

                                    noteViewModel.updateNote(currentNote)
                                    val updatedNote = noteViewModel.getNoteById(currentNote.id)
                                    refreshUI(updatedNote)
                                    updateNoteInFirestore(currentNote, updatedImageUrl)

                                    view?.findNavController()
                                        ?.popBackStack(R.id.homeFragment, false)
                                } else {
                                    Log.e(
                                        "Firestore",
                                        "Empty or null imageUrl after uploading image"
                                    )
                                    Toast.makeText(
                                        requireContext(),
                                        "Error uploading image",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } catch (e: Exception) {
                                Log.e("Firestore", "Error uploading image: ${e.message}", e)
                                Toast.makeText(
                                    requireContext(),
                                    "Error uploading image",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
            }
        }
    }
    private fun refreshUI(updatedNote: Note) {
        binding.editNoteTitle.setText(updatedNote.title)
        binding.editNoteDesc.setText(updatedNote.content)
        binding.updateNoteDate.setText(updatedNote.date)

        if (updatedNote.imageUrl?.isNotEmpty() == true) {
            loadImageIntoImageView(Uri.parse(updatedNote.imageUrl))
        }
    }

    private fun deleteNote(){
        AlertDialog.Builder(activity).apply {
            setTitle("Delete Note")
            setMessage("Do you want to delete this note?")
            setPositiveButton("Delete"){_,_->
                noteViewModel.deleteNote(currentNote)
                val noteId=currentNote.id
                noteViewModel.deleteNoteFromFirestore(noteId)
                Toast.makeText(context,"Note Deleted",Toast.LENGTH_SHORT).show()
                view?.findNavController()?.popBackStack(R.id.homeFragment,false)
            }
            setNegativeButton("Cancel",null)
        }.create().show()
    }
    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menu.clear()
        menuInflater.inflate(R.menu.delete_menu,menu)
    }
    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when(menuItem.itemId){
            R.id.delete_menu->{
                deleteNote()
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
                    1 -> checkGalleryPermission()
                }
            }
            .show()
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
        val galleryIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
        }
        startActivityForResult(galleryIntent, pickImageRequestCode)
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
                    currentPhotoPath = it.absolutePath
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
                    currentPhotoPath = data.data?.path
                    loadImageIntoImageView(data.data)

                    if (currentPhotoPath != null) {
                        lifecycleScope.launch {
                            try {
                                val imageUrl = uploadImageAndStoreInFirestore(
                                    currentNote.id,
                                    currentPhotoPath!!
                                )
                                val updatedImageUrl = imageUploadDeferred.await()
                                updateNoteInFirestore(currentNote, updatedImageUrl)
                            } catch (e: Exception) {
                                Log.e("Firestore", "Error uploading image: ${e.message}", e)
                                Toast.makeText(
                                    requireContext(),
                                    "Error uploading image",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun loadImageIntoImageView(imageUri: Uri?) {
        imageUri?.let {
            Glide.with(requireContext())
                .load(it)
                .into(binding.editNoteImg)

            binding.editNoteImg.visibility= View.VISIBLE
        }
    }

    private fun updateNoteInFirestore(note: Note, imageUrl: String?) {
        val noteDocument = dbFirestore.collection("notes").document(note.id)
        val noteMap = mapOf(
            "title" to note.title,
            "content" to note.content,
            "date" to note.date,
            "userId" to note.userId,
            "imageUrl" to imageUrl
        )

        noteDocument.set(noteMap, SetOptions.merge())
            .addOnSuccessListener {
                Log.d("Firestore", "Note updated successfully. Document ID: ${note.id}")
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error updating note in Firestore: ${e.message}", e)
            }
    }

    suspend fun uploadImageAndStoreInFirestore(noteId: String, imagePath: String): String? {
        val user = FirebaseAuth.getInstance().currentUser

        return if (user != null) {
            val userId = user.uid
            val storageReference = FirebaseStorage.getInstance().reference
            val imageRef = storageReference.child("images/$userId/$noteId.jpg")

            val imageByteArray = convertImageToByteArray(imagePath)

            try {
                val uploadTask = imageRef.putBytes(imageByteArray)
                uploadTask.await()

                val imageUrl = imageRef.downloadUrl.await().toString()
                this@EditNoteFragment.imageUrl = imageUrl
                imageUploadDeferred.complete(imageUrl)
                loadImageIntoImageView(Uri.parse(imageUrl))
                imageUrl
            } catch (e: Exception) {
                Log.e("EditNoteFragment", "Error uploading image: ${e.message}", e)
                Toast.makeText(
                    requireContext(),
                    "Error uploading image: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                null
            }
        } else {
            null
        }
    }

    private fun convertImageToByteArray(imagePath: String): ByteArray {
        val file = File(imagePath)
        return try {
            val inputStream = FileInputStream(file)
            val byteArray = ByteArray(file.length().toInt())
            inputStream.read(byteArray)
            inputStream.close()
            byteArray
        } catch (e: IOException) {
            Log.e("EditNoteFragment", "Error converting image to byte array: ${e.message}", e)
            ByteArray(0)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        editNoteBinding=null
    }
}