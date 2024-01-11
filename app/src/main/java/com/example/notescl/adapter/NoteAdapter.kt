package com.example.notescl.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.notescl.databinding.NoteLayoutBinding
import com.example.notescl.fragments.HomeFragmentDirections
import com.example.notescl.model.Note
import java.text.SimpleDateFormat
import java.util.logging.SimpleFormatter

class NoteAdapter:RecyclerView.Adapter<NoteAdapter.NoteViewHolder>() {

    class NoteViewHolder(val itemBinding:NoteLayoutBinding):RecyclerView.ViewHolder(itemBinding.root)


    private val differCallBack=object :DiffUtil.ItemCallback<Note>(){
        override fun areItemsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem.id==newItem.id &&
                    oldItem.content==newItem.content &&
                    oldItem.title==newItem.title

        }

        override fun areContentsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem==newItem

        }

    }
    val differ=AsyncListDiffer(this,differCallBack)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        return NoteViewHolder(
            NoteLayoutBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        )
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
       val currentNote=differ.currentList[position]

        holder.itemBinding.noteTitle.text=currentNote.title
        holder.itemBinding.noteDesc.text=currentNote.content


        holder.itemView.setOnClickListener{
            val direction=HomeFragmentDirections.actionHomeFragmentToEditNoteFragment(currentNote)
            it.findNavController().navigate(direction)
        }
    }
}