package com.sing.tgthird.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.sing.tgthird.core.model.AuthState
import com.sing.tgthird.core.model.LoginCredentials
import com.sing.tgthird.core.repository.TelegramRepository
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    repository: TelegramRepository,
    authState: AuthState,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var apiId by remember { mutableStateOf("") }
    var apiHash by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(8.dp),
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("TG", color = MaterialTheme.colorScheme.onPrimary)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("TG Third", style = MaterialTheme.typography.titleLarge)
                        Text(authLabel(authState), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                when (authState) {
                    AuthState.SignedOut,
                    is AuthState.Error -> {
                        OutlinedTextField(
                            value = apiId,
                            onValueChange = { apiId = it },
                            label = { Text("API ID") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = apiHash,
                            onValueChange = { apiHash = it },
                            label = { Text("API hash") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Phone") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        ActionButton(
                            text = "Login",
                            enabled = apiId.toIntOrNull() != null && apiHash.isNotBlank() && phone.isNotBlank(),
                            loading = false,
                            onClick = {
                                scope.launch {
                                    repository.startLogin(
                                        LoginCredentials(
                                            apiId = apiId.toInt(),
                                            apiHash = apiHash.trim(),
                                            phoneNumber = phone.trim()
                                        )
                                    )
                                }
                            }
                        )
                    }

                    is AuthState.WaitingForCode -> {
                        OutlinedTextField(
                            value = code,
                            onValueChange = { code = it },
                            label = { Text("Code") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        ActionButton(
                            text = "Continue",
                            enabled = code.isNotBlank(),
                            loading = false,
                            onClick = {
                                scope.launch {
                                    repository.submitCode(code.trim())
                                }
                            }
                        )
                    }

                    is AuthState.WaitingForPassword -> {
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        ActionButton(
                            text = "Continue",
                            enabled = password.isNotBlank(),
                            loading = false,
                            onClick = {
                                scope.launch {
                                    repository.submitPassword(password)
                                }
                            }
                        )
                    }

                    AuthState.Connecting -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp))
                            Text("Connecting")
                        }
                    }

                    is AuthState.Ready -> Unit
                }

                if (authState is AuthState.Error) {
                    Text(authState.message, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun ActionButton(
    text: String,
    enabled: Boolean,
    loading: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp,
                modifier = Modifier.size(18.dp)
            )
        } else {
            Text(text)
        }
    }
}

private fun authLabel(authState: AuthState): String =
    when (authState) {
        AuthState.SignedOut -> "Signed out"
        AuthState.Connecting -> "Connecting"
        is AuthState.WaitingForCode -> "Code required"
        is AuthState.WaitingForPassword -> "Password required"
        is AuthState.Ready -> authState.user.displayName
        is AuthState.Error -> "Error"
    }
