package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.Contact
import com.example.ui.theme.*
import com.example.viewmodel.ContactViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhonebookApp(
    viewModel: ContactViewModel,
    onExportCsv: () -> Unit,
    onImportCsv: () -> Unit
) {
    val context = LocalContext.current
    val contacts by viewModel.contacts.collectAsState()
    val currentIndex by viewModel.currentIndex.collectAsState()

    // Dialog state
    var showAddEditDialog by remember { mutableStateOf(false) }
    var showSearchDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    // Contact being edited (null for Add)
    var editingContact by remember { mutableStateOf<Contact?>(null) }

    val currentContact = contacts.getOrNull(currentIndex)

    // RTL Direction
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = if (isSystemInDarkTheme()) DarkNavy else LightBg,
            bottomBar = {
                // Bottom persistent action area (FAB-like bar)
                BottomActionBar(
                    onSearchClick = {
                        viewModel.clearSearchFilters()
                        showSearchDialog = true
                    },
                    onAddClick = {
                        editingContact = null
                        showAddEditDialog = true
                    },
                    onExportClick = onExportCsv
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Header Panel (Top Header mimicking the web app header)
                HeaderPanel(
                    currentContactIndex = if (contacts.isNotEmpty()) currentIndex else -1,
                    totalContacts = contacts.size,
                    onPrevClick = { viewModel.prevContact() },
                    onNextClick = { viewModel.nextContact() },
                    onEditClick = {
                        if (currentContact != null) {
                            editingContact = currentContact
                            showAddEditDialog = true
                        } else {
                            Toast.makeText(context, "مخاطبی برای ویرایش وجود ندارد", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onDeleteClick = {
                        if (currentContact != null) {
                            showDeleteConfirmDialog = true
                        } else {
                            Toast.makeText(context, "مخاطبی برای حذف وجود ندارد", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onSettingsClick = { showSettingsDialog = true }
                )

                // Main Slider Content
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(18.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    if (contacts.isEmpty()) {
                        // Empty State
                        EmptyStateView()
                    } else {
                        // Contact Card Display
                        if (currentContact != null) {
                            ContactCardView(
                                contact = currentContact,
                                onPhoneClick = { phone ->
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                    context.startActivity(intent)
                                }
                            )
                        }
                    }
                }
            }
        }

        // --- Overlays & Modals ---

        // Add/Edit Dialog
        if (showAddEditDialog) {
            AddEditContactDialog(
                contact = editingContact,
                viewModel = viewModel,
                onDismiss = { showAddEditDialog = false },
                onSave = { contact ->
                    if (contact.id == 0L) {
                        viewModel.insertContact(contact)
                        Toast.makeText(context, "مخاطب با موفقیت ثبت شد", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.updateContact(contact)
                        Toast.makeText(context, "مخاطب با موفقیت ویرایش شد", Toast.LENGTH_SHORT).show()
                    }
                    showAddEditDialog = false
                }
            )
        }

        // Search Dialog
        if (showSearchDialog) {
            SearchContactDialog(
                viewModel = viewModel,
                onDismiss = { showSearchDialog = false },
                onSelectContact = { contact ->
                    viewModel.jumpToContact(contact.id)
                    showSearchDialog = false
                }
            )
        }

        // Settings Dialog
        if (showSettingsDialog) {
            SettingsDialog(
                onDismiss = { showSettingsDialog = false },
                onImportClick = {
                    showSettingsDialog = false
                    onImportCsv()
                },
                onExportClick = {
                    showSettingsDialog = false
                    onExportCsv()
                },
                onResetClick = {
                    showSettingsDialog = false
                    showResetConfirmDialog = true
                },
                onAboutClick = {
                    showSettingsDialog = false
                    showAboutDialog = true
                }
            )
        }

        // About Dialog
        if (showAboutDialog) {
            AboutDialog(
                onDismiss = { showAboutDialog = false },
                onCallDeveloper = { phone ->
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                    context.startActivity(intent)
                }
            )
        }

        // Delete Confirmation Dialog
        if (showDeleteConfirmDialog && currentContact != null) {
            CustomAlertDialog(
                title = "حذف مخاطب",
                message = "آیا از حذف مخاطب «${currentContact.fullName}» مطمئن هستید؟ این عمل غیرقابل بازگشت است.",
                confirmText = "بله، حذف شود",
                cancelText = "انصراف",
                onConfirm = {
                    viewModel.deleteContact(currentContact)
                    Toast.makeText(context, "مخاطب حذف شد", Toast.LENGTH_SHORT).show()
                    showDeleteConfirmDialog = false
                },
                onDismiss = { showDeleteConfirmDialog = false }
            )
        }

        // Reset All Data Confirmation Dialog
        if (showResetConfirmDialog) {
            CustomAlertDialog(
                title = "هشدار! حذف تمام داده‌ها",
                message = "آیا از حذف همه مخاطبین اطمینان دارید؟ این عمل غیرقابل بازگشت است و تمام اطلاعات شما برای همیشه پاک خواهد شد.",
                confirmText = "بله، همه حذف شوند",
                cancelText = "انصراف",
                onConfirm = {
                    viewModel.resetAllData()
                    Toast.makeText(context, "تمام مخاطبین حذف شدند", Toast.LENGTH_SHORT).show()
                    showResetConfirmDialog = false
                },
                onDismiss = { showResetConfirmDialog = false },
                isWarning = true
            )
        }
    }
}

// ================== Component UI ==================

@Composable
fun HeaderPanel(
    currentContactIndex: Int,
    totalContacts: Int,
    onPrevClick: () -> Unit,
    onNextClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val gradient = Brush.horizontalGradient(
        colors = listOf(DarkNavy, RoyalBlue)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(gradient)
            .padding(16.dp)
    ) {
        // Top row: App Title and Record Indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "دفتر تلفن",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black
            )

            // Record indicator badge
            val indicatorText = if (currentContactIndex != -1) {
                "${currentContactIndex + 1}/$totalContacts"
            } else {
                "0/0"
            }

            Box(
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.15f), CircleShape)
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = indicatorText,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Bottom row: Nav Controls and Header Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Navigation: Prev and Next
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HeaderNavButton(text = "قبلی", onClick = onPrevClick)
                HeaderNavButton(text = "بعدی", onClick = onNextClick)
            }

            // Actions: Edit, Delete, Settings
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HeaderActionButton(
                    icon = Icons.Default.Edit,
                    text = "ویرایش",
                    color = Color(0xFF22C55E), // matching web green-16%
                    onClick = onEditClick
                )
                HeaderActionButton(
                    icon = Icons.Default.Delete,
                    text = "حذف",
                    color = Color(0xFFEF4444), // matching web red-16%
                    onClick = onDeleteClick
                )
                HeaderActionButton(
                    icon = Icons.Default.Settings,
                    text = "تنظیمات",
                    color = Color(0xFF3B82F6), // matching web settings
                    onClick = onSettingsClick
                )
            }
        }
    }
}

@Composable
fun HeaderNavButton(
    text: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White.copy(alpha = 0.14f),
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
        modifier = Modifier.height(40.dp)
    ) {
        Text(text = text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun HeaderActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    color: Color,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(40.dp)
            .background(color.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
            .testTag("action_${text}")
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun BottomActionBar(
    onSearchClick: () -> Unit,
    onAddClick: () -> Unit,
    onExportClick: () -> Unit
) {
    Surface(
        tonalElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onSearchClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrightBlue,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("search_button")
            ) {
                Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "🔍 جستجو", fontSize = 14.sp, fontWeight = FontWeight.Black)
            }

            Button(
                onClick = onAddClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrightBlue,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("register_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "➕ ثبت", fontSize = 14.sp, fontWeight = FontWeight.Black)
            }

            Button(
                onClick = onExportClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = GreenMain,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("excel_button")
            ) {
                Icon(Icons.Default.Download, contentDescription = "Excel", modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "⬇️ اکسل", fontSize = 14.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun EmptyStateView() {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.ContactPhone,
                contentDescription = null,
                tint = SlateGray.copy(alpha = 0.5f),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "مخاطبی یافت نشد",
                color = SlateGray,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "برای شروع، یک مخاطب جدید ثبت کنید",
                color = SlateGray.copy(alpha = 0.8f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ContactCardView(
    contact: Contact,
    onPhoneClick: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp)
        ) {
            // Field Grid representation
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Two items per row (or stacked depending on screen)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ContactFieldItem(
                        label = "تاریخ ثبت (شمسی)",
                        value = contact.persianDate.ifBlank { "—" },
                        modifier = Modifier.weight(1f)
                    )
                    ContactFieldItem(
                        label = "نام و نام خانوادگی",
                        value = contact.fullName,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ContactFieldItem(
                        label = "سمت",
                        value = contact.position.ifBlank { "—" },
                        modifier = Modifier.weight(1f)
                    )
                    ContactFieldItem(
                        label = "قسمت",
                        value = contact.department.ifBlank { "—" },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Description (Full width)
                ContactFieldItem(
                    label = "توضیحات",
                    value = contact.description.ifBlank { "—" },
                    modifier = Modifier.fillMaxWidth(),
                    isMultiLine = true
                )
            }

            // Phones Section
            if (contact.mobile.isNotBlank() || contact.officePhone.isNotBlank()) {
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (contact.mobile.isNotBlank()) {
                        PhoneChip(
                            label = "📱 ${contact.mobile}",
                            onClick = { onPhoneClick(contact.mobile) },
                            isOffice = false,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (contact.officePhone.isNotBlank()) {
                        PhoneChip(
                            label = "☎️ ${contact.officePhone}",
                            onClick = { onPhoneClick(contact.officePhone) },
                            isOffice = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ContactFieldItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    isMultiLine: Boolean = false
) {
    val boxBg = if (isSystemInDarkTheme()) Color(0xFF334155).copy(alpha = 0.4f) else Color(0xFFF8FAFC)
    val borderColor = if (isSystemInDarkTheme()) Color(0xFF475569) else Color(0xFFE2E8F0)

    Box(
        modifier = modifier
            .background(boxBg, RoundedCornerShape(18.dp))
            .border(1.dp, borderColor, RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .heightIn(min = if (isMultiLine) 120.dp else 90.dp)
    ) {
        Column {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = SlateGray
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isSystemInDarkTheme()) Color.White else TextDark,
                lineHeight = 22.sp
            )
        }
    }
}

@Composable
fun PhoneChip(
    label: String,
    onClick: () -> Unit,
    isOffice: Boolean,
    modifier: Modifier = Modifier
) {
    val bg = if (isOffice) {
        if (isSystemInDarkTheme()) Color(0xFF312E81) else Color(0xFFE0E7FF)
    } else {
        if (isSystemInDarkTheme()) Color(0xFF1E3A8A) else Color(0xFFDBEAFE)
    }

    val contentColor = if (isSystemInDarkTheme()) Color.White else Color(0xFF0F172A)
    val borderColor = if (isOffice) Color(0xFFC7D2FE) else Color(0xFFBFDBFE)

    Box(
        modifier = modifier
            .background(bg, RoundedCornerShape(999.dp))
            .border(1.dp, borderColor, RoundedCornerShape(999.dp))
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = contentColor,
            fontWeight = FontWeight.Black,
            fontSize = 15.sp
        )
    }
}

// ================== Modal Dialogs ==================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditContactDialog(
    contact: Contact?,
    viewModel: ContactViewModel,
    onDismiss: () -> Unit,
    onSave: (Contact) -> Unit
) {
    var fullName by remember { mutableStateOf(contact?.fullName ?: "") }
    var persianDate by remember { mutableStateOf(contact?.persianDate ?: viewModel.getCurrentPersianDate()) }
    var position by remember { mutableStateOf(contact?.position ?: "") }
    var department by remember { mutableStateOf(contact?.department ?: "") }
    var description by remember { mutableStateOf(contact?.description ?: "") }
    var mobile by remember { mutableStateOf(contact?.mobile ?: "") }
    var officePhone by remember { mutableStateOf(contact?.officePhone ?: "") }

    var fullNameError by remember { mutableStateOf(false) }
    var dateError by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.95f)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
            tonalElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Modal Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (contact == null) "افزودن مخاطب" else "ویرایش مخاطب",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Scrollable Form Fields
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Date & Name row
                    OutlinedTextField(
                        value = persianDate,
                        onValueChange = {
                            persianDate = it
                            dateError = !viewModel.isValidPersianDate(it)
                        },
                        label = { Text("تاریخ ثبت (شمسی)") },
                        isError = dateError,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        placeholder = { Text("مثال: ۱۴۰۵/۰۳/۱۱") }
                    )

                    OutlinedTextField(
                        value = fullName,
                        onValueChange = {
                            fullName = it
                            fullNameError = it.isBlank()
                        },
                        label = { Text("نام و نام خانوادگی") },
                        isError = fullNameError,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("اجباری") }
                    )

                    OutlinedTextField(
                        value = position,
                        onValueChange = { position = it },
                        label = { Text("سمت") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = department,
                        onValueChange = { department = it },
                        label = { Text("قسمت") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = mobile,
                        onValueChange = { mobile = it },
                        label = { Text("شماره تماس اول") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )

                    OutlinedTextField(
                        value = officePhone,
                        onValueChange = { officePhone = it },
                        label = { Text("شماره تماس دوم") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("توضیحات") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default)
                    )

                    Text(
                        text = "📅 تاریخ به صورت خودکار از سیستم گرفته می‌شود (فرمت: ۱۴۰۵/۰۳/۱۱)",
                        fontSize = 11.sp,
                        color = SlateGray,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Save button
                Button(
                    onClick = {
                        fullNameError = fullName.isBlank()
                        dateError = !viewModel.isValidPersianDate(persianDate)

                        if (!fullNameError && !dateError) {
                            val finalDate = if (persianDate.isBlank()) {
                                viewModel.getCurrentPersianDate()
                            } else {
                                viewModel.normalizePersianDate(persianDate)
                            }

                            val result = Contact(
                                id = contact?.id ?: 0L,
                                fullName = fullName.trim(),
                                position = position.trim(),
                                department = department.trim(),
                                description = description.trim(),
                                mobile = mobile.trim(),
                                officePhone = officePhone.trim(),
                                persianDate = finalDate
                            )
                            onSave(result)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrightBlue,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("save_contact_button")
                ) {
                    Text(text = "💾 ذخیره مخاطب", fontSize = 15.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchContactDialog(
    viewModel: ContactViewModel,
    onDismiss: () -> Unit,
    onSelectContact: (Contact) -> Unit
) {
    val sDate by viewModel.sDate.collectAsState()
    val sDepartment by viewModel.sDepartment.collectAsState()
    val sPosition by viewModel.sPosition.collectAsState()
    val sAdvancedSearch by viewModel.sAdvancedSearch.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val uniquePositions by viewModel.uniquePositions.collectAsState()

    var showDropdown by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.95f)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(MaterialTheme.colorScheme.surface),
            tonalElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "جستجو", fontSize = 18.sp, fontWeight = FontWeight.Black)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Search form
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = sDate,
                            onValueChange = { viewModel.updateSDate(it) },
                            label = { Text("تاریخ (مثال: ۱۴۰۳/۱/۵)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = sDepartment,
                            onValueChange = { viewModel.updateSDepartment(it) },
                            label = { Text("قسمت") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    // Custom Dropdown for Positions
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = if (sPosition.isEmpty()) "همه سمتها" else sPosition,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("سمت") },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(onClick = { showDropdown = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                }
                            }
                        )
                        DropdownMenu(
                            expanded = showDropdown,
                            onDismissRequest = { showDropdown = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            DropdownMenuItem(
                                text = { Text("همه سمتها") },
                                onClick = {
                                    viewModel.updateSPosition("")
                                    showDropdown = false
                                }
                            )
                            uniquePositions.forEach { pos ->
                                DropdownMenuItem(
                                    text = { Text(pos) },
                                    onClick = {
                                        viewModel.updateSPosition(pos)
                                        showDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = sAdvancedSearch,
                        onValueChange = { viewModel.updateSAdvancedSearch(it) },
                        label = { Text("جستجوی پیشرفته...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Button(
                        onClick = { viewModel.performSearch() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrightBlue,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(text = "🔎 یافتن موارد مشابه", fontSize = 15.sp, fontWeight = FontWeight.Black)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable Search Results
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (searchResults.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "موردی یافت نشد",
                                    color = SlateGray,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        items(searchResults) { r ->
                            SearchResultItem(
                                contact = r,
                                onSelect = { onSelectContact(r) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchResultItem(
    contact: Contact,
    onSelect: () -> Unit
) {
    val boxBg = if (isSystemInDarkTheme()) Color(0xFF334155).copy(alpha = 0.4f) else Color(0xFFF8FAFC)
    val borderColor = if (isSystemInDarkTheme()) Color(0xFF475569) else Color(0xFFE2E8F0)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(boxBg, RoundedCornerShape(16.dp))
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contact.fullName,
                fontWeight = FontWeight.Black,
                fontSize = 16.sp,
                color = if (isSystemInDarkTheme()) Color.White else TextDark
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${contact.department.ifBlank { "—" }} | ${contact.position.ifBlank { "—" }} | 📅 ${contact.persianDate.ifBlank { "بدون تاریخ" }}",
                fontSize = 13.sp,
                color = SlateGray
            )
            if (contact.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "📝 ${if (contact.description.length > 50) contact.description.take(50) + "..." else contact.description}",
                    fontSize = 12.sp,
                    color = SlateGray.copy(alpha = 0.9f)
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Button(
            onClick = onSelect,
            colors = ButtonDefaults.buttonColors(
                containerColor = BrightBlue,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(999.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text(text = "نمایش", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SettingsDialog(
    onDismiss: () -> Unit,
    onImportClick: () -> Unit,
    onExportClick: () -> Unit,
    onResetClick: () -> Unit,
    onAboutClick: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "تنظیمات", fontSize = 18.sp, fontWeight = FontWeight.Black)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider()

                // Excel Import/Export
                SettingsSection(title = "اکسل (آفلاین)") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onImportClick,
                            colors = ButtonDefaults.buttonColors(containerColor = BrightBlue),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "وارد کردن از اکسل", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = onExportClick,
                            colors = ButtonDefaults.buttonColors(containerColor = GreenMain),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "خروجی اکسل", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Data management
                SettingsSection(title = "مدیریت داده‌ها") {
                    Button(
                        onClick = onResetClick,
                        colors = ButtonDefaults.buttonColors(containerColor = OrangeMain),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "⚠️ ریست کلی داده‌ها", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Info Section
                SettingsSection(title = "اطلاعات") {
                    Button(
                        onClick = onAboutClick,
                        colors = ButtonDefaults.buttonColors(containerColor = SlateGray),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "ℹ️ درباره برنامه", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    val boxBg = if (isSystemInDarkTheme()) Color(0xFF334155).copy(alpha = 0.2f) else Color(0xFFF8FAFC)
    val borderColor = if (isSystemInDarkTheme()) Color(0xFF475569) else Color(0xFFE5E7EB)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(boxBg, RoundedCornerShape(18.dp))
            .border(1.dp, borderColor, RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            color = if (isSystemInDarkTheme()) Color.White else TextDark
        )
        Spacer(modifier = Modifier.height(10.dp))
        content()
    }
}

@Composable
fun AboutDialog(
    onDismiss: () -> Unit,
    onCallDeveloper: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "درباره برنامه", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(text = "👨‍💻", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(10.dp))
                Text(text = "مهدی حسین پور", fontSize = 18.sp, fontWeight = FontWeight.Black)
                Text(text = "طراحی و اجرا", fontSize = 14.sp, color = SlateGray)

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { onCallDeveloper("09213544345") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSystemInDarkTheme()) Color(0xFF312E81) else Color(0xFFF1F5F9),
                        contentColor = BrightBlue
                    ),
                    shape = RoundedCornerShape(999.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "0921-354-4345",
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "نسخه 3.0 - آفلاین کامل + تاریخ شمسی",
                    fontSize = 11.sp,
                    color = SlateGray
                )
            }
        }
    }
}

@Composable
fun CustomAlertDialog(
    title: String,
    message: String,
    confirmText: String,
    cancelText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isWarning: Boolean = false
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isWarning) "⚠️" else "❓",
                    fontSize = 48.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isWarning) RedMain else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 12.dp),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = message,
                    fontSize = 14.sp,
                    color = SlateGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 24.dp),
                    lineHeight = 22.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSystemInDarkTheme()) Color(0xFF334155) else Color(0xFFE5E7EB),
                            contentColor = if (isSystemInDarkTheme()) Color.White else TextDark
                        ),
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Text(text = cancelText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isWarning) RedMain else BrightBlue,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Text(text = confirmText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
