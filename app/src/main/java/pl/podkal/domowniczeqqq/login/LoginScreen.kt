package pl.podkal.domowniczeqqq.login

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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

private val FamilioTeal = Color(0xFF1ABC9C)
private val FamilioDark = Color(0xFF222222)
private val FamilioGray = Color(0xFF777777)

@Composable
fun LoginScreen(navController: NavController) {
    val logVm: LoginViewModel = hiltViewModel()
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    val authState by logVm.authState.collectAsState()
    val context = LocalContext.current as Activity

    val googleSignInLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        logVm.handleGoogleSignInResult(result.data)
    }

    LaunchedEffect(authState) {
        when (authState) {
            is AuthResponse.Success -> {
                navController.navigate("home_screen") {
                    popUpTo("login_screen") { inclusive = true }
                }
            }
            is AuthResponse.Error -> {
                Toast.makeText(context, "Błąd: ${(authState as AuthResponse.Error).message}", Toast.LENGTH_SHORT).show()
            }
            else -> {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Image(
            painter = painterResource(R.drawable.logo),
            contentDescription = "Logo Familio",
            modifier = Modifier
                .width(250.dp)
                .height(250.dp)
                .padding(top = 30.dp)
                .clip(CircleShape)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Zaloguj się",
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

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            placeholder = { Text("Adres e-mail") },
            leadingIcon = { Icon(Icons.Rounded.Email, null) },
            shape = RoundedCornerShape(15.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            placeholder = { Text("Hasło") },
            leadingIcon = { Icon(Icons.Rounded.Lock, null) },
            visualTransformation = PasswordVisualTransformation(),
            shape = RoundedCornerShape(15.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        Column(modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = { if (email.isBlank() || password.isBlank()) {
                    Toast.makeText(context, "Uzupełnij wszystkie pola!", Toast.LENGTH_SHORT).show()
                } else {
                    logVm.login(email, password)}
                          },
                modifier = Modifier
                    .width(200.dp)
                    .height(40.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FamilioTeal, contentColor = Color.White)
            ) {
                Text("Zaloguj się", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {logVm.signInWithGoogle(context, googleSignInLauncher)},
                modifier = Modifier
                    .width(200.dp)
                    .height(40.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FamilioDark, contentColor = Color.White)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("Zaloguj przez ", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(7.dp))
                    Image(
                        painter = painterResource(id = R.drawable.google_icon),
                        contentDescription = "Google Icon",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Text(
                "Zapomniałeś hasła?",
                color = FamilioGray,
                modifier = Modifier.clickable { /* dodaj reset hasła */ },
                fontWeight = FontWeight.Bold
            )

            Text(
                "Zarejestruj konto",
                color = FamilioGray,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { navController.navigate("register_screen") }
            )
        }
    }
}
