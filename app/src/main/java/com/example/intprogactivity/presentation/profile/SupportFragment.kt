package com.example.intprogactivity.presentation.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.example.intprogactivity.presentation.theme.AppBackground
import com.example.intprogactivity.presentation.theme.BrandPrimary
import com.example.intprogactivity.presentation.theme.TripFlightsTheme
import com.example.intprogactivity.util.UiState
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class SupportFragment : Fragment() {

    private val viewModel: SupportViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                TripFlightsTheme {
                    SupportScreen(
                        viewModel = viewModel,
                        onBack = { findNavController().navigateUp() }
                    )
                }
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.loadTickets()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen(viewModel: SupportViewModel, onBack: () -> Unit) {
    val ticketsState  by viewModel.ticketsState.collectAsStateWithLifecycle()
    val submitState   by viewModel.submitState.collectAsStateWithLifecycle()
    val snackbar      = remember { SnackbarHostState() }
    var showDialog    by remember { mutableStateOf(false) }
    var expandedId    by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(submitState) {
        when (submitState) {
            is UiState.Success -> {
                snackbar.showSnackbar("Ticket submitted — we'll reply soon!")
                viewModel.resetSubmitState()
                showDialog = false
            }
            is UiState.Error -> {
                snackbar.showSnackbar((submitState as UiState.Error).message)
                viewModel.resetSubmitState()
            }
            else -> Unit
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Support Tickets") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                actions = {
                    IconButton(onClick = { showDialog = true }) {
                        Icon(Icons.Filled.Add, "New ticket", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BrandPrimary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(padding)) {

            when (val s = ticketsState) {
                is UiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is UiState.Error   -> Text(s.message, modifier = Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.error)
                is UiState.Success -> {
                    if (s.data.isEmpty()) {
                        EmptySupportState(onNewTicket = { showDialog = true })
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(s.data) { ticket ->
                                TicketCard(
                                    ticket    = ticket,
                                    expanded  = expandedId == ticket.id,
                                    onToggle  = { expandedId = if (expandedId == ticket.id) null else ticket.id }
                                )
                            }
                        }
                    }
                }
                else -> Unit
            }
        }
    }

    if (showDialog) {
        NewTicketDialog(
            isLoading = submitState is UiState.Loading,
            onDismiss = { showDialog = false },
            onSubmit  = { subject, message -> viewModel.submitTicket(subject, message) }
        )
    }
}

@Composable
private fun EmptySupportState(onNewTicket: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Filled.HelpOutline, null, modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        Text("No support tickets", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(8.dp))
        Text("Need help? Create a ticket and we'll get back to you.",
            fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(20.dp))
        Button(onClick = onNewTicket,
            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
            shape = RoundedCornerShape(12.dp)) {
            Text("New Ticket")
        }
    }
}

@Composable
private fun TicketCard(ticket: SupportTicket, expanded: Boolean, onToggle: () -> Unit) {
    val fmt = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    Card(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(ticket.subject, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                    modifier = Modifier.weight(1f))
                StatusChip(ticket.status)
            }
            Spacer(Modifier.height(4.dp))
            Text(fmt.format(Date(ticket.createdAt)),
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            if (expanded) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))

                // Original message
                MessageBubble(ticket.message, isStaff = false,
                    time = fmt.format(Date(ticket.createdAt)))

                // Replies
                ticket.replies.forEach { reply ->
                    Spacer(Modifier.height(8.dp))
                    MessageBubble(reply.message, isStaff = reply.isStaff,
                        time = fmt.format(Date(reply.createdAt)))
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: String, isStaff: Boolean, time: String) {
    val bgColor = if (isStaff) Color(0xFFEBF5FF) else Color(0xFFF3F4F6)
    val label   = if (isStaff) "Support Agent" else "You"
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(label, fontWeight = FontWeight.SemiBold, fontSize = 12.sp,
                color = if (isStaff) BrandPrimary else MaterialTheme.colorScheme.onSurface)
            Text(time, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(4.dp))
        Text(message, fontSize = 14.sp)
    }
}

@Composable
private fun StatusChip(status: String) {
    val (bg, fg) = if (status == "open")
        Color(0xFFDCFCE7) to Color(0xFF15803D)
    else
        Color(0xFFF3F4F6) to Color(0xFF6B7280)
    Surface(color = bg, shape = RoundedCornerShape(12.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (status == "closed")
                Icon(Icons.Filled.CheckCircle, null, tint = fg, modifier = Modifier.size(12.dp))
            Text(status.replaceFirstChar { it.uppercase() },
                fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = fg)
        }
    }
}

@Composable
private fun NewTicketDialog(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String, String) -> Unit
) {
    var subject by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
                Text("New Support Ticket", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Subject") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Describe your issue") },
                    minLines = 4,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss, enabled = !isLoading) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onSubmit(subject, message) },
                        enabled = subject.isNotBlank() && message.isNotBlank() && !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isLoading)
                            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                        else
                            Text("Submit")
                    }
                }
            }
        }
    }
}
