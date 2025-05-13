package com.project.taima

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.ListView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.project.taima.adapter.TransactionAdapter
import com.project.taima.model.Transaction
import java.util.Date
import kotlin.math.abs

class TransactionActivity : AppCompatActivity() {

    private lateinit var balanceText: TextView
    private lateinit var balanceCircle: ImageView
    private lateinit var transactionsList: ListView
    private lateinit var transactionAdapter: TransactionAdapter
    private lateinit var noTransactionsMessage: TextView
    private lateinit var db: FirebaseFirestore
    private val transactions: MutableList<Transaction> = mutableListOf()

    @SuppressLint("MissingInflatedId", "SetTextI18n", "DefaultLocale")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transaction)
        setupBackButton()
        val customerId = intent.getStringExtra("CUSTOMER_ID")

        db = FirebaseFirestore.getInstance()
        transactionAdapter = TransactionAdapter(this, transactions,
            onEditClicked = { transaction ->
            showEditTransactionDialog(transaction)
            },
            onDelete = { transaction ->
            deleteTransaction(transaction)
            }
        )

        balanceCircle = findViewById(R.id.balanceCircleTransaction)
        balanceText = findViewById(R.id.balanceTextTransction)
        transactionsList = findViewById(R.id.transactionsList)
        noTransactionsMessage = findViewById(R.id.noTransactionsMessage)
        transactionsList.adapter = transactionAdapter

        getInitialBalance(customerId!!)
        updateUI()
        findViewById<FloatingActionButton>(R.id.addTransactionButton).setOnClickListener {
            showAddTransactionDialog(customerId)
        }
    }

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    private fun updateUI() {
        if (transactions.isEmpty()) {
            transactionsList.visibility = View.GONE
            noTransactionsMessage.visibility = View.VISIBLE
        } else {
            transactionsList.visibility = View.VISIBLE
            noTransactionsMessage.visibility = View.GONE
        }
    }


    private fun setupBackButton() {
        findViewById<ImageView>(R.id.backIcon).setOnClickListener {
            val intent = Intent(this@TransactionActivity, CustomerActivity::class.java)
            startActivity(intent)
        }
    }

    private fun showAddTransactionDialog(customerId: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.form_transaction, null)
        val transactionAmount = dialogView.findViewById<EditText>(R.id.transactionAmount)
        val transactionTypeGroup = dialogView.findViewById<RadioGroup>(R.id.transactionTypeGroup)

        transactionAmount.addTextChangedListener(object : TextWatcher {
            @SuppressLint("SetTextI18n")
            override fun afterTextChanged(s: Editable?) {
                if (s.isNullOrEmpty()) return

                val inputText = s.toString()

                if (inputText.contains(".")) {
                    val parts = inputText.split(".")
                    val integerPart = parts[0]
                    val decimalPart = if (parts.size > 1) parts[1] else ""

                    if (integerPart.length > 7) {
                        transactionAmount.removeTextChangedListener(this)
                        transactionAmount.setText(integerPart.substring(0, 7) + "." + decimalPart)
                        transactionAmount.setSelection(transactionAmount.text.length)
                        transactionAmount.addTextChangedListener(this)
                    }

                    if (decimalPart.length > 2) {
                        transactionAmount.removeTextChangedListener(this)
                        transactionAmount.setText(integerPart + "." + decimalPart.substring(0, 2))
                        transactionAmount.setSelection(transactionAmount.text.length)
                        transactionAmount.addTextChangedListener(this)
                    }
                } else {
                    if (inputText.length > 7) {
                        transactionAmount.removeTextChangedListener(this)
                        transactionAmount.setText(inputText.substring(0, 7))
                        transactionAmount.setSelection(transactionAmount.text.length)
                        transactionAmount.addTextChangedListener(this)
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        AlertDialog.Builder(this)
            .setTitle("Nueva Transacción")
            .setView(dialogView)
            .setPositiveButton("Añadir") { _, _ ->
                val amount = transactionAmount.text.toString().toDoubleOrNull()
                val selectedTypeId = transactionTypeGroup.checkedRadioButtonId

                if (amount != null && selectedTypeId != -1) {
                    val isIncome = selectedTypeId == R.id.incomeRadio
                    val type = if (isIncome) "Cobrado" else "Fiado"

                    val date = Timestamp(Date())
                    addTransactionToFirestore(customerId, amount, isIncome)


                    transactions.add(Transaction(type = type, date = date, amount = amount, isIncome = isIncome))
                    transactionAdapter.notifyDataSetChanged()


                    getInitialBalance(customerId)
                } else {
                    Toast.makeText(this, "Por favor, completa todos los campos.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }


    private fun addTransactionToFirestore(customerId: String, amount: Double, isIncome: Boolean) {

        val fieldToUpdate = if (isIncome) "charged" else "deposit"
        val newDocRef = db.collection("Transactions").document()

        val transactionData = mapOf(
            "id" to newDocRef.id,
            "customerId" to customerId,
            fieldToUpdate to listOf(amount),
            "date" to FieldValue.serverTimestamp()
        )

        newDocRef.set(transactionData)
            .addOnSuccessListener {
                Toast.makeText(this, "Transacción añadida con éxito.", Toast.LENGTH_SHORT).show()
                addTransactionToBalance(customerId, amount, isIncome)
            }
            .addOnFailureListener { exception ->
                Log.w("TransactionActivity", "Error al añadir transacción.", exception)
                Toast.makeText(this, "Error al añadir transacción.", Toast.LENGTH_SHORT).show()
            }
    }

    @SuppressLint("SetTextI18n", "DefaultLocale")
    private fun addTransactionToBalance(customerId: String, amount: Double, isIncome: Boolean) {
        db.collection("Customer")
            .document(customerId)
            .get()
            .addOnSuccessListener { customerDocument ->
                if (customerDocument.exists()) {
                    val currentBalance = customerDocument.getDouble("balance") ?: 0.0

                    val adjustedBalance = if (isIncome) {
                        currentBalance + amount
                    } else {
                        currentBalance - amount
                    }

                    balanceText.text = "Saldo: ${String.format("%.2f", adjustedBalance)}€"
                    if (adjustedBalance >= 0) {
                        balanceCircle.setImageResource(R.drawable.circle_green_icon)
                    } else {
                        balanceCircle.setImageResource(R.drawable.circle_red_icon)
                    }

                    db.collection("Customer")
                        .document(customerId)
                        .update("balance", adjustedBalance)
                        .addOnSuccessListener {
                            Log.d("TransactionCheck", "Balance successfully updated to: $adjustedBalance")
                        }
                        .addOnFailureListener { exception ->
                            Log.w("TransactionActivity", "Error updating balance.", exception)
                        }
                } else {
                    Log.w("TransactionActivity", "Client document not found.")
                }
            }
            .addOnFailureListener { exception ->
                Log.w("TransactionActivity", "Error obtaining customer balance.", exception)
            }
    }

    private fun showEditTransactionDialog(transaction: Transaction) {

        val dialogView = LayoutInflater.from(this).inflate(R.layout.form_transaction, null)
        val transactionAmount = dialogView.findViewById<EditText>(R.id.transactionAmount)
        val transactionTypeGroup = dialogView.findViewById<RadioGroup>(R.id.transactionTypeGroup)
        val incomeRadio = dialogView.findViewById<RadioButton>(R.id.incomeRadio)
        val expenseRadio = dialogView.findViewById<RadioButton>(R.id.expenseRadio)


        transactionAmount.addTextChangedListener(object : TextWatcher {
            @SuppressLint("SetTextI18n")
            override fun afterTextChanged(s: Editable?) {
                if (s.isNullOrEmpty()) return

                val inputText = s.toString()

                if (inputText.contains(".")) {
                    val parts = inputText.split(".")
                    val integerPart = parts[0]
                    val decimalPart = if (parts.size > 1) parts[1] else ""

                    if (integerPart.length > 7) {
                        transactionAmount.removeTextChangedListener(this)
                        transactionAmount.setText(integerPart.substring(0, 7) + "." + decimalPart)
                        transactionAmount.setSelection(transactionAmount.text.length)
                        transactionAmount.addTextChangedListener(this)
                    }

                    if (decimalPart.length > 2) {
                        transactionAmount.removeTextChangedListener(this)
                        transactionAmount.setText(integerPart + "." + decimalPart.substring(0, 2))
                        transactionAmount.setSelection(transactionAmount.text.length)
                        transactionAmount.addTextChangedListener(this)
                    }
                } else {
                    if (inputText.length > 7) {
                        transactionAmount.removeTextChangedListener(this)
                        transactionAmount.setText(inputText.substring(0, 7))
                        transactionAmount.setSelection(transactionAmount.text.length)
                        transactionAmount.addTextChangedListener(this)
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        db.collection("Transactions")
            .document(transaction.id)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val amountField = when {
                        document.contains("charged") -> "charged"
                        document.contains("deposit") -> "deposit"
                        else -> null
                    }

                    if (amountField != null) {
                        val amountList = document.get(amountField) as? List<Double> ?: emptyList()
                        if (amountList.isNotEmpty()) {
                            transactionAmount.setText(amountList.first().toString())
                            if (amountField == "charged") {
                                incomeRadio.isChecked = true
                            } else {
                                expenseRadio.isChecked = true
                            }
                        }
                    }


                    AlertDialog.Builder(this)
                        .setTitle("Editar Transacción")
                        .setView(dialogView)
                        .setPositiveButton("Guardar") { _, _ ->
                            val newAmount = transactionAmount.text.toString().toDoubleOrNull()
                            val selectedTypeId = transactionTypeGroup.checkedRadioButtonId

                            if (newAmount != null && selectedTypeId != -1) {
                                val isIncome = selectedTypeId == R.id.incomeRadio

                                db.collection("Transactions").document(transaction.id)
                                    .get()
                                    .addOnSuccessListener { document ->
                                        if (document != null) {
                                            val previousAmount = if (document.contains("charged")) {
                                                (document.get("charged") as? List<Double>)?.firstOrNull()
                                                    ?: 0.0
                                            } else {
                                                -(document.get("deposit") as? List<Double>)?.firstOrNull()!!
                                            }

                                            val updatedAmount =
                                                if (isIncome) newAmount else -newAmount

                                            val operation = if (isIncome) "+" else "-"
                                            Log.d(
                                                "UpdateCheckIncome",
                                                "(A) ${if (isIncome) "Income" else "Expense"}: $operation$newAmount = $updatedAmount"
                                            )

                                            val difference = updatedAmount - previousAmount

                                            Log.d(
                                                "UpdateCheckDiff",
                                                "(D) updatedAmount = $updatedAmount, previousAmount = $previousAmount, difference = $difference"
                                            )


                                            val updates = if (isIncome) {
                                                mapOf(
                                                    "charged" to listOf(newAmount),
                                                    "deposit" to FieldValue.delete()
                                                )
                                            } else {
                                                mapOf(
                                                    "deposit" to listOf(abs(newAmount)),
                                                    "charged" to FieldValue.delete()
                                                )
                                            }

                                            db.collection("Transactions").document(transaction.id)
                                                .update(updates)
                                                .addOnSuccessListener {
                                                    Toast.makeText(
                                                        this,
                                                        "Transacción actualizada con éxito.",
                                                        Toast.LENGTH_SHORT
                                                    ).show()

                                                    updateCustomerBalance(transaction.customerId, difference, isIncome)
                                                    updateTransactionInList(transaction.id, abs(updatedAmount), isIncome)
                                                }
                                                .addOnFailureListener { exception ->
                                                    Toast.makeText(
                                                        this,
                                                        "Error al guardar cambios: ${exception.message}",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                        } else {
                                            Toast.makeText(
                                                this,
                                                "No se encontró la transacción.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                    .addOnFailureListener { exception ->
                                        Toast.makeText(
                                            this,
                                            "Error al obtener datos: ${exception.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                            } else {
                                Toast.makeText(
                                    this,
                                    "Por favor, completa todos los campos.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        .setNegativeButton("Cancelar", null)
                        .show()
                }
            }
    }


    private fun updateTransactionInList(transactionId: String, updatedAmount: Double, isIncome: Boolean) {
        val transactionIndex = transactions.indexOfFirst { it.id == transactionId }
        if (transactionIndex != -1) {
            val updatedTransaction = transactions[transactionIndex].copy(
                amount = updatedAmount,
                isIncome = isIncome,
                type = if (isIncome) "Cobrado" else "Fiado"
            )
            transactions[transactionIndex] = updatedTransaction
            transactionAdapter.notifyDataSetChanged()
        }
    }



    @SuppressLint("SetTextI18n", "DefaultLocale")
    private fun updateCustomerBalance(customerId: String, amount: Double, isIncome: Boolean) {
        db.collection("Customer")
            .document(customerId)
            .get()
            .addOnSuccessListener { customerDocument ->
                if (customerDocument.exists()) {
                    val currentBalance = customerDocument.getDouble("balance") ?: 0.0

                    val newBalance = if(isIncome){
                        if (amount<0) currentBalance - abs(amount)
                        else currentBalance + abs(amount)
                    }
                    else{
                        if (amount>0) currentBalance + abs(amount)
                        else currentBalance - abs(amount)
                    }

                    db.collection("Customer")
                        .document(customerId)
                        .update("balance", newBalance)
                        .addOnSuccessListener {
                            balanceText.text = "Saldo: ${String.format("%.2f", newBalance)}€"
                            if (newBalance >= 0) {
                                balanceCircle.setImageResource(R.drawable.circle_green_icon)
                            } else {
                                balanceCircle.setImageResource(R.drawable.circle_red_icon)
                            }
                            Log.d("BalanceUpdater", "Balance successfully updated: $newBalance.")
                        }
                        .addOnFailureListener { exception ->
                            Log.w("BalanceUpdater", "Error updating balance.", exception)
                        }
                }
            }
            .addOnFailureListener { exception ->
                Log.w("TransactionHandler", "Error obtaining customer balance.", exception)
            }
    }


    @SuppressLint("SetTextI18n", "DefaultLocale")
    private fun deleteTransaction(transaction: Transaction) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar transacción")
            .setMessage("¿Estás seguro de que deseas eliminar esta operación?")
            .setPositiveButton("Sí") { _, _ ->

                deleteTransactionFromBalance(transaction) { adjustedBalance ->

                    db.collection("Customer")
                        .document(transaction.customerId)
                        .update("balance", adjustedBalance)
                        .addOnSuccessListener {
                            Log.d("DeleteTransaction", "Updated balance after deleting transaction: $adjustedBalance")

                            db.collection("Transactions").document(transaction.id)
                                .delete()
                                .addOnSuccessListener {

                                    if (adjustedBalance >= 0.00) balanceCircle.setImageResource(R.drawable.circle_green_icon)
                                    else balanceCircle.setImageResource(R.drawable.circle_red_icon)
                                    balanceText.text = "Saldo: ${String.format("%.2f", adjustedBalance)}€"

                                    Toast.makeText(this, "Transacción eliminada correctamente.", Toast.LENGTH_SHORT).show()
                                    transactions.remove(transaction)
                                    updateUI()
                                    transactionAdapter.notifyDataSetChanged()
                                }
                                .addOnFailureListener {
                                    Toast.makeText(this, "Error deleting the transaction.", Toast.LENGTH_SHORT).show()
                                }
                        }
                        .addOnFailureListener { exception ->
                            Log.e("DeleteTransaction", "Error updating balance.", exception)
                        }
                }
            }
            .setNegativeButton("No", null)
            .show()
    }


    private fun deleteTransactionFromBalance(transaction: Transaction, callback: (Double) -> Unit) {
        db.collection("Customer")
            .document(transaction.customerId)
            .get()
            .addOnSuccessListener { customerDocument ->
                if (customerDocument.exists()) {
                    val currentBalance = customerDocument.getDouble("balance") ?: 0.0

                    Log.w("TypeCheck", transaction.type)
                    val adjustedBalance = if (transaction.type == "Cobrado") {
                        currentBalance - transaction.amount
                    } else {
                        currentBalance + transaction.amount

                    }
                    /*Log.w("TypeCheck","$currentBalance + ${transaction.amount} = ${currentBalance + transaction.amount}")
                    Log.w("TypeCheck","$currentBalance - ${transaction.amount} = ${currentBalance - transaction.amount}")
                    Log.w("TypeCheck", "$adjustedBalance")*/
                    callback(adjustedBalance)
                } else {
                    Log.w("DeleteTransactionFromBalance", "Error: Customer with ID: ${transaction.customerId} not found")
                }
            }
            .addOnFailureListener { exception ->
                Log.e("DeleteTransactionFromBalance", "Error obtaining customer balance.", exception)
            }
    }




    @SuppressLint("DefaultLocale", "SetTextI18n")
    private fun getInitialBalance(customerId: String) {
        db.collection("Customer")
            .document(customerId)
            .get()
            .addOnSuccessListener { customerDocument ->
                if (customerDocument.exists()) {
                    val initialBalance = customerDocument.getDouble("balance") ?: 0.0
                    Log.d("TransactionCheck", "Saldo al inicio: $initialBalance.")
                    balanceText.text = "Saldo: ${String.format("%.2f", initialBalance)}€"

                    if (initialBalance >= 0.00) balanceCircle.setImageResource(R.drawable.circle_green_icon)
                    else balanceCircle.setImageResource(R.drawable.circle_red_icon)
                    fetchTransactionsForCustomer(customerId)
                } else {
                    Log.d("TransactionActivity", "Customer not found.")
                }
            }
            .addOnFailureListener { exception ->
                Log.w("TransactionActivity", "Error in obtaining customer.", exception)
            }
    }




    @SuppressLint("SetTextI18n", "DefaultLocale")
    private fun fetchTransactionsForCustomer(customerId: String) {
        db.collection("Transactions")
            .whereEqualTo("customerId", customerId)
            .orderBy("date", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    transactions.clear()

                    querySnapshot.forEach { document ->
                        try {
                            val transactionId = document.getString("id") ?: document.id
                            val timestamp = document.getTimestamp("date") ?: Timestamp.now()

                            val deposit = document.get("deposit") as? List<Double> ?: emptyList()
                            val charged = document.get("charged") as? List<Double> ?: emptyList()


                            deposit.forEach { amount ->
                                transactions.add(
                                    Transaction(
                                        id = transactionId,
                                        type = "Fiado",
                                        date = timestamp,
                                        amount = amount,
                                        isIncome = false,
                                        customerId = customerId
                                    )
                                )
                            }
                            charged.forEach { amount ->
                                transactions.add(
                                    Transaction(
                                        id = transactionId,
                                        type = "Cobrado",
                                        date = timestamp,
                                        amount = amount,
                                        isIncome = true,
                                        customerId = customerId
                                    )
                                )
                            }

                        } catch (e: Exception) {
                            Log.e("Transactions", "Document mapping error: ${document.id}", e)
                        }
                    }
                    updateUI()
                    transactionAdapter.notifyDataSetChanged()


                    Log.d("Transactions", "Transactions loaded: ${transactions.size}")
                } else {
                    Log.d("Transactions", "No transactions found for customer $customerId")
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error al obtener transacciones: ${exception.message}", Toast.LENGTH_SHORT).show()
                Log.e("Transactions", "Error al obtener transacciones: ${exception.message}", exception)
            }
    }
}