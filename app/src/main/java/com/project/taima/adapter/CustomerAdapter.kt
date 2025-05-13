package com.project.taima.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.project.taima.R
import com.project.taima.model.Customer

class CustomerAdapter(
    private val clientes: MutableList<Customer>,
    private val onEditClick: (Customer) -> Unit,
    private val onDelete: (Customer) -> Unit,
    private val onTransactionClick: (String) -> Unit
) : RecyclerView.Adapter<CustomerAdapter.CustomerViewHolder>() {

    private val expandedItems = mutableSetOf<Int>()

    inner class CustomerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val route: TextView = itemView.findViewById(R.id.customerRoute)
        private val alias: TextView = itemView.findViewById(R.id.customerAlias)
        private val actionButtons: LinearLayout = itemView.findViewById(R.id.actionButton)
        private val editButton: LinearLayout = itemView.findViewById(R.id.btnEdit)
        private val transactionButton: LinearLayout = itemView.findViewById(R.id.btnTransaction)
        private val deleteButton: LinearLayout = itemView.findViewById(R.id.btnDelete)

        fun bind(customer: Customer, isExpanded: Boolean) {
            route.text = customer.route
            alias.text = customer.alias

            actionButtons.visibility = if (isExpanded) View.VISIBLE else View.GONE

            itemView.setOnClickListener {
                if (expandedItems.contains(absoluteAdapterPosition)) {
                    expandedItems.remove(absoluteAdapterPosition)
                } else {
                    expandedItems.add(absoluteAdapterPosition)
                }
                notifyItemChanged(absoluteAdapterPosition)
            }

            editButton.setOnClickListener {
                onEditClick(customer)
            }

            transactionButton.setOnClickListener {
                onTransactionClick(customer.id)
            }

            deleteButton.setOnClickListener {
                onDelete(customer)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_customer, parent, false)
        return CustomerViewHolder(view)
    }

    override fun onBindViewHolder(holder: CustomerViewHolder, position: Int) {
        val customer = clientes[position]
        holder.bind(customer, expandedItems.contains(position))
    }

    override fun getItemCount() = clientes.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newCustomers: List<Customer>) {
        this.clientes.clear()
        this.clientes.addAll(newCustomers)
        notifyDataSetChanged()
    }
}
