package com.project.taima.adapter

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.project.taima.R
import com.project.taima.model.Customer
import java.text.DecimalFormat
import java.util.Collections

class RouteAdapter (private val customers: MutableList<Customer>) : RecyclerView.Adapter<RouteAdapter.RouteViewHolder>() {

    private val db = FirebaseFirestore.getInstance()
    private val customersRef = db.collection("Customer")

    inner class RouteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val route = itemView.findViewById<TextView>(R.id.routeCustomerRoute)!!
        val alias: TextView = itemView.findViewById(R.id.routeCustomerAlias)
        val balance: TextView = itemView.findViewById(R.id.routeCustomerBalance)
        val balanceCircle: ImageView = itemView.findViewById(R.id.balanceCircle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RouteViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_customer_route, parent, false)
        return RouteViewHolder(view)
    }

    @SuppressLint("SetTextI18n", "DefaultLocale")
    override fun onBindViewHolder(holder: RouteViewHolder, position: Int) {
        val customer = customers[position]
        holder.route.text = customer.route
        holder.alias.text = customer.alias

         if(customer.balance == 0.0) { holder.balance.text = "Saldo: 0.00 €"}
        else {holder.balance.text = "Saldo: ${DecimalFormat("#.00").format(customer.balance)} €"}

        if (customer.balance < 0) {
            holder.balanceCircle.setImageResource(R.drawable.circle_red_icon)
        } else {
            holder.balanceCircle.setImageResource(R.drawable.circle_green_icon)
        }

    }

    override fun getItemCount() = customers.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newCustomers: List<Customer>) {
        this.customers.clear()
        this.customers.addAll(newCustomers)
        Log.d("RouteAdapter", "Updated list size: ${customers.size}")
        notifyDataSetChanged()

    }

    fun swapItems(fromPosition: Int, toPosition: Int) {
        Collections.swap(customers, fromPosition, toPosition)
        notifyItemMoved(fromPosition, toPosition)

        updateCustomerPositions()
    }


    private fun updateCustomerPositions() {
        Log.d(
            "Firebase",
            "Entra al metodo"
        )
        for ((index, customer) in customers.withIndex()) {
            Log.d(
                "Firebase",
                "Entra al for"
            )
            val customerRef = customersRef.document(customer.id)
            customerRef.update("position", index)
                .addOnSuccessListener {
                    Log.d(
                        "Firebase",
                        "Position updated successfully for customer ${customer.position}"
                    )
                }
                .addOnFailureListener { e ->
                    Log.e("Firebase", "Error updating position", e)
                }
        }
    }
}