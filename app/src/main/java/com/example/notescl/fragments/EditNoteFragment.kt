package com.example.notescl.fragments

import android.app.AlertDialog
import android.os.Bundle
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
import androidx.navigation.fragment.navArgs
import com.example.notescl.MainActivity
import com.example.notescl.R
import com.example.notescl.databinding.FragmentEditNoteBinding
import com.example.notescl.model.Note
import com.example.notescl.viewModel.NoteViewModel
import java.text.SimpleDateFormat
import java.util.Date


class EditNoteFragment : Fragment(R.layout.fragment_edit_note),MenuProvider {

    private var editNoteBinding:FragmentEditNoteBinding?=null
    private val binding get() = editNoteBinding!!

    private lateinit var noteViewModel: NoteViewModel
    private lateinit var currentNote: Note

    private val args:EditNoteFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
      editNoteBinding=FragmentEditNoteBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val menuHost: MenuHost =requireActivity()
        menuHost.addMenuProvider(this,viewLifecycleOwner, Lifecycle.State.RESUMED)

        noteViewModel=(activity as MainActivity).noteViewModel
        currentNote=args.note!!


        binding.editNoteTitle.setText(currentNote.title)
        binding.editNoteDesc.setText(currentNote.content)
        binding.updateNoteDate.setText(currentNote.date)

        binding.editNoteFab.setOnClickListener{
            val noteTitle=binding.editNoteTitle.text.toString().trim()
            val noteContent=binding.editNoteDesc.text.toString().trim()
            val d=Date()
            val notesDate:CharSequence=android.text.format.DateFormat.format("MMMM d,yyyy",d.time)

            if(noteTitle.isNotEmpty()){
                val note= Note(0,noteTitle,noteContent,notesDate.toString())
                noteViewModel.updateNote(note)
                view.findNavController().popBackStack(R.id.homeFragment,false)
            }
            else{
                Toast.makeText(context,"Please enter a Note title",Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun deleteNote(){
        AlertDialog.Builder(activity).apply {
            setTitle("Delete Note")
            setMessage("Do you want to delete this note?")
            setPositiveButton("Delete"){_,_->
                noteViewModel.deleteNote(currentNote)
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
            else->false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        editNoteBinding=null
    }
}