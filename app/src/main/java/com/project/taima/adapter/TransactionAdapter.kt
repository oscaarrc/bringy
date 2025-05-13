package com.project.taima.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.project.taima.R
import com.project.taima.model.Transaction
import java.text.SimpleDateFormat
import java.util.Locale

class TransactionAdapter(
    private val context: Context,
    private val transactions: MutableList<Transaction>,
    private val onEditClicked: (Transaction) -> Unit,
    private val onDelete: (Transaction) -> Unit,
) : BaseAdapter() {

    override fun getCount(): Int = transactions.size

    override fun getItem(position: Int): Any = transactions[position]

    override fun getItemId(position: Int): Long = position.toLong()


    @SuppressLint("SetTextI18n", "DefaultLocale")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_transaction, parent, false)

        val transaction = transactions[position]
        val typeAndDate = view.findViewById<TextView>(R.id.transactionTypeAndDate)
        val amount = view.findViewById<TextView>(R.id.transactionAmount)
        val arrowImg = view.findViewById<ImageView>(R.id.arrowImg)
        val actionButtons = view.findViewById<View>(R.id.circleActionButtons)
        val btnEdit = view.findViewById<View>(R.id.btnEditCircle)
        val btnDelete = view.findViewById<View>(R.id.btnDeleteCircle)


        if (transaction.date != null) {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val formattedDate = dateFormat.format(transaction.date.toDate())
            typeAndDate.text = "${transaction.type}: $formattedDate"
        } else {
            typeAndDate.text = "Fecha no disponible"
        }


        if (transaction.amount != 0.0) {
            amount.text = "${String.format("%.2f", transaction.amount)}€"
        } else {
            amount.text = "0.00€"
        }

        val arrowResource = if (transaction.isIncome) R.drawable.green_arrow_icon else R.drawable.red_arrow_icon
        arrowImg.setImageResource(arrowResource)
        amount.gravity = Gravity.END

        view.setOnClickListener {
            actionButtons.visibility = if (actionButtons.visibility == View.GONE) View.VISIBLE else View.GONE
        }

        btnEdit.setOnClickListener {
            onEditClicked(transaction)
        }
        btnDelete.setOnClickListener {
            onDelete(transaction)
        }

        return view
    }
}
