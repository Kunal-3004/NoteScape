package com.example.notescl.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.navigation.findNavController
import com.example.notescl.MainActivity
import com.example.notescl.R
import com.example.notescl.databinding.FragmentAddNoteBinding
import com.example.notescl.model.Note
import com.example.notescl.viewModel.NoteViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date


class AddNoteFragment : Fragment(R.layout.fragment_add_note),MenuProvider {

    private var addNoteBinding:FragmentAddNoteBinding?=null
    private val binding get() = addNoteBinding!!

    private lateinit var noteViewModel: NoteViewModel
    private lateinit var addNoteView: View



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

        val markdownEditText=binding.addNoteDesc
        val stylusBar= binding.styleBar
        markdownEditText.setStylesBar(stylusBar)


    }
    private fun saveNote(view: View) {
        val noteTitle = binding.addNoteTitle.text.toString().trim()
        val noteContent = binding.addNoteDesc.text.toString().trim()
        val d = Date()
        val notesDate: CharSequence = android.text.format.DateFormat.format("MMMM d,yyyy", d.time)

        val user = FirebaseAuth.getInstance().currentUser
        val userId = user?.uid

        if (noteTitle.isNotEmpty()) {
            val note = Note(0, noteTitle, noteContent, notesDate.toString(),userId)
            noteViewModel.addNote(note)
            Toast.makeText(addNoteView.context,"Note Saved",Toast.LENGTH_SHORT).show()
            view.findNavController().popBackStack(R.id.homeFragment, false)

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
            else->false
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



    override fun onDestroy() {
        super.onDestroy()
        addNoteBinding=null
    }
}