package com.project.taima.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.google.firebase.firestore.FirebaseFirestore
import com.project.taima.R
import com.project.taima.model.Transaction
import java.text.SimpleDateFormat
import java.util.Locale

class SummaryAdapter(
    private val context: Context,
    private val transactions: List<Transaction>
) : ArrayAdapter<Transaction>(context, R.layout.item_transaction, transactions) {

    private val customerCache = mutableMapOf<String, String>() // Caché para los nombres de clientes
    private val db = FirebaseFirestore.getInstance() // Instancia de Firestore

    @SuppressLint("SetTextI18n")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_transaction, parent, false)
        val transaction = transactions[position]

        val typeAndDate = view.findViewById<TextView>(R.id.transactionTypeAndDate)
        val amount = view.findViewById<TextView>(R.id.transactionAmount)
        val arrowImg = view.findViewById<ImageView>(R.id.arrowImg)

        val formattedDate = SimpleDateFormat("dd/MM - HH:mm", Locale.getDefault()).format(
            transaction.date?.toDate()!!
        )

        typeAndDate.text = "Cargando... | $formattedDate"

        if (customerCache.containsKey(transaction.customerId)) {
            val customerName = customerCache[transaction.customerId]
            typeAndDate.text = "$customerName | $formattedDate"
        }
        else {
            db.collection("Customer")
                .document(transaction.customerId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val customerName = document.getString("route") ?: "Desconocido"
                        customerCache[transaction.customerId] = customerName
                        typeAndDate.text = "$customerName | $formattedDate"
                    } else {
                        typeAndDate.text = "Cliente no encontrado | $formattedDate"
                    }
                }
                .addOnFailureListener { exception ->
                    typeAndDate.text = "Error al cargar cliente | $formattedDate"
                    Log.e("SummaryAdapter_getCustomer", "Error: $exception")
                }
        }
        amount.text = "%.2f".format(transaction.amount)
        arrowImg.setImageResource(
            if (transaction.isIncome) R.drawable.green_arrow_icon else R.drawable.red_arrow_icon
        )

        return view
    }
}

