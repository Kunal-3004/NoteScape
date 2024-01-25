package com.example.notescl.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.lifecycle.LiveData
import androidx.navigation.findNavController
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.notescl.R
import com.example.notescl.databinding.NoteLayoutBinding
import com.example.notescl.fragments.HomeFragmentDirections
import com.example.notescl.model.Note
import java.text.SimpleDateFormat
import java.util.logging.SimpleFormatter

class NoteAdapter:RecyclerView.Adapter<NoteAdapter.NoteViewHolder>(),Filterable {

    private var originalList: List<Note> = mutableListOf()

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
        holder.itemBinding.noteDate.text=currentNote.date

        if (!currentNote.imageUrl.isNullOrBlank()) {
            Glide.with(holder.itemView.context)
                .load(currentNote.imageUrl)
                .into(holder.itemBinding.noteImg)
            holder.itemBinding.noteImg.visibility = View.VISIBLE
        } else {
            holder.itemBinding.noteImg.visibility = View.GONE
        }


        holder.itemView.setOnClickListener{
            val direction=HomeFragmentDirections.actionHomeFragmentToEditNoteFragment(currentNote)
            it.findNavController().navigate(direction)
        }
    }

    fun submitListAndFilter(newList: List<Note>) {
        originalList = newList
        differ.submitList(newList)
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val filteredResults = if (constraint.isNullOrBlank()) {
                    originalList
                } else {
                    originalList.filter {
                        it.title.contains(constraint, true) || it.content.contains(constraint, true)
                    }
                }
                val results = FilterResults()
                results.values = filteredResults
                return results
            }
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                differ.submitList(results?.values as List<Note>)
            }
        }
    }
}

