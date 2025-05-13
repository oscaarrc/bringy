package com.project.taima

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import com.project.taima.adapter.CustomerAdapter
import com.project.taima.model.Customer
import java.util.Locale

class CustomerActivity : AppCompatActivity() {

    private lateinit var customerAdapter: CustomerAdapter
    private val customers = mutableListOf<Customer>()
    private val db = FirebaseFirestore.getInstance()
    private lateinit var btnDay: Button
    private var selectedDay: String = "Todos"
    private val originalCustomers = mutableListOf<Customer>()


    @SuppressLint("MissingInflatedId", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_customer)

        setupRecyclerView()
        fetchCustomersFromFirestore()
        setupAddCustomerButton()
        setupSearchFunctionality()
        setupBackButton()
        setupDayFilterButton()

    }
    @SuppressLint("SourceLockedOrientationActivity")
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }



    @SuppressLint("SetTextI18n")
    private fun setupDayFilterButton(){
        btnDay = findViewById(R.id.btnDay)
        btnDay.text = "Todos"
        btnDay.setOnClickListener {
            showDayFilterDialog()
        }
    }

    private fun showDayFilterDialog() {
        val daysOfWeek = arrayOf(
            "Sin filtro", "Lunes", "Martes", "Miércoles",
            "Jueves", "Viernes", "Sábado", "Domingo"
        )

        AlertDialog.Builder(this)
            .setTitle("Selecciona un día")
            .setItems(daysOfWeek) { _, which ->
                selectedDay = daysOfWeek[which]
                btnDay.text = if (selectedDay == "Sin filtro") "Todos" else selectedDay
                fetchCustomersFromFirestore()
            }
            .show()
    }

    private fun setupRecyclerView() {
        val customerRecyclerView = findViewById<RecyclerView>(R.id.customerRecyclerView)

        customerAdapter = CustomerAdapter(customers,
            onEditClick = { customer ->
                showEditDialog(customer)
            },
            onDelete = { customer ->
                deleteCustomer(customer)
            },
            onTransactionClick = { customerId ->
                redirectToTransactionView(customerId)
            }
        )
        customerRecyclerView.layoutManager = LinearLayoutManager(this)
        customerRecyclerView.adapter = customerAdapter
    }

    private fun setupAddCustomerButton() {
        findViewById<FloatingActionButton>(R.id.addClient).setOnClickListener {
            showAddCustomerDialog()
        }
    }

    private fun redirectToTransactionView(customerId: String) {
        val intent = Intent(this, TransactionActivity::class.java)
        intent.putExtra("CUSTOMER_ID", customerId)
        startActivity(intent)
    }

    private fun setupSearchFunctionality() {
        val searchEditText = findViewById<EditText>(R.id.searchEditText)
        searchEditText.textLocale = Locale("es", "ES")
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val searchText = s.toString().lowercase(Locale.getDefault())
                val filteredCustomers = if (searchText.isEmpty()) {
                    originalCustomers
                } else {
                    filterCustomers(searchText)
                }
                customerAdapter.updateData(filteredCustomers)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filterCustomers(searchText: String): List<Customer> {
        return originalCustomers.filter { customer ->
            customer.alias.lowercase(Locale.getDefault()).contains(searchText) ||
                    customer.route.lowercase(Locale.getDefault()).contains(searchText)
        }
    }

    private fun setupBackButton() {
        val backBtn = findViewById<ImageView>(R.id.backIcon)
        backBtn.setOnClickListener {
            val intent = Intent(this@CustomerActivity, HomeActivity::class.java)
            startActivity(intent)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun fetchCustomersFromFirestore() {
        customers.clear()
        originalCustomers.clear()

        val filterDay = if (selectedDay == "Todos") "Sin filtro" else selectedDay

        db.collection("Customer")
            .get()
            .addOnSuccessListener { result ->
                result.forEach { document ->
                    val customer = document.toObject<Customer>()
                    customer.id = document.id
                    customers.add(customer)
                    originalCustomers.add(customer)
                }


                if (filterDay != "Sin filtro") {
                    val filteredList = customers.filter { customer ->
                        customer.routeDay.contains(filterDay)
                    }

                    customers.clear()
                    customers.addAll(filteredList)

                    originalCustomers.clear()
                    originalCustomers.addAll(filteredList)
                }

                customerAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Log.w("CustomerActivity", "Error al obtener documentos.", exception)
            }
    }


    @SuppressLint("InflateParams")
    private fun showAddCustomerDialog() {
        Dialog(this).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            val dialogView = LayoutInflater.from(this@CustomerActivity).inflate(R.layout.add_form_customer, null)
            setContentView(dialogView)
            window?.setLayout((resources.displayMetrics.widthPixels * 0.9).toInt(), WindowManager.LayoutParams.WRAP_CONTENT)

            setupDialogViews(dialogView, this)
            show()
        }
    }

    private fun showEditDialog(customer: Customer) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.edit_form_customer, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        val routeText = dialogView.findViewById<EditText>(R.id.routeTextEdit)
        val aliasText = dialogView.findViewById<EditText>(R.id.aliasTextEdit)
        val balanceText = dialogView.findViewById<EditText>(R.id.balanceTextEdit)

        balanceText.addTextChangedListener(object : TextWatcher {
            @SuppressLint("SetTextI18n")
            override fun afterTextChanged(s: Editable?) {
                if (s.isNullOrEmpty()) return

                val inputText = s.toString()

                if (inputText.contains(".")) {
                    val parts = inputText.split(".")
                    val integerPart = parts[0]
                    val decimalPart = if (parts.size > 1) parts[1] else ""

                    if (integerPart.length > 7) {
                        balanceText.removeTextChangedListener(this)
                        balanceText.setText(integerPart.substring(0, 7) + "." + decimalPart)
                        balanceText.setSelection(balanceText.text.length)
                        balanceText.addTextChangedListener(this)
                    }

                    if (decimalPart.length > 2) {
                        balanceText.removeTextChangedListener(this)
                        balanceText.setText(integerPart + "." + decimalPart.substring(0, 2))
                        balanceText.setSelection(balanceText.text.length)
                        balanceText.addTextChangedListener(this)
                    }
                } else {
                    if (inputText.length > 7) {
                        balanceText.removeTextChangedListener(this)
                        balanceText.setText(inputText.substring(0, 7))
                        balanceText.setSelection(balanceText.text.length)
                        balanceText.addTextChangedListener(this)
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        routeText.setText(customer.route)
        aliasText.setText(customer.alias)
        balanceText.setText(customer.balance.toString())

        val selectedDays = mutableListOf<String>()
        setupDayButtons(dialogView, selectedDays, customer.routeDay)

        val saveButton = dialogView.findViewById<Button>(R.id.btnAddEdit)


        saveButton.setOnClickListener {
            val updatedRoute = routeText.text.toString()
            val updatedAlias = aliasText.text.toString()
            val updatedBalance = balanceText.text.toString().toDoubleOrNull() ?: 0.0

            if (updatedRoute == customer.route) {
                updateCustomerInFirestore(
                    customer.id,
                    updatedRoute,
                    updatedAlias,
                    updatedBalance,
                    selectedDays
                )
                dialog.dismiss()
            } else {
                checkCustomerRoute(updatedRoute) { routeExists ->
                    if (!routeExists) {
                        updateCustomerInFirestore(
                            customer.id,
                            updatedRoute,
                            updatedAlias,
                            updatedBalance,
                            selectedDays
                        )
                        dialog.dismiss()
                    }
                }
            }

        }

        val cancelIconEdit = dialogView.findViewById<ImageView>(R.id.cancelIconEdit)
        cancelIconEdit.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }


    private fun updateCustomerInFirestore(customerId: String, route: String, alias: String, balance: Double, routeDay: List<String>) {
        if (route.isEmpty()) {
            Toast.makeText(this, "Error: La ruta no puede estar vacía.", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("Customer")
            .whereEqualTo("route", route)
            .get()
            .addOnSuccessListener {
                val customerRef = db.collection("Customer").document(customerId)

                customerRef.update(
                    "route", route,
                    "alias", alias,
                    "balance", balance,
                    "routeDay", routeDay
                )
                    .addOnSuccessListener {
                        Toast.makeText(this, "Cliente actualizado", Toast.LENGTH_SHORT).show()
                        fetchCustomersFromFirestore()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error al actualizar cliente: ${e.message}", Toast.LENGTH_SHORT).show()
                    }

            }

            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al comprobar ruta: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


    private fun setupDialogViews(dialogView: android.view.View, dialog: Dialog) {
        val routeText = dialogView.findViewById<EditText>(R.id.routeText)
        val aliasText = dialogView.findViewById<EditText>(R.id.aliasText)
        val balanceText = dialogView.findViewById<EditText>(R.id.balanceText)


        balanceText.addTextChangedListener(object : TextWatcher {
            @SuppressLint("SetTextI18n")
            override fun afterTextChanged(s: Editable?) {
                if (s.isNullOrEmpty()) return

                val inputText = s.toString()

                if (inputText.contains(".")) {
                    val parts = inputText.split(".")
                    val integerPart = parts[0]
                    val decimalPart = if (parts.size > 1) parts[1] else ""

                    if (integerPart.length > 7) {
                        balanceText.removeTextChangedListener(this)
                        balanceText.setText(integerPart.substring(0, 7) + "." + decimalPart)
                        balanceText.setSelection(balanceText.text.length)
                        balanceText.addTextChangedListener(this)
                    }

                    if (decimalPart.length > 2) {
                        balanceText.removeTextChangedListener(this)
                        balanceText.setText(integerPart + "." + decimalPart.substring(0, 2))
                        balanceText.setSelection(balanceText.text.length)
                        balanceText.addTextChangedListener(this)
                    }
                } else {
                    if (inputText.length > 7) {
                        balanceText.removeTextChangedListener(this)
                        balanceText.setText(inputText.substring(0, 7))
                        balanceText.setSelection(balanceText.text.length)
                        balanceText.addTextChangedListener(this)
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        val selectedDays = mutableListOf<String>()
        setupDayButtons(dialogView, selectedDays)

        val cancelIcon = dialogView.findViewById<ImageView>(R.id.cancelIcon)
        cancelIcon.setOnClickListener {
            dialog.dismiss()
        }

        val btnAdd = dialogView.findViewById<Button>(R.id.btnAdd)
        btnAdd.setOnClickListener {
            val route = routeText.text.toString()
            val alias = aliasText.text.toString()
            val balance = balanceText.text.toString().toDoubleOrNull() ?: 0.0

            if (route.isNotEmpty()) {
                checkCustomerRoute(route) { routeExists ->
                    if (!routeExists) {

                        val newCustomer = Customer(
                            route = route,
                            alias = alias,
                            balance = balance,
                            routeDay = selectedDays,
                            position = emptyMap<String, Int>().toMutableMap())

                        addCustomerToFirestore(newCustomer)
                        dialog.dismiss()
                    }
                }

            } else {
                Toast.makeText(this, "Por favor, ingresa una ruta.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupDayButtons(dialogView: android.view.View, selectedDays: MutableList<String>, routeDay: List<String>? = null) {
        val daysButtons = mapOf(
            "Lunes" to dialogView.findViewById<Button>(R.id.btnMonday),
            "Martes" to dialogView.findViewById(R.id.btnTuesday),
            "Miércoles" to dialogView.findViewById(R.id.btnWednesday),
            "Jueves" to dialogView.findViewById(R.id.btnThursday),
            "Viernes" to dialogView.findViewById(R.id.btnFriday),
            "Sábado" to dialogView.findViewById(R.id.btnSaturday),
            "Domingo" to dialogView.findViewById(R.id.btnSunday)
        )

        routeDay?.forEach { day ->
            daysButtons[day]?.apply {
                isSelected = true
                selectedDays.add(day)
            }
        }

        daysButtons.forEach { (day, button) ->
            button.setOnClickListener {
                if (selectedDays.contains(day)) {
                    selectedDays.remove(day)
                    button.isSelected = false
                } else {
                    selectedDays.add(day)
                    button.isSelected = true
                }
            }
        }
    }


    private fun addCustomerToFirestore(customer: Customer) {
        db.collection("Customer")
            .whereEqualTo("route", customer.route)
            .get()
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {
                    Toast.makeText(this, "Error: La ruta ya existe.", Toast.LENGTH_SHORT).show()
                } else {
                    db.collection("Customer")
                        .add(customer)
                        .addOnSuccessListener { documentReference ->
                            Toast.makeText(this, "Cliente añadido correctamente", Toast.LENGTH_SHORT).show()
                            redirectToTransactionView(documentReference.id)
                            fetchCustomersFromFirestore()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Error al agregar cliente: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al comprobar la ruta: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun checkCustomerRoute(route: String, callback: (Boolean) -> Unit) {
        db.collection("Customer")
            .whereEqualTo("route", route)
            .get()
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {
                    Toast.makeText(this, "Error: La ruta ya existe.", Toast.LENGTH_SHORT).show()
                    callback(true)
                } else {
                    callback(false)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al verificar la ruta.", Toast.LENGTH_SHORT).show()
                callback(true)
            }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun deleteCustomer(customer: Customer) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar cliente")
            .setMessage("¿Estás seguro de que deseas eliminar al cliente ${customer.route} y todas sus transacciones?")
            .setPositiveButton("Sí") { _, _ ->
                deleteAllTransactionsForCustomer(customer.id) {
                    db.collection("Customer").document(customer.id)
                        .delete()
                        .addOnSuccessListener {
                            Toast.makeText(this, "Cliente eliminado correctamente.", Toast.LENGTH_SHORT).show()
                            customers.remove(customer)
                            customerAdapter.notifyDataSetChanged()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Error al eliminar el cliente.", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun deleteAllTransactionsForCustomer(customerId: String, onComplete: () -> Unit) {
        db.collection("Transactions")
            .whereEqualTo("customerId", customerId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val batch = db.batch()

                for (document in querySnapshot.documents) {
                    batch.delete(document.reference)
                }

                batch.commit()
                    .addOnSuccessListener {
                        Log.d("DeleteAllTransactions", "Todas las transacciones del cliente $customerId fueron eliminadas correctamente.")
                        onComplete()
                    }
                    .addOnFailureListener { exception ->
                        Log.e("DeleteAllTransactions", "Error al eliminar las transacciones del cliente $customerId.", exception)
                    }
            }
            .addOnFailureListener { exception ->
                Log.e("DeleteAllTransactions", "Error al obtener las transacciones del cliente $customerId.", exception)
            }
    }


}