package pl.podkal.domowniczeqqq.register

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import pl.podkal.domowniczeq.R
import pl.podkal.domowniczeqqq.login.AuthResponse
import pl.podkal.domowniczeqqq.login.LoginViewModel

private val FamilioTeal = Color(0xFF1ABC9C) // Turkus
private val FamilioDark = Color(0xFF222222) // Ciemny grafit
private val FamilioGray = Color(0xFF777777) // Szary

@Composable
fun RegisterScreen(
    navController: NavController,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val authState = viewModel.authState.collectAsState().value

    // Toasty dla użytkownika
    LaunchedEffect(key1 = authState) {
        when (authState) {
            is AuthResponse.Success -> {
                Toast.makeText(context, "Konto utworzone pomyślnie!", Toast.LENGTH_SHORT).show()
                navController.navigate("login_screen") {
                    popUpTo("register_screen") { inclusive = true }
                }
            }

            is AuthResponse.Error -> {
                Toast.makeText(context, "Błąd: ${authState.message}", Toast.LENGTH_SHORT).show()
            }

            else -> {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Logo na samej górze
        Image(
            painter = painterResource(R.drawable.logo),
            contentDescription = "Logo Familio",
            modifier = Modifier
                .width(250.dp)
                .height(250.dp)
                .padding(top = 30.dp)
                .clip(CircleShape)
        )

        Spacer(modifier = Modifier.height(20.dp)) // Duża przerwa między logo a nagłówkiem

        // Nagłówek i podtytuł w jednym rzędzie
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Zarejestruj się",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = FamilioDark
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Wypełnij formularz, aby kontynuować",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = FamilioGray
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Pola tekstowe
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            placeholder = { Text(text = "Adres e-mail") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Email,
                    contentDescription = null
                )
            },
            shape = RoundedCornerShape(15.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp)) // Minimalna przerwa

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            placeholder = { Text(text = "Hasło") },
            leadingIcon = { Icon(imageVector = Icons.Rounded.Lock, contentDescription = null) },
            visualTransformation = PasswordVisualTransformation(),
            shape = RoundedCornerShape(15.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp)) // Przerwa przed przyciskiem

        // Przyciski i komunikaty
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(5.dp) // Mniejsze odstępy
        ) {
            Button(
                onClick = {
                    if (email.isNotBlank() && password.isNotBlank()) {
                        isLoading = true
                        viewModel.registerbyEmail(email, password)
                    }
                },
                modifier = Modifier
                    .width(170.dp)
                    .height(40.dp)
                    .border(1.dp, FamilioDark, RoundedCornerShape(25.dp))
                    .clip(RoundedCornerShape(20.dp)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = FamilioTeal,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Zarejestruj",
                    fontWeight = FontWeight.Bold,

                )
            }
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                modifier = Modifier.clickable { /* Dodaj akcję dla resetu hasła */ },
                text = "Zapomniałeś hasła?",
                fontWeight = FontWeight.Bold,
                color = FamilioGray
            )

            Text(
                modifier = Modifier
                    .clickable { navController.navigate("login_screen") },
                text = "Wróć do logowania",
                fontWeight = FontWeight.Bold,
                color = FamilioGray
            )
        }
    }
}