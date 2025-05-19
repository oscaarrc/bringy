# Introducción

>  ¡Bienvenido a Bringy, la aplicación Android que facilita la gestión de envíos
 para autónomos y pequeños emprendedores!
 Con Bringy podrás:
>
>• Administrar clientes de forma sencilla: crea, edita o elimina fichas en 
segundos y consulta el historial de operaciones para conocer al detalle 
quién ha pagado y quién mantiene saldos pendientes.
>
> • Planificar tu ruta diaria: visualiza de un vistazo todos los puntos de 
entrega del día y organiza tu jornada de manera óptima.
> • Consultar tu estado financiero: obtén informes automáticos de ingresos 
y gastos con desglose diario, semanal, mensual y anual, y mantén siempre 
el control de tu tesorería.
> • Importar y exportar datos: genera hojas de cálculo Excel con la 
información de tus clientes y sus saldos, o integra en la app tus propios 
listados para trabajar con datos unificados.
Optimiza tu trabajo diario, centraliza la información de tus envíos y lleva al día 
tu contabilidad con Bringy.

# Documentación:

## Activities

En este proyecto de Android, cada pantalla principal se implementa como una **Activity** que actúa como controlador de su vista y coordina la lógica de navegación y sesión. A continuación se describen todas las Activities fundamentales de la app:

------

### 1. `LoginActivity`

**Ruta:** `com.project.taima.LoginActivity`

**Responsabilidad principal:**

- Gestionar la pantalla de inicio de sesión basada únicamente en DNI.
- Comprobar sesión activa y redirigir automáticamente a `HomeActivity` si la sesión aún es válida (30 días).
- Validar la existencia del DNI en Firestore y, en caso afirmativo, autenticar de forma anónima con Firebase Auth.

**Flujo de trabajo:**

1. **Inicialización**
   - Se invoca `FirebaseAuth.getInstance()` para preparar la autenticación.
   - Se asocian los campos de UI (`EditText` para DNI, `Button` de login).
2. **Chequeo de sesión activa**
   - Lee de `SharedPreferences` el token de sesión y la fecha de login.
   - Si existe token y no han pasado más de 30 días, lanza `HomeActivity`.
3. **Acción de “Login”**
   - Al pulsar el botón, extrae el DNI del `EditText`.
   - Llama a `checkDniExists(dni)`, que consulta Firestore en la colección `"User"` filtrando por campo `dni`.
     - Si existe al menos un documento, procede a `signInAnonymously()`.
     - Si no, muestra `Toast("Usuario no encontrado")`.
4. **Autenticación anónima y guardado de sesión**
   - `signInAnonymously()` invoca `auth.signInAnonymously()`.
   - Al éxito, genera un token único (timestamp) y actualiza el documento del usuario en Firestore con `sessionToken`.
   - Almacena en `SharedPreferences`:
     - `sessionToken`
     - `sessionDate` (fecha de login en milisegundos).
   - Navega a `HomeActivity` y cierra `LoginActivity`.

**Gestión de orientación:**

- Sobrescribe `onConfigurationChanged` para forzar modo retrato si el dispositivo gira a apaisado.

------



### 2. `HomeActivity`

**Ruta:** `com.project.taima.HomeActivity`

**Responsabilidad principal:**

- Punto de entrada tras iniciar sesión.
- Mostrar secciones principales de la app (Clientes, Ruta, Resumen, Herramientas).
- Gestionar cierre de sesión.

**Flujo de trabajo:**

1. **Inicialización**
   - Obtiene instancia de `FirebaseAuth`.
   - Vincula vistas de sección (`LinearLayout` y `TextView` para “Logout”).
2. **Navegación interna**
   - Cada sección (`sectionCustomer`, `sectionRoute`, `sectionSummary`, `sectionTools`) tiene un `OnClickListener` que lanza la Activity correspondiente:
     - `CustomerActivity`
     - `RouteActivity`
     - `SummaryActivity`
     - `ToolsActivity`
3. **Cierre de sesión**
   - Al pulsar en el `TextView` de logout:
     1. Llama a `auth.signOut()`.
     2. Elimina `sessionToken` y `sessionDate` de `SharedPreferences`.
     3. Inicia `LoginActivity` con flags `NEW_TASK | CLEAR_TASK` para limpiar el back stack.
     4. Cierra `HomeActivity`.
     5. Loggea en consola: `"Usuario deslogueado correctamente"`.

**Gestión de orientación:**

- Igual que en `LoginActivity`, fuerza retrato en `onConfigurationChanged`.





### 3. `CustomerActivity`

**Ruta:** `com.project.taima.CustomerActivity`

**Responsabilidad principal:**

- Mostrar y gestionar la lista de clientes almacenados en Firestore.
- Permitir filtrar, buscar, añadir, editar y eliminar clientes.
- Navegar a la vista de transacciones de cada cliente.

------

#### Flujo de trabajo y componentes

1. **Inicialización (onCreate)**
   - Se infla `activity_customer.xml`.
   - Se invocan métodos auxiliares para:
     - Configurar el `RecyclerView` con un `CustomerAdapter`.
     - Cargar clientes desde Firestore.
     - Preparar el botón flotante de “Añadir cliente”.
     - Habilitar búsqueda por alias o ruta.
     - Configurar botón “Atrás” a `HomeActivity`.
     - Configurar filtro por día de ruta.
2. **Gestión de orientación**
   - En `onConfigurationChanged`, fuerza siempre modo retrato si el dispositivo gira.
3. **RecyclerView**
   - **CustomerAdapter** recibe:
     - `onEditClick(customer)`: abre diálogo de edición.
     - `onDelete(customer)`: muestra alerta de confirmación y borra cliente y sus transacciones.
     - `onTransactionClick(customerId)`: lanza `TransactionActivity`, pasando `CUSTOMER_ID`.
4. **Carga y filtrado de datos**
   - **fetchCustomersFromFirestore()**:
     1. Limpia listas `customers` y `originalCustomers`.
     2. Obtiene todos los documentos de la colección `"Customer"`.
     3. Convierte cada documento a `Customer` (usando `toObject<Customer>()`), asigna `id` y añade a ambas listas.
     4. Si hay un día seleccionado distinto de “Todos”, filtra `customers` por `routeDay`.
     5. Notifica al adaptador para refrescar la UI.
   - **Filtro de día**:
     - Botón `btnDay` abre un diálogo con los días de la semana.
     - Al seleccionar un día, actualiza `selectedDay` y recarga datos.
   - **Búsqueda**:
     - `EditText` de búsqueda escucha cambios de texto.
     - Filtra `originalCustomers` por `alias` o `route` conteniendo el texto ingresado (minúsculas).
5. **Añadir cliente**
   - **showAddCustomerDialog()** despliega un diálogo personalizado (`add_form_customer.xml`):
     - Campos: `route`, `alias`, `balance`, selección de días.
     - Valida que `route` no esté vacío y no exista en Firestore (`checkCustomerRoute`).
     - Crea objeto `Customer` y lo inserta en `"Customer"`.
     - Al éxito: muestra toast, redirige a `TransactionActivity` con el nuevo `customerId` y recarga la lista.
6. **Editar cliente**
   - **showEditDialog(customer)** usa un `AlertDialog` con layout `edit_form_customer.xml`:
     - Carga valores actuales en `EditText` y botones de día.
     - Valida cambios:
       - Si la ruta no cambia o no existe otra igual, actualiza Firestore con nuevos valores (`updateCustomerInFirestore`).
     - Al éxito: toast de confirmación y recarga la lista.
7. **Eliminar cliente**
   - **deleteCustomer(customer)** muestra confirmación.
   - Si confirma:
     - Llama a `deleteAllTransactionsForCustomer(customer.id)`, que elimina en batch todas las transacciones relacionadas (“Transactions” con campo `customerId`).
     - Luego borra el documento del cliente en `"Customer"`.
     - Actualiza la lista en memoria y notifica al adaptador.
8. **Navegación**
   - Icono “Atrás” (`backIcon`) vuelve a `HomeActivity`.
   - Click en un cliente (o su botón transacción) inicia `TransactionActivity`, pasando el `customerId` correspondiente.



### 4. `RouteActivity`

**Ruta:** `com.project.taima.RouteActivity`

**Responsabilidad principal:**

- Mostrar la lista diaria de clientes asignados a la ruta del día actual.
- Permitir reordenar la ruta arrastrando y soltando (drag & drop) y persistir la nueva posición en Firestore.
- Facilitar búsqueda rápida por alias o ruta.
- Navegar de vuelta a la pantalla principal.

------

#### Flujo de trabajo y componentes

1. **Inicialización (onCreate)**
   - Se infla `activity_route.xml`.
   - Se configuran componentes principales:
     - `RecyclerView` con un `RouteAdapter` ligado a la lista `customers`.
     - Carga inicial de datos mediante `fetchCustomersFromFirestore()`.
     - Un `ItemTouchHelper` para detectar gestos de arrastre y soltar.
     - Barra de búsqueda para filtrar la lista.
     - Ícono “Atrás” que vuelve a `HomeActivity`.
2. **Gestión de orientación**
   - Igual que en otras Activities, fuerza modo retrato en `onConfigurationChanged`.
3. **Obtención de la ruta diaria**
   - El método `dailyRoute()` utiliza un `SimpleDateFormat("EEEE", Locale("es","ES"))` para obtener el día de la semana en español (p. ej. “Lunes”), formateado con mayúscula inicial.
   - Este valor se usa para filtrar qué clientes aparecen en la ruta.
4. **Carga y actualización de la lista**
   - **fetchCustomersFromFirestore()**:
     1. Calcula `routeDay` con el nombre del día.
     2. Solicita todos los documentos de `"Customer"` ordenados por el campo `position` (pero este campo contiene un map de día→posición).
     3. En el callback de éxito, delega a `updateCustomerList(result, routeDay)`.
   - **updateCustomerList(result, routeDay)**:
     1. Limpia las listas `customers` y `originalCustomers`.
     2. Itera cada documento:
        - Convierte a `Customer`, asigna `id`.
        - Si el cliente tiene asignado el día actual en su lista `routeDay`, lo añade a las listas.
        - Si no tenía posición asignada para ese día, le da la siguiente posición disponible (al final).
     3. Ordena `customers` según `position[routeDay]`.
     4. Llama a `routeAdapter.updateData(...)` para refrescar la UI.
5. **Reordenación de la ruta**
   - Se crea un `ItemTouchHelper.SimpleCallback` que escucha movimientos UP/DOWN.
   - En `onMove`:
     1. Se intercambian los elementos en la lista con `routeAdapter.swapItems()`.
     2. Se invoca `dailyRoute()` para determinar el día actual.
     3. Se llama a `updatePositionsForDay(currentDay)`.
   - **updatePositionsForDay(currentDay)**:
     - Formatea `currentDay` con mayúscula inicial (“Lunes”).
     - Recorre cada cliente en la lista actualizada y asigna en su mapa `position[day] = índice`.
     - Actualiza en Firestore el campo `"position"` de cada documento.
     - Loggea el éxito o error de cada actualización.
6. **Búsqueda en tiempo real**
   - `EditText` de búsqueda añade un `TextWatcher`.
   - En `onTextChanged`, invoca `filterCustomers(text)`.
   - **filterCustomers(searchText)**:
     - Si el texto está vacío, restaura `originalCustomers`.
     - Si no, filtra `originalCustomers` por coincidencia parcial en `alias` o `route`.
     - Llama a `routeAdapter.updateData(filteredList)` para mostrar sólo resultados que cumplan.
7. **Navegación**
   - El icono `backIcon` tiene un `OnClickListener` que inicia `HomeActivity` y retorna al usuario a la pantalla principal.



### 5. `SummaryActivity`

**Ruta:** `com.project.taima.com.project.taima.SummaryActivity`

> **Nota:** El paquete indica `com.project.taima.com.project.taima`, probablemente un duplicado accidental; convendría corregirlo a `com.project.taima`.

**Responsabilidad principal:**

- Mostrar al usuario un resumen de transacciones (cobradas y fiadas) en distintos intervalos: diario, semanal, mensual o anual.
- Listar las transacciones paginadas de Firestore y calcular totales según el filtro seleccionado.
- Permitir cargar más transacciones bajo demanda.
- Navegar de vuelta a la pantalla principal.

------

#### Flujo de trabajo y componentes

1. **Inicialización (onCreate)**
   - Se infla `activity_summary.xml`.
   - Configura:
     - Botón “Atrás” que vuelve a `HomeActivity`.
     - Botón de filtro de resumen (`btnDailySummary`) iniciando en **“Resumen diario”**.
     - Vistas de texto para totales (`chargedText`, `depositText`).
     - Lista (`ListView`) con un `SummaryAdapter` para renderizar cada `Transaction`.
     - Botón flotante `loadMoreButton` para paginar.
   - Resetea el estado interno: lista `transactions`, `lastLoadedTransaction`, `totalCharged` y `totalDeposit`.
   - Llama a:
     1. `getDateRange("Resumen diario")` → obtiene `startDate` y `endDate`.
     2. `totalChargedAndDepositCalc(...)` → calcula y muestra los totales iniciales.
     3. `loadTransactions()` → carga las primeras 20 transacciones del periodo diario.
2. **Gestión de orientación**
   - En `onConfigurationChanged`, al igual que las demás Activities, fuerza el modo retrato ante giros.
3. **Selección de tipo de resumen**
   - Al pulsar `btnDailySummary`, muestra un diálogo con opciones:
     - **Resumen diario**
     - **Resumen semanal**
     - **Resumen mensual**
     - **Resumen anual**
   - Al elegir:
     1. Actualiza `selectedSummaryType` y el texto del botón.
     2. Resetea la lista y los acumuladores.
     3. Obtiene nuevo rango de fechas via `getDateRange()`.
     4. Recalcula totales con `totalChargedAndDepositCalc()`.
     5. Vuelve a llamar a `loadTransactions()`.
4. **Cálculo de rango de fechas**
   - `getDateRange(summaryType: String): Pair<Date,Date>`:
     - Inicializa un `Calendar` al comienzo del día (00:00).
     - Según `summaryType`, ajusta `startDate` y `endDate`:
       - **Diario:** hoy → mañana
       - **Semanal:** primer día de la semana → +1 semana
       - **Mensual:** día 1 del mes → +1 mes
       - **Anual:** día 1 del año → +1 año
     - Retorna `(startDate, endDate)`.
5. **Cálculo de totales**
   - `totalChargedAndDepositCalc(startDate, endDate, onResult)`:
     1. Consulta Firestore sobre `"Transactions"` con `date ≥ startDate` y `< endDate`.
     2. Itera cada documento:
        - Extrae `charged` y `deposit` (listas numéricas), se queda con el primer elemento o 0.0.
        - Suma por separado a `totalCharged` o `totalDeposit`.
     3. Invoca el callback `onResult(totalCharged, totalDeposit)` para actualizar los TextViews.
6. **Carga y paginación de transacciones**
   - `loadTransactions()`:
     1. Recupera el mismo rango de fechas que para totales.
     2. Construye la query:
        - Filtra por fecha y orden descendente.
        - Limita a 20 documentos.
        - Si hay un `lastLoadedTransaction`, llama a `.startAfter(it)`.
     3. Al recibir `result`:
        - Si está vacío, oculta el botón “Cargar más” y retorna.
        - Para cada documento:
          - Extrae ID, fecha (`Timestamp`), `customerId`.
          - Obtiene valores `chargedValue` y `depositValue`.
          - Crea un objeto `Transaction(id, date, amount, isIncome, customerId)` y lo añade a la lista `transactions`.
        - Actualiza `lastLoadedTransaction` con el último documento.
        - Decide visibilidad de “Cargar más”: visible si llegaron 20 items, oculta en caso contrario.
        - Muestra la lista y oculta el mensaje de “sin transacciones”.
        - Llama a `adapter.notifyDataSetChanged()`.
7. **Navegación**
   - El icono `backIconSummary` inicia `HomeActivity` al pulsarlo.

------

**Dependencias clave:**

- **FirebaseFirestore:** colección `"Transactions"`.
- **SummaryAdapter:** adaptador de `ListView` para mostrar transacciones individuales.
- **AlertDialog:** selección de tipo de resumen.
- **Timestamp / Date / Calendar:** manejo de fechas y rangos de tiempo.



### 6. `TransactionActivity`

**Ruta:** `com.project.taima.TransactionActivity`

**Responsabilidad principal:**

- Mostrar y gestionar las transacciones (cobros y fiados) de un cliente específico.
- Permitir añadir, editar y eliminar transacciones, actualizando en tiempo real el balance del cliente tanto en la UI como en Firestore.
- Navegar de vuelta a la lista de clientes.

------

#### Flujo de trabajo y componentes

1. **Inicialización (onCreate)**
   - Se infla `activity_transaction.xml`.
   - Se configura el botón “Atrás” para volver a `CustomerActivity`.
   - Se obtiene `customerId` del `Intent`.
   - Se inicializa Firestore (`db = FirebaseFirestore.getInstance()`).
   - Se crea un `TransactionAdapter` para el `ListView` de transacciones, con callbacks para editar y eliminar transacciones.
   - Se vinculan vistas de UI:
     - `balanceCircle` (indicador visual del balance: verde si ≥ 0, rojo si < 0).
     - `balanceText` (texto con “Saldo: X,XX €”).
     - `transactionsList` (lista de transacciones).
     - `noTransactionsMessage` (mensaje cuando no hay transacciones).
   - Se llama a:
     1. `getInitialBalance(customerId)` → carga el balance inicial y pinta la UI.
     2. `fetchTransactionsForCustomer(customerId)` → obtiene todas las transacciones del cliente y las muestra.
   - Se asocia el FAB “Añadir transacción” al método `showAddTransactionDialog(customerId)`.
2. **Gestión de orientación**
   - En `onConfigurationChanged`, al igual que en otras Activities, se fuerza el modo retrato para evitar layouts rotados.
3. **Mostrar u ocultar la lista**
   - `updateUI()` verifica si la lista `transactions` está vacía:
     - Si vacía: oculta `transactionsList`, muestra `noTransactionsMessage`.
     - Si no: al revés.
4. **Añadir transacción**
   - **showAddTransactionDialog(customerId)**:
     1. Infla el layout `form_transaction.xml`, con un `EditText` para el importe y un `RadioGroup` para “Cobrado” o “Fiado”.
     2. Aplica un `TextWatcher` al `EditText` para limitar formato (hasta 7 dígitos enteros y 2 decimales).
     3. Al pulsar “Añadir” en el diálogo:
        - Valida importe y tipo de transacción.
        - Llama a `addTransactionToFirestore(customerId, amount, isIncome)`.
        - Añade la transacción a la lista local y refresca el adaptador.
        - Actualiza el balance llamando a `getInitialBalance(customerId)`.
   - **addTransactionToFirestore(customerId, amount, isIncome)**:
     1. Crea un nuevo documento en la colección `"Transactions"` con campos:
        - `id`, `customerId`, `charged` o `deposit` (lista con el importe), `date` (timestamp del servidor).
     2. Al éxito, muestra un Toast y llama a `addTransactionToBalance(customerId, amount, isIncome)`.
   - **addTransactionToBalance(customerId, amount, isIncome)**:
     1. Recupera el documento del cliente en `"Customer"`.
     2. Calcula `adjustedBalance` sumando o restando el importe.
     3. Actualiza el texto y el color del círculo (`green` si ≥ 0, `red` si < 0).
     4. Escribe el nuevo balance en Firestore.
5. **Editar transacción**
   - **Callback onEditClicked(transaction)** dispara `showEditTransactionDialog(transaction)`:
     1. Infla `form_transaction.xml` y precarga importe y tipo (según si el documento contenía `charged` o `deposit`).
     2. Aplica validaciones de formato al `EditText`.
     3. Al pulsar “Guardar”:
        - Recupera el importe anterior del documento.
        - Calcula la diferencia `difference = updatedAmount - previousAmount` (positivo o negativo según tipo).
        - Prepara un `Map` de actualizaciones: asigna el campo `charged` o `deposit` y borra el otro con `FieldValue.delete()`.
        - Llama a `update(...)` en el documento de la transacción.
        - Al éxito:
          - Muestra Toast de confirmación.
          - Llama a `updateCustomerBalance(transaction.customerId, difference, isIncome)` para ajustar balance.
          - Llama a `updateTransactionInList(...)` para actualizar el objeto en memoria y refrescar la lista.
   - **updateCustomerBalance(customerId, amount, isIncome)**:
     1. Obtiene el balance actual del cliente.
     2. Ajusta según la diferencia calculada (sumar o restar).
     3. Escribe el nuevo balance en Firestore y actualiza la UI (`balanceText`, `balanceCircle`).
6. **Eliminar transacción**
   - **Callback onDelete(transaction)** dispara `deleteTransaction(transaction)`:
     1. Muestra un `AlertDialog` de confirmación.
     2. Si confirma:
        - Llama a `deleteTransactionFromBalance(transaction)`, que obtiene el balance actual y calcula el `adjustedBalance` invirtiendo la operación original.
        - Al obtener `adjustedBalance`, actualiza el balance del cliente en Firestore.
        - Luego borra el documento de la transacción.
        - Al éxito:
          - Cambia el color del círculo según el nuevo balance.
          - Actualiza `balanceText`.
          - Elimina la transacción de la lista local y refresca la UI y adaptador.
7. **Carga inicial de transacciones**
   - **fetchTransactionsForCustomer(customerId)**:
     1. Consulta `"Transactions"` filtrando por `customerId` y orden descendente por `date`.
     2. Por cada documento, extrae listas `deposit` y `charged`.
     3. Instancia uno o varios objetos `Transaction` según los importes y los añade a la lista.
     4. Llama a `updateUI()` y `adapter.notifyDataSetChanged()`.

------

**Modelos implicados:**

- `Transaction(id: String, type: String, date: Timestamp, amount: Double, isIncome: Boolean, customerId: String)`

**Dependencias clave:**

- **FirebaseFirestore**: colecciones `"Customer"` y `"Transactions"`.
- **TransactionAdapter**: adaptador de `ListView` con capacidades de edición y eliminación.
- **AlertDialog & LayoutInflater**: formularios de añadir/editar.
- **FieldValue.serverTimestamp() / FieldValue.delete()**: para marcar timestamps y eliminar campos en Firestore.



### 7. `ToolsActivity`

**Ruta:** `com.project.taima.ToolsActivity`

**Responsabilidad principal:**

- Proporcionar herramientas de importación y exportación de datos entre Firestore y archivos Excel (.xlsx).
- Mostrar indicadores de progreso y mensajes al usuario durante las operaciones.

------

#### Flujo de trabajo y componentes

1. **Inicialización (onCreate)**
   - Se infla `activity_tools.xml`.
   - Se vinculan vistas:
     - `progressBar` (barra de progreso).
     - `loadingText` (mensaje de estado).
     - `loadingLayout` (contenedor que agrupa ambos).
   - Se registran callbacks:
     - **Exportar:** botón en `exportSection` abre un `AlertDialog` para confirmar, luego llama a `performExport()`.
     - **Importar:** botón en `importSection` lanza el sistema de selección de fichero `.xlsx` mediante `ActivityResultContracts.GetContent`.
   - Se configura el botón “Atrás” (`backIconToools`) para volver a `HomeActivity`.
2. **Exportación a Excel**
   - **showExportDialog()**:
     - Muestra diálogo “¿Deseas exportar los datos a un archivo Excel?”.
     - Si se acepta, oculta el diálogo y llama a `performExport()`, si se cancela, muestra un toast de “Operación cancelada”.
   - **performExport()**:
     - Llama a `showLoading(true)` para mostrar la barra de progreso.
     - Invoca `exportFirestoreToExcel { success, filePath -> … }` desde `utils/exportFirestoreToExcel`.
     - Al completarse:
       - Oculta el loading (`showLoading(false)`).
       - Si `success` y existe `filePath`, muestra toast “Archivo guardado en la carpeta Descargas.”, en caso contrario, “Error al exportar los datos.”
3. **Importación desde Excel**
   - **Selección de fichero:**
     - `openFilePicker()` lanza el selector de archivos filtrando tipo MIME `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`.
   - **Callback getContent:**
     - Si el usuario selecciona un URI válido, llama a `importExcelAndUpdateFirestore(uri)`, en caso contrario, toast “No se seleccionó ningún archivo.”
   - **importExcelAndUpdateFirestore(uri: Uri)**:
     1. Muestra el loading.
     2. Crea un archivo temporal copiando el contenido del URI (`createTempFileFromUri`).
     3. Llama a `importExcelToFirestore(tempFilePath, progressBar, loadingText) { success, message -> … }`.
     4. Al completarse, oculta el loading y muestra un toast con “Datos importados con éxito.” o el mensaje de error recibido.
     5. Captura excepciones de I/O y muestra mensajes apropiados.
4. **Control de carga**
   - **showLoading(isLoading: Boolean)**:
     - Si `isLoading == true`, hace visible `loadingLayout`; si no, lo oculta.
   - **showToast(message: String)**:
     - Muestra un `Toast` largo con el texto proporcionado.

------

### 8. Utilidades de Excel (`excelOperations.kt`)

Este fichero agrupa dos funciones clave en el paquete `com.project.taima.utils`:

#### a) `exportFirestoreToExcel(callback: (Boolean, String?) -> Unit)`

- **Objetivo:** Generar un libro de Excel (.xlsx) con los datos de la colección `"Customer"`, organizados por hoja para cada día de la semana.
- **Pasos principales:**
  1. **Crear y configurar el XSSFWorkbook:**
     - Una hoja para cada día: “Lunes”, “Martes”, …, “Domingo”.
     - Estilo de moneda `#,##0.00€` aplicado a la columna de saldo.
     - Fila de encabezado con columnas “CALLE”, “ALIAS”, “SALDO”.
  2. **Leer clientes de Firestore:**
     - Recupera todos los documentos de `"Customer"`.
     - Para cada cliente extrae `route`, `alias`, `balance` y lista `routeDay`.
     - Añade una fila a la hoja correspondiente para cada día en `routeDay`.
  3. **Escribir el archivo en Descargas:**
     - Obtiene la carpeta `Environment.DIRECTORY_DOWNLOADS`.
     - Genera un nombre único `Datos_Taisma.xlsx`, añadiendo sufijo `(1)`, `(2)`,… si ya existe.
     - Guarda el workbook con `FileOutputStream`.
  4. **Invocar callback:**
     - `callback(true, filePath)` en éxito, o `callback(false, null)` en error.

#### b) `importExcelToFirestore(filePath: String, progressBar: ProgressBar, loadingText: TextView, callback: (Boolean, String?) -> Unit)`

- **Objetivo:** Leer un archivo Excel y sincronizar los datos en Firestore, reemplazando completamente las colecciones `"Transactions"` y `"Customer"`.
- **Pasos principales:**
  1. **Mostrar loading.**
  2. **Vaciar Firestore:**
     - Borra todas las transacciones en batch.
     - Luego borra todos los clientes en batch.
  3. **Leer y parsear el Excel (addCustomersFromExcel):**
     - Usa Apache POI (`WorkbookFactory.create`).
     - Construye un mapa `route → Customer` combinando días de ruta (hoja) para cada fila.
     - Cada `Customer` inicializa `position` vacío (para rutas).
     - Inserta todos los clientes en Firestore y espera a que todas las tareas terminen.
  4. **Ocultar loading y callback:**
     - `callback(true, "Importación completada con éxito.")` o con mensaje de error apropiado.
  5. **Gestión de errores:**
     - Captura `InvalidFormatException` y excepciones generales mostrando logs y devolviendo mensajes descriptivos.



## Adapters

Los **Adapters** en Android sirven de puente entre los datos (Modelos) y las vistas de lista (RecyclerView), encargándose de inflar el layout de cada ítem y de vincular los valores de los objetos con los componentes de la UI.

- **CustomerAdapter**
  - **Propósito:** Mostrar la lista de `Customer` en pantalla con funcionalidad de expandir ítems para revelar acciones (editar, ver transacciones, eliminar).
  - **Aspectos clave:**
    - Mantiene un conjunto `expandedItems` para controlar qué ítems están desplegados.
    - Recibe lambdas `onEditClick`, `onDelete` y `onTransactionClick` para delegar las acciones desde la Activity.
    - Implementa `updateData()` para refrescar toda la lista.
- **RouteAdapter**
  - **Propósito:** Mostrar la ruta diaria (lista de `Customer`) en orden, con su saldo y un indicador de color (verde/rojo) según balance.
  - **Aspectos clave:**
    - Permite reordenar elementos mediante `swapItems()`, actualizando además la posición en Firestore.
    - Formatea el saldo a dos decimales y ajusta el icono de `balanceCircle` según sea positivo o negativo.
    - Implementa `updateData()` para cargar nuevos datos y `notifyItemMoved()` en drag & drop.
- **SummaryAdapter**
  - **Tipo:** `ArrayAdapter<Transaction>` para `ListView`.
  - **Propósito:** Mostrar un listado de transacciones resumidas con fecha y nombre de cliente.
  - **Detalles clave:**
    - Usa un **cache** (`customerCache`) para evitar múltiples lecturas de Firestore al resolver rutas por `customerId`.
    - Formatea la fecha con `SimpleDateFormat("dd/MM - HH:mm")`.
    - Muestra flechas verdes o rojas según `isIncome`.
    - Infla `R.layout.item_transaction` y actualiza su contenido en `getView()`.
- **TransactionAdapter**
  - **Tipo:** `BaseAdapter` para `ListView`.
  - **Propósito:** Listar todas las transacciones de un cliente con acciones de editar/eliminar.
  - **Detalles clave:**
    - Formatea fecha como `dd/MM/yyyy HH:mm` y muestra el tipo (“Cobrado” o “Fiado”).
    - Ajusta la visibilidad de botones de acción (`btnEditCircle`, `btnDeleteCircle`) al pulsar el ítem.
    - Muestra importe con dos decimales y color de flecha según ingreso o gasto.
    - Ejecuta las lambdas `onEditClicked` y `onDelete` al pulsar los iconos correspondientes.



## Models

En esta capa, los **Modelos** son simples `data class` que reflejan las entidades de Firestore y facilitan la conversión automática de documentos a objetos Kotlin. Cada modelo define solo los campos necesarios para las operaciones CRUD y la UI.

- **Customer**
  Representa un cliente/ruta en la colección `"Customer"`, con propiedades como `id`, `route`, `alias`, días de servicio (`routeDay`), `balance`, posiciones por día (`position`) y un flag `isExpanded` para el estado en la lista.
- **Transaction**
  Mapea cada documento de `"Transactions"` con campos `id`, `customerId`, tipo (`type` / `isIncome`), `amount` y marca temporal `date`, permitiendo ordenar, mostrar y calcular balances de forma directa.



## Resources

En el directorio **res/** se organizan los recursos de la app de la siguiente forma:

- **anim/**: dos animaciones usadas en el splash screen al iniciar la aplicación.
- **drawable/**: imágenes y *shapes* personalizadas (íconos, fondos, botones).
- **font/**: las tipografías (fuentes) empleadas en toda la interfaz.
- **values/**: archivos XML con parámetros reutilizables (cadenas de texto, colores, estilos, dimensiones, arrays, etc.).
- **layout/**: los diseños (XML) de todas las pantallas y componentes de la app.



## Autoría

Este proyecto fue desarrollado por [@oscaarrc](https://github.com/oscaarrc)

## Licencia

Proyecto elaborado con fines educativos como TFG de 2º CFGS de Desarrollo de Aplicaciones Multiplataforma en el IES Ana Luisa Benítez.
